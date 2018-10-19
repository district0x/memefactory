(ns memefactory.server.syncer
  (:require [bignumber.core :as bn]
            [camel-snake-kebab.core :as cs :include-macros true]
            [cljs-ipfs-api.files :as ifiles]
            [cljs-solidity-sha3.core :refer [solidity-sha3]]
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
            [district.shared.error-handling :refer [try-catch]]
            [mount.core :as mount :refer [defstate]]
            [print.foo :refer [look] :include-macros true]
            [taoensso.timbre :as log]
            [memefactory.server.ipfs]
            [clojure.string :as str]))

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

(derive :contract/meme :contract/registry-entry)
(derive :contract/param-change :contract/registry-entry)

;;;;;;;;;;;;;;;;;;;;;;
;; Event processors ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn- add-registry-entry [registry-entry timestamp]
  (db/insert-registry-entry! (merge registry-entry
                                    {:reg-entry/created-on timestamp})))

(defn- add-param-change [{:keys [:param-change/key :param-change/db] :as param-change}]
  (let [{:keys [:initial-param/value] :as initial-param} (db/get-initial-param key db)]
    (db/insert-or-replace-param-change! (assoc param-change :param-change/initial-value value))))


(defmulti process-event (fn [contract-type ev] [contract-type (:event-type ev)]))

(defmethod process-event [:contract/meme :constructed]
  [contract-type {:keys [:registry-entry :timestamp :creator :meta-hash
                         :total-supply :version :deposit :challenge-period-end] :as ev}]
  (try-catch
   (let [registry-entry-data {:reg-entry/address registry-entry
                              :reg-entry/creator creator
                              :reg-entry/version version
                              :reg-entry/created-on timestamp
                              :reg-entry/deposit (bn/number deposit)
                              :reg-entry/challenge-period-end (bn/number challenge-period-end)}
         meme {:reg-entry/address registry-entry
               :meme/meta-hash (web3/to-ascii meta-hash)
               :meme/total-supply (bn/number total-supply)
               :meme/total-minted 0}]
     (add-registry-entry registry-entry-data timestamp)
     (let [{:keys [:meme/meta-hash]} meme]
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
                      (db/tag-meme! (:reg-entry/address meme) t))))))))))


(defmethod process-event [:contract/param-change :constructed]
  [contract-type {:keys [:registry-entry :timestamp] :as ev}]
  (try-catch
   (add-registry-entry registry-entry timestamp)
   ;; TODO Fix this call
   (add-param-change {})))

(defmethod process-event [:contract/param-change :change-applied]
  [contract-type {:keys [:registry-entry :timestamp] :as ev}]
  (try-catch
   ;; TODO: could also just change applied date to timestamp
   (add-param-change registry-entry)))

(defmethod process-event [:contract/registry-entry :challenge-created]
  [_ {:keys [:registry-entry :timestamp] :as ev}]
  (try-catch
   (let [challenge  nil      ;; TODO Fix get challenge data from event
         registry-entry nil] ;; get registry entry from db
     (.then (get-ipfs-meta (:challenge/meta-hash challenge) {:comment "Dummy comment"})
            (fn [challenge-meta]
              (db/update-registry-entry! (merge registry-entry
                                                challenge
                                                {:challenge/created-on timestamp
                                                 :challenge/comment (:comment challenge-meta)}))
              (db/update-user! {:user/address (:challenge/challenger challenge)}))))))

(defmethod process-event [:contract/registry-entry :vote-committed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
   (let [voter (web3-utils/uint->address (first data))
         vote nil] ;; TODO Fix get vote info from event
     (db/insert-vote! (merge vote {:vote/created-on timestamp})))))

(defmethod process-event [:contract/registry-entry :vote-revealed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
   (let [voter (web3-utils/uint->address (first data))
         vote nil            ;; get vote data from event
         registry-entry nil] ;; TODO Fix get updated registry entry data from event
     (db/update-registry-entry! registry-entry)
     (db/update-vote! vote))))

(defmethod process-event [:contract/registry-entry :vote-reward-claimed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
   (let [voter (web3-utils/uint->address (first data))
         vote nil ;; TODO Fix get new vote data from event
         reward (second data)]
     (db/update-vote! vote)
     (db/inc-user-field! voter :user/voter-total-earned (bn/number reward)))))

(defmethod process-event [:contract/registry-entry :vote-amount-reclaimed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
   (let [voter (web3-utils/uint->address (first data))
         vote nil ] ;; TODO Fix get vote data from event that needs to be updated
     (db/update-vote! vote))))

(defmethod process-event [:contract/registry-entry :challenge-reward-claimed]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
   (let [reg-entry nil ;; TODO Fix get this info from event
         {:keys [:challenge/challenger :reg-entry/deposit]} reg-entry]

     (db/update-registry-entry! reg-entry)
     (db/inc-user-field! (:challenge/challenger reg-entry) :user/challenger-total-earned deposit))))

(defmethod process-event [:contract/meme :minted]
  [_ {:keys [:registry-entry :timestamp :data] :as ev}]
  (try-catch
   (let [[creator token-id-start token-id-end total-minted] data
         timestamp (bn/number timestamp)
         token-id-start (bn/number token-id-start)
         token-id-end (bn/number token-id-end)
         total-minted (bn/number total-minted)]

     (db/insert-meme-tokens! {:token-id-start token-id-start
                              :token-id-end token-id-end
                              :reg-entry/address registry-entry})

     (doseq [tid (range token-id-start (inc token-id-end))]
       (db/insert-or-replace-meme-token-owner {:meme-token/token-id tid
                                               :meme-token/owner (web3/to-hex creator)
                                               :meme-token/transferred-on timestamp}))

     (db/update-meme-first-mint-on! registry-entry timestamp)
     (db/update-meme-token-id-start! registry-entry token-id-start)
     (db/update-meme-total-minted! registry-entry total-minted))))

(defmethod process-event [:contract/meme-auction :auction-started]
  [_ {:keys [:meme-auction :timestamp :data] :as ev}]
  (try-catch
   (let [meme-auction nil] ;; TODO Fix get this from event
     (db/insert-or-update-meme-auction! meme-auction))))

(defmethod process-event [:contract/meme-auction :buy]
  [_ {:keys [:meme-auction :timestamp :data] :as ev}]
  (try-catch
   (let [[buyer price] data
         price (bn/number price)
         {reg-entry-address :reg-entry/address
          auction-address :meme-auction/address} nil] ;; TODO Fix, get this info from event
     (db/inc-meme-total-trade-volume! {:reg-entry/address reg-entry-address
                                       :amount price})
     (db/insert-or-update-meme-auction! {:meme-auction/address auction-address
                                         :meme-auction/bought-for price
                                         :meme-auction/bought-on timestamp
                                         :meme-auction/buyer (web3-utils/uint->address buyer)}))))

(defmethod process-event [:contract/meme-token :transfer]
  [_ ev]
  (try-catch
   (let [{:keys [:_to :_token-id :_timestamp]} ev]
     (db/insert-or-replace-meme-token-owner {:meme-token/token-id (bn/number _token-id)
                                             :meme-token/owner _to
                                             :meme-token/transferred-on (bn/number _timestamp)}))))

(defmethod process-event [:contract/eternal-db :eternal-db-event]
  [_ ev]
  (try-catch
   (let [{:keys [:contract-address :records :values :timestamp]} ev
         records->values (zipmap records values)
         keys->values (->> #{"challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration" "deposit"
                             "challengeDispensation" "voteQuorum" "maxTotalSupply" "maxAuctionDuration"}
                           (map (fn [k] (when-let [v (records->values (web3/sha3 k))] [k v])))
                           (into {}))]
     (doseq [[k v] keys->values]
       (when-not (db/initial-param-exists? k contract-address)
           (db/insert-initial-param! {:initial-param/key k
                                      :initial-param/db contract-address
                                      :initial-param/value (bn/number v)
                                      :initial-param/set-on timestamp}))))))

(defmethod process-event :default
  [contract-type {:keys [:event-type] :as evt}]
  (log/warn (str "No process-event method defined for processing contract-type: " contract-type " event-type: " event-type) evt ::process-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End of events processors ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatch-event [contract-type err {:keys [args event] :as a}]
  (let [event-type (cond
                     (:event-type args) (cs/->kebab-case-keyword (web3-utils/bytes32->str (:event-type args)))
                     event (cs/->kebab-case-keyword event))
        ev (-> args
               (assoc :contract-address (:address a))
               (assoc :event-type event-type)
               (update :timestamp bn/number)
               (update :version bn/number))]
    (log/info (str "Dispatching" " " info-text " " contract-type " " event-type) {:ev ev} ::dispatch-event)
    (process-event contract-type ev)))

(defn start [{:keys [:initial-param-query] :as opts}]

  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))

  (when-not (= ::db/started @db/memefactory-db)
    (throw (js/Error. "Database module has not started")))

  (let [last-block-number (last-block-number)
        watchers [{:watcher (partial eternal-db/change-applied-event :param-change-registry-db)
                   :on-event #(dispatch-event :contract/eternal-db %1 %2)}
                  {:watcher (partial eternal-db/change-applied-event :meme-registry-db)
                   :on-event #(dispatch-event :contract/eternal-db %1 %2)}
                  {:watcher (partial registry/registry-entry-event [:meme-registry :meme-registry-fwd])
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

;; for (uint i = 0; i < 32; i++) {
;;             b[i] = byte(uint8(x / (2**(8*(31 - i)))));
;;         }


;; (defn uint->bytes [uint-bn]
;;   (->> (map (fn [i]
;;               (let [hex (-> (bn/div-to-int uint-bn
;;                                            (bn/pow (web3/to-big-number 2) (* 8 (- 31 i))))
;;                             (.toString 16))]
;;                 (-> (subs hex (- (count hex) 2))
;;                     (js/parseInt 16))))
;;             (range 0 32))
;;        (apply js/String.fromCharCode)))

;; (uint->bytes (web3/to-big-number "5.4493935368480931894975801525381113978709950010311913582966553752998353590377e+76"))


;; "0x                            787a7972707776665843715567706b38655052437a716663674e553854445869"
;; "0x516d655778787936464463333371787a7972707776665843715567706b38655052437a716663674e553854445869" <- (web3/from-ascii "QmeWxxy6FDc33qxzyrpwvfXCqUgpk8ePRCzqfcgNU8TDXi")
