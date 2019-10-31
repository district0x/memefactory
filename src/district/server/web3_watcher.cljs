(ns district.server.web3-watcher
  (:require
    [cljs-web3.core :as web3]
    [district.server.config :refer [config]]
    [district.server.web3 :refer [web3]]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)
(defstate web3-watcher
  :start (start (merge (:web3-watcher @config)
                       (:web3-watcher (mount/args))))
  :stop (stop))


;; (defn online? []
;;   @(:online? @web3-watcher))


;; (defn- update-online! [online?]
;;   (reset! (:online? @web3-watcher) online?))


;; (defn- reset-confirmations-left! []
;;   (reset! (:confirmations-left @web3-watcher) (:confirmations @web3-watcher)))


;; (defn- decrease-confirmations-left! []
;;   (swap! (:confirmations-left @web3-watcher) dec))


;; (defn- check-connection []
;;   (let [connected? (web3/connected? @web3)]
;;     (cond
;;       (and connected? (not (online?)))
;;       (do
;;         (update-online! true)
;;         (reset-confirmations-left!)
;;         ((:on-online @web3-watcher)))

;;       (and (not connected?)
;;            (> @(:confirmations-left @web3-watcher) 0))
;;       (decrease-confirmations-left!)

;;       (and (not connected?)
;;            (online?)
;;            (<= @(:confirmations-left @web3-watcher) 0))
;;       (do
;;         (update-online! false)
;;         ((:on-offline @web3-watcher))))))


(defn start [{:keys [:interval :on-online :on-offline :confirmations] :as opts
              :or {interval 3000 confirmations 3}}]
:TODO

  #_(merge {:on-online identity
          :on-offline identity
          :interval-id (js/setInterval check-connection interval)
          :online? (atom (web3/connected? @web3))
          :confirmations-left (atom confirmations)}
         opts))


(defn stop []
  #_(js/clearInterval (:interval-id @web3-watcher)))
