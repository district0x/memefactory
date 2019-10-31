(ns district.server.web3-events
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts]
   [district.server.web3 :refer [web3]]
   [medley.core :as medley]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]))

(declare start)
(declare stop)

(defstate ^{:on-reload :noop} web3-events
  :start (start (merge (:web3-events @config)
                       (:web3-events (mount/args))))
  :stop (stop web3-events))

(defn register-callback! [event-key callback & [callback-id]]
  (let [[contract-key event] (if-not (= event-key ::after-past-events-dummy-key)
                               (get (:events @web3-events) event-key)
                               [::after-past-events-dummy-contract ::after-past-events-dummy-event])
        callback-id (or callback-id (str (random-uuid)))]
    (when-not contract-key
      (throw (js/Error. "Trying to register callback for non existing event " event-key)))

    (swap! (:callbacks @web3-events) (fn [callbacks]
                                       (-> callbacks
                                           (assoc-in [contract-key event callback-id] callback)
                                           (assoc-in [:callback-id->path callback-id] [contract-key event]))))
    callback-id))

(defn register-after-past-events-dispatched-callback! [callback]
  (register-callback! ::after-past-events-dummy-key callback))

(defn unregister-callbacks! [callback-ids]
  (doseq [callback-id callback-ids]
    (let [path (get-in @(:callbacks @web3-events) [:callback-id->path callback-id])]
      (swap! (:callbacks @web3-events) (fn [callbacks]
                                         (-> callbacks
                                             (medley/dissoc-in (into path [callback-id]))
                                             (medley/dissoc-in [:callback-id->path callback-id]))))))
  callback-ids)

(defn dispatch [err {:keys [:contract :event] :as evt}]
  (if err
    (log/error "Error Dispatching" {:err err :event evt} ::event-dispatch)
    (when (:dispatch-logging? @web3-events)
      (log/info "Dispatching event" {:err err :event evt} ::event-dispatch)))

  (when (and err
             (fn? (:on-error @web3-events)))
    ((:on-error @web3-events) err evt))

  (when (or (not err)
            (and err (:dispatch-on-error? @web3-events)))

    (doall
     (for [callback (vals (get-in @(:callbacks @web3-events) [(:contract-key contract) event]))]
       (callback err evt)))))

(defn- start-dispatching-latest-events! [events]
  (web3-eth/get-block-number @web3 (fn [_ last-block-number]
                                     (let [event-filters (doall (for [[k [contract event]] events]
                                                                  (let [[_ callback] (first (get-in @(:callbacks @web3-events) [contract event]))]
                                                                    (smart-contracts/subscribe-events contract
                                                                                                      event
                                                                                                      {:from-block last-block-number
                                                                                                       :latest-event? true}
                                                                                                      callback))))]
                                       (log/info "Subscribed to future events" {:events (keys events)})
                                       (swap! (:event-filters @web3-events) (fn [_ new] new) event-filters)))))

(defn- dispatch-after-past-events-callbacks! []
  (let [callbacks (get-in @(:callbacks @web3-events) [::after-past-events-dummy-contract ::after-past-events-dummy-event])
        callback-fns (vals callbacks)
        callback-ids (keys callbacks)]
    (doseq [callback callback-fns]
      (callback))
    (unregister-callbacks! callback-ids)))

(defn start [{:keys [:events] :as opts}]
  (web3-eth/connected? @web3 (fn [_ listening?]

                               (if-not listening?
                                 (throw (js/Error. "Can't connect to Ethereum node"))

                                 (smart-contracts/replay-past-events-in-order
                                  events
                                  dispatch
                                  {:from-block 0
                                   :to-block "latest"
                                   :on-finish (fn []
                                                (dispatch-after-past-events-callbacks!)
                                                (start-dispatching-latest-events! events))}))))
  (merge opts {:callbacks (atom {})
               :event-filters (atom nil)}))

(defn stop [web3-events]
  (log/info "Stopping web3-events" (:events @web3-events))
  (doseq [subscription @(:event-filters @web3-events)]
    (web3-eth/unsubscribe @web3 subscription)))
