(ns memefactory.server.syncer
  (:require [bignumber.core :as bn]
            [camel-snake-kebab.core :as cs :include-macros true]
            [cljs-ipfs-api.files :as ifiles]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [district.server.config :refer [config]]
            [district.server.smart-contracts :as smart-contracts :refer [replay-past-events]]
            [district.server.web3 :refer [web3]]
            [district.web3-utils :as web3-utils]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-auction :as meme-auction]
            [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
            [memefactory.server.contract.meme-token :as meme-token]
            [memefactory.server.contract.param-change :as param-change]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [memefactory.server.db :as db]
            [memefactory.server.deployer]
            [memefactory.server.generator]
            [mount.core :as mount :refer [defstate]]
            [print.foo :refer [look] :include-macros true]
            [taoensso.timbre :as log])
  (:require-macros [memefactory.server.macros :refer [try-catch]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop syncer))

(def info-text "smart-contract event")
(def error-text "smart-contract event error")

(defn get-ipfs-meta [meta-hash & [default]]
  (js/Promise.
   (fn [resolve reject]
     (log/info (str "Downloading: " "/ipfs/" meta-hash) ::get-meme-data)
     (ifiles/fget (str "/ipfs/" meta-hash)
                  {:req-opts {:compress false}}
                  (fn [err content]
                    (try
                      (if (and (not err)
                               (not-empty content))
                        ;; Get returns the entire content, this include CIDv0+more meta+data
                        ;; TODO add better way of parsing get return
                        (-> (re-find #".+(\{.+\})" content)
                            second
                            js/JSON.parse
                            (js->clj :keywordize-keys true)
                            resolve)
                        (throw (js/Error. (str (or err "Error") " when downloading " "/ipfs/" meta-hash ))))
                      (catch :default e
                        (log/error error-text {:error (ex-message e)} ::get-meme-data)
                        (when goog.DEBUG
                          (resolve default)))))))))

(defn- last-block-number []
  (web3-eth/block-number @web3))

(defn on-start
  "Queries for inital parameter values"
  [initial-param-query]
  (try-catch
   (doall
    (doseq [[contract-key fields] initial-param-query]
      (doseq [[k v] (zipmap fields (->> (eternal-db/get-uint-values contract-key fields)
                                        (map bn/number)))]
        (db/insert-or-replace-param-change! {:reg-entry/address "initinitinitinitinitinitinitinitinitinit"
                                             :param-change/db (smart-contracts/contract-address contract-key)
                                             :param-change/key (name k)
                                             :param-change/value v
                                             ;; TODO: can we get that date?
                                             :param-change/applied-on 0}))))))

(derive :contract/meme         :contract/registry-entry)
(derive :contract/param-change :contract/registry-entry)

;;;;;;;;;;;;;;;;;;;;;;
;; Event processors ;;
;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-event (fn [contract-type ev] [contract-type (:event-type ev)]))


(defn add-registry-entry [registry-entry timestamp]
  (db/insert-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                    (registry-entry/load-registry-entry-challenge registry-entry)
                                    {:reg-entry/created-on timestamp})))

(defmethod process-event [:contract/meme :constructed]
  [contract-type {:keys [:registry-entry :timestamp] :as ev}]

  (try-catch
   (add-registry-entry registry-entry timestamp)
   (let [{:keys [:meme/meta-hash] :as meme} (meme/load-meme registry-entry)]
     (.then (get-ipfs-meta meta-hash {:title "Dummy meme title"
                                      :image-hash "REPLACE WITH IPFS IMAGE HASH HERE"
                                      :search-tags nil})
            (fn [meme-meta]
              (let [{:keys [title image-hash search-tags]} meme-meta]
                (db/insert-meme! (merge meme
                                        {:meme/image-hash image-hash
                                         :meme/title title}))
                (when search-tags
                  (doseq [t search-tags]
                    (db/tag-meme! (:reg-entry/address meme) t)))))))))

(defmethod process-event [:contract/param-change :constructed]
  [contract-type {:keys [:registry-entry :timestamp] :as ev}]

  (try-catch
   (add-registry-entry registry-entry timestamp)
   (db/insert-param-change! (param-change/load-param-change registry-entry))))


(defmethod process-event [:contract/registry-entry :challenge-created]
  [_ {:keys [:registry-entry :timestamp] :as ev}]

  (try-catch
   (let [challenge (registry-entry/load-registry-entry-challenge registry-entry)]
     (.then (get-ipfs-meta (:challenge/meta-hash challenge) {:comment "Dummy comment"})
            (fn [challenge-meta]
              (db/update-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                                challenge
                                                {:challenge/created-on timestamp
                                                 :challenge/comment (:comment challenge-meta)}))
              (db/update-user! {:user/address (:challenge/challenger challenge)}))))))

(defmethod process-event [:contract/registry-entry :vote-committed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]

  (try-catch
   (let [voter (web3-utils/uint->address (first data))
         vote (registry-entry/load-vote registry-entry voter)]
     (db/insert-vote! (merge vote {:vote/created-on timestamp})))))

(defmethod process-event [:contract/registry-entry :vote-revealed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]

  (try-catch
   (let [voter (web3-utils/uint->address (first data))]
     (db/update-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                       (registry-entry/load-registry-entry-challenge registry-entry)))
     (db/update-vote! (registry-entry/load-vote registry-entry voter)))))

(defmethod process-event [:contract/registry-entry :vote-reward-claimed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]

  (try-catch
   (let [voter (web3-utils/uint->address (first data))
         vote (registry-entry/load-vote registry-entry voter)
         reward (second data)]
     (db/update-vote! vote)
     (db/inc-user-field! voter :user/voter-total-earned (bn/number reward)))))

(defmethod process-event [:contract/registry-entry :challenge-reward-claimed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]

  (try-catch
   (let [{:keys [:challenge/challenger :reg-entry/deposit] :as reg-entry}
         (merge (registry-entry/load-registry-entry registry-entry)
                (registry-entry/load-registry-entry-challenge registry-entry))]

     (db/update-registry-entry! reg-entry)
     (db/inc-user-field! (:challenge/challenger reg-entry) :user/challenger-total-earned deposit))))

(defmethod process-event [:contract/meme :minted]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
   (let [[_ token-id-start token-id-end] data]
     (db/insert-meme-tokens! {:token-id-start (bn/number token-id-start)
                              :token-id-end (bn/number token-id-end)
                              :reg-entry/address registry-entry})
     (db/update-meme-first-mint-on! {:reg-entry/address registry-entry
                                     :meme/first-mint-on timestamp}))))

(defmethod process-event [:contract/meme-auction :auction-started]
  [_ {:keys [:meme-auction :timestamp :data] :as ev}]

  (try-catch
   (db/insert-or-update-meme-auction! (meme-auction/load-meme-auction meme-auction))))

(defmethod process-event [:contract/meme-auction :auction-buy]
  [_ {:keys [:meme-auction :timestamp :data] :as ev}]

  (try-catch
   (let [[buyer price] data
         price (bn/number price)
         {reg-entry-address :reg-entry/address
          auction-address :meme-auction/address} (meme-auction/load-meme-auction meme-auction)]
     (db/inc-meme-total-trade-volume! {:reg-entry/address reg-entry-address
                                       :amount price})
     (db/insert-or-update-meme-auction! {:meme-auction/address auction-address
                                         :meme-auction/bought-for price
                                         :meme-auction/bought-on timestamp
                                         :meme-auction/buyer (web3-utils/uint->address buyer)}))))

(defmethod process-event [:contract/meme-token :transfer-from]
  [_ ev]

  (try-catch
   (let [{:keys [:_to :_token-id :_timestamp]} ev]
     (db/insert-or-replace-meme-token-owner {:meme-token/token-id (bn/number _token-id)
                                             :meme-token/owner _to
                                             :meme-token/transferred-on (bn/number _timestamp)}))))

(defmethod process-event :default
  [k ev]
  (log/warn (str "No precess-event defined for processing " k ev) ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End of events processors ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatch-event [contract-type err {:keys [args event] :as a}]
  (let [event-type (cond
                     (:event-type args) (cs/->kebab-case-keyword (web3-utils/bytes32->str (:event-type args)))
                     event      (cs/->kebab-case-keyword event)) 
        ev (-> args
               (assoc :event-type event-type)
               (update :timestamp bn/number)
               (update :version bn/number))]
    (log/info (str info-text " " contract-type " " event-type) {:ev ev} ::dispatch-event)
    (process-event contract-type ev)))

(defn start [{:keys [:initial-param-query] :as opts}]
  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))
  #_(on-start initial-param-query)
  (let [last-block-number (last-block-number)
        watchers [{:watcher (partial registry/registry-entry-event [:meme-registry :meme-registry-fwd])
                   :on-event #(dispatch-event :contract/meme %1 %2)}
                  {:watcher (partial registry/registry-entry-event [:param-change-registry :param-change-registry-fwd])
                   :on-event #(dispatch-event :contract/param-change %1 %2)}
                  {:watcher meme-auction-factory/meme-auction-event
                   :on-event #(dispatch-event :contract/meme-auction %1 %2)}
                  {:watcher meme-token/meme-token-transfer-event
                   :on-event #(dispatch-event :contract/meme-token %1 %2)}]]
    (concat

     ;; Replay every past events (from block 0 to (dec last-block-number))
     (when (pos? last-block-number)
       (->> watchers
            (map (fn [{:keys [watcher on-event]}]
                   (-> (apply watcher [{} {:from-block 0 :to-block (dec last-block-number)}])
                       (replay-past-events on-event))))
            doall))

     ;; Filters that will watch for last event and dispatch
     (->> watchers
          (map (fn [{:keys [watcher on-event]}]
                 (apply watcher [{} "latest" on-event])))
          doall))))

(defn stop [syncer]
  (doseq [filter (remove nil? @syncer)]
    (web3-eth/stop-watching! filter (fn [err]))))
