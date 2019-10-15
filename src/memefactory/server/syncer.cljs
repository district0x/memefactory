(ns memefactory.server.syncer
  (:require
   [bignumber.core :as bn]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-web3.core :as web3-core]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.utils :as web3-utils]
   [cljs.core.async :as async]
   [district.server.config :refer [config]]
   [district.server.web3 :refer [web3]]
   [district.server.web3-events :as web3-events]
   [district.shared.async-helpers :refer [safe-go <?]]
   [district.shared.error-handling :refer [try-catch]]
   [district.time :as time]
   [memefactory.server.db :as db]
   [memefactory.server.generator]
   [memefactory.server.ipfs :as ipfs]
   [memefactory.server.ranks-cache :as ranks-cache]
   [memefactory.server.utils :as server-utils]
   [memefactory.shared.contract.registry-entry :refer [vote-options]]
   [memefactory.shared.utils :as shared-utils]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log :refer [spy]]
   ))

(declare start)
(declare stop)

(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop syncer))

(declare schedule-meme-number-assigner)

(defn evict-ranks-cache []
  (ranks-cache/evict-rank :creator-rank)
  (ranks-cache/evict-rank :collector-rank)
  (ranks-cache/evict-rank :curator-rank)
  (ranks-cache/evict-rank :challenger-rank)
  (ranks-cache/evict-rank :voter-rank))

(defn assign-next-number! [address]
  (let [current-meme-number (db/current-meme-number)]
    (db/assign-meme-number! address (inc current-meme-number))
    (log/info (str "Meme " address " got number " (inc current-meme-number) " assigned."))))

(defn meme-number-assigner [address]
  (safe-go
   (let [{:keys [:challenge/challenger :reg-entry/challenge-period-end :challenge/reveal-period-end] :as re}
         (db/get-registry-entry {:reg-entry/address address} [:reg-entry/created-on :reg-entry/challenge-period-end :challenge/challenger
                                                              :challenge/commit-period-end :challenge/commit-period-end
                                                              :challenge/reveal-period-end :challenge/votes-for :challenge/votes-against])
         {:keys [:meme/number] :as meme} (db/get-meme address)
         now-in-seconds (<? (server-utils/now-in-seconds))]
     ;; if we have a number assigned we don't do anything
     (when-not number
       (if (not challenger)
         (assign-next-number! address)
         (if (> now-in-seconds reveal-period-end)
           (when (= (shared-utils/reg-entry-status now-in-seconds re)
                    :reg-entry.status/whitelisted)
             (assign-next-number! address))
           (schedule-meme-number-assigner address (inc (- reveal-period-end now-in-seconds)))))))))

(defn schedule-meme-number-assigner [address seconds]
  (when (pos? seconds)
    (log/info (str "Scheduling meme-number-assigner for meme " address " in " seconds " seconds"))
    (js/setTimeout (partial meme-number-assigner address)
                   (* 1000 seconds))))

(defn- get-event [{:keys [:event :log-index :block-number]
                   {:keys [:contract-key :forwards-to]} :contract}]
  {:event/contract-key (name (or forwards-to contract-key))
   :event/event-name (name event)
   :event/log-index log-index
   :event/block-number block-number})

;;;;;;;;;;;;;;;;;;;;;;
;; Event handlers   ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn- add-registry-entry [registry-entry timestamp]
  (db/insert-registry-entry! (merge registry-entry
                                    {:reg-entry/created-on timestamp})))

(defn- add-error [m err-txt]
  (assoc m :errors (conj (:errors m) err-txt)))

(defn vote-sanity-check [{:keys [:vote/voter
                                 :vote/amount
                                 :vote/option]}]
  (cond-> {:errors []}
    (not (web3-utils/address? @web3 voter))
    (add-error "Not a voter address")

    (or (not (number? amount))
        (= 0 amount))
    (add-error "Incorrect vote amount")

    (not (#{0 1 2} option))
    (add-error "Unknown option")))

(defn meme-sanity-check [{:keys [:reg-entry/address
                                 :meme/meta-hash
                                 :meme/total-supply
                                 :meme/total-minted]}]
  (cond-> {:errors []}
    (not (ipfs/ipfs-hash? meta-hash)) (add-error "Malformed ipfs meta hash")
    (not (web3-utils/address? @web3 address)) (add-error "Malformed registry entry address")
    (not (int? total-supply)) (add-error "Malformed total-suply number")
    (not (int? total-minted)) (add-error "Malformed total minted number")))

(defn meme-constructed-event [_ {:keys [:args] :as event}]
  (safe-go
   (let [{:keys [:registry-entry :timestamp :creator :meta-hash
                 :total-supply :version :deposit :challenge-period-end]} args
         registry-entry-data {:reg-entry/address registry-entry
                              :reg-entry/creator creator
                              :reg-entry/version version
                              :reg-entry/created-on timestamp
                              :reg-entry/deposit (bn/number deposit)
                              :reg-entry/challenge-period-end (bn/number challenge-period-end)}
         meme {:reg-entry/address registry-entry
               :meme/meta-hash (web3-utils/to-ascii @web3 meta-hash)
               :meme/total-supply (bn/number total-supply)
               :meme/total-minted 0}
         {:keys [:meme/meta-hash]} meme
         errors (meme-sanity-check meme)
         now-in-seconds (<? (server-utils/now-in-seconds))]
     (if-not (empty? (:errors errors))
       (do
         (log/warn (str "Dropping smart contract event " (:event event))
                   (merge errors registry-entry-data meme)
                   ::meme-constructed-event)
         errors)

       (let [meme-meta (<? (server-utils/get-ipfs-meta @ipfs/ipfs meta-hash))
             {:keys [:title :image-hash :search-tags :comment]} meme-meta]
         (db/insert-registry-entry! registry-entry-data)
         (db/upsert-user! {:user/address creator})
         (db/insert-meme! (merge meme {:reg-entry/address registry-entry
                                       :meme/comment comment
                                       :meme/image-hash image-hash
                                       :meme/title title}))

         (schedule-meme-number-assigner registry-entry (inc (- (bn/number challenge-period-end)
                                                               now-in-seconds)))
         (when search-tags
           (doseq [t (into #{} search-tags)]
             (db/tag-meme! {:reg-entry/address registry-entry :tag/name t}))))))))

(defn param-change-constructed-event [_ {:keys [:args]}]
  (safe-go
   (let [{:keys [:registry-entry :creator :version :deposit :challenge-period-end :db :key :value :timestamp :meta-hash]} args
         hash (web3-utils/to-ascii @web3 meta-hash)
         {:keys [reason]} (server-utils/get-ipfs-meta @ipfs/ipfs hash)]
     (add-registry-entry {:reg-entry/address registry-entry
                          :reg-entry/creator creator
                          :reg-entry/version version
                          :reg-entry/created-on timestamp
                          :reg-entry/deposit (bn/number deposit)
                          :reg-entry/challenge-period-end (bn/number challenge-period-end)}
                         timestamp)
     (db/insert-or-replace-param-change!
      {:reg-entry/address registry-entry
       :param-change/db db
       :param-change/key key
       :param-change/value (bn/number value)
       :param-change/original-value (:param/value (first (db/get-params db [key])))
       :param-change/reason reason
       :param-change/meta-hash hash}))))

(defn param-change-applied-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp]} args]
    (db/update-param-change! {:reg-entry/address registry-entry
                              :param-change/applied-on timestamp})))

(defn challenge-created-event [_ {:keys [:args]}]
  (safe-go
   (let [{:keys [:registry-entry :challenger :commit-period-end
                 :reveal-period-end :reward-pool :metahash :timestamp :version]} args
         challenge {:reg-entry/address registry-entry
                    :challenge/challenger challenger
                    :challenge/commit-period-end (bn/number commit-period-end)
                    :challenge/reveal-period-end (bn/number reveal-period-end)
                    :challenge/reward-pool (bn/number reward-pool)
                    :challenge/meta-hash (web3-utils/to-ascii @web3 metahash)}
         registry-entry {:reg-entry/address registry-entry
                         :reg-entry/version version}
         challenge-meta (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:challenge/meta-hash challenge)))]
     (db/upsert-user! {:user/address challenger})
     (db/update-registry-entry! (merge registry-entry
                                       challenge
                                       {:challenge/created-on timestamp
                                        :challenge/comment (:comment challenge-meta)})))))

(defn vote-committed-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp :voter :amount]} args
        vote {:reg-entry/address registry-entry
              :vote/voter voter
              :vote/amount (bn/number amount)
              :vote/option 0}]
    (db/upsert-user! {:user/address voter})
    (db/insert-vote! (merge vote {:vote/created-on timestamp}))))

(defn vote-revealed-event [_ {:keys [:args]}]
  (safe-go
   (let [{:keys [:registry-entry :timestamp :version :voter :option]} args
         vote (db/get-vote {:reg-entry/address registry-entry :vote/voter voter} [:vote/voter :vote/amount :reg-entry/address])
         re (db/get-registry-entry {:reg-entry/address registry-entry} [:challenge/votes-against :challenge/votes-for :challenge/votes-total])
         {:keys [:challenge/votes-total :challenge/votes-against :challenge/votes-for]} re]
     (let [option (bn/number option)
           {:keys [:vote/voter :vote/amount :vote/option] :as vote} (merge vote
                                                                           {:vote/option option
                                                                            :vote/revealed-on timestamp})
           errors (vote-sanity-check vote)]
       (if-not (empty? (:errors errors))
         errors
         (do (db/update-registry-entry! (cond-> {:reg-entry/address registry-entry
                                                 :reg-entry/version version}

                                          true
                                          (assoc :challenge/votes-total (+ votes-total amount))

                                          (= (vote-options option) :vote.option/vote-against)
                                          (assoc :challenge/votes-against (+ votes-against amount))

                                          (= (vote-options option) :vote.option/vote-for)
                                          (assoc :challenge/votes-for (+ votes-for amount))))
             (db/update-vote! vote)))
       (db/upsert-user! {:user/address voter})))))

(defn vote-reward-claimed-event [_ {:keys [:args]}]
  (safe-go
   (let [{:keys [:registry-entry :timestamp :version :voter :amount]} args]
     (db/update-vote! {:reg-entry/address registry-entry
                       :vote/voter voter
                       :vote/claimed-reward-on timestamp})
     (db/inc-user-field! voter :user/voter-total-earned (bn/number amount)))))

(defn vote-amount-claimed-event [_ {:keys [:args]} ]
  (let [{:keys [:registry-entry :timestamp :version :voter :amount]} args]
    (db/update-vote! {:reg-entry/address registry-entry
                      :vote/voter voter
                      :vote/reclaimed-amount-on timestamp})))

(defn challenge-reward-claimed-event [_ {:keys [:args]}]
  (safe-go
   (let [{:keys [:registry-entry :timestamp :version :challenger :amount]} args
         re (db/get-registry-entry {:reg-entry/address registry-entry} [:challenge/challenger :reg-entry/deposit])
         {:keys [:challenge/challenger :reg-entry/deposit]} re]
     (db/update-registry-entry! {:reg-entry/address registry-entry
                                 :challenge/claimed-reward-on timestamp
                                 :challenge/reward-amount (bn/number amount)})
     (db/inc-user-field! challenger :user/challenger-total-earned (bn/number amount)))))

(defn meme-minted-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp :version :creator :token-start-id :token-end-id :total-minted]} args
        token-start-id (bn/number token-start-id)
        token-end-id (bn/number token-end-id)
        total-minted (bn/number total-minted)]
    (db/insert-meme-tokens! {:token-id-start token-start-id
                             :token-id-end token-end-id
                             :reg-entry/address registry-entry})
    (db/update-meme! {:reg-entry/address registry-entry
                      :meme/first-mint-on timestamp
                      :meme/token-id-start token-start-id
                      :meme/total-minted total-minted})
    (doseq [tid (range token-start-id (inc token-end-id))]
      (db/upsert-meme-token-owner! {:meme-token/token-id tid
                                    :meme-token/owner creator
                                    :meme-token/transferred-on timestamp}))))

(defn meme-auction-started-event [_ {:keys [:args]}]
  (let [{:keys [:meme-auction :timestamp :meme-auction :token-id :seller :start-price :end-price :duration :description :started-on]} args]
    (db/insert-meme-auction! {:meme-auction/address meme-auction
                              :meme-auction/token-id (bn/number token-id)
                              :meme-auction/seller seller
                              :meme-auction/start-price (bn/number start-price)
                              :meme-auction/end-price (bn/number end-price)
                              :meme-auction/duration (bn/number duration)
                              :meme-auction/description description
                              :meme-auction/started-on (bn/number started-on)})))

(defn meme-auction-canceled-event [_ {:keys [:args]}]
  (let [{:keys [:meme-auction :timestamp :token-id]} args]
    (db/update-meme-auction! {:meme-auction/address meme-auction
                              :meme-auction/canceled-on timestamp})))

;; TODO : never gets called, wtf?
(defn meme-auction-buy-event [_ {:keys [:args] :as evt}]
  (safe-go

   (log/error "@@@ meme-auction-buy-event" evt)

   (let [{:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds]} args
         auction (db/get-meme-auction meme-auction)
         reg-entry-address (-> (db/get-meme-by-auction-address meme-auction)
                               :reg-entry/address)
         seller (:meme-auction/seller auction)
         {:keys [:user/best-single-card-sale]} (db/get-user {:user/address seller}
                                                            [:user/best-single-card-sale])]
     (db/upsert-user! {:user/address seller
                       :user/best-single-card-sale (max best-single-card-sale
                                                        (bn/number seller-proceeds))})
     (db/upsert-user! {:user/address buyer})
     (db/inc-user-field! seller :user/total-earned (bn/number seller-proceeds))
     (db/inc-meme-field! reg-entry-address :meme/total-trade-volume (bn/number price))
     (db/update-meme-auction! {:meme-auction/address meme-auction
                               :meme-auction/bought-for (bn/number price)
                               :meme-auction/bought-on timestamp
                               :meme-auction/buyer buyer}))))

(defn meme-token-transfer-event [_ {:keys [:args]}]
  (let [{:keys [:_to :_token-id :_timestamp]} args]
    (db/upsert-meme-token-owner! {:meme-token/token-id (bn/number _token-id)
                                  :meme-token/owner _to
                                  :meme-token/transferred-on (bn/number _timestamp)})))

(defn eternal-db-event [_ {:keys [:args :address] :as event}]
  (let [{:keys [:records :values :timestamp]} args
        records->values (zipmap records values)
        keys->values (->> #{"challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration" "deposit"
                            "challengeDispensation" "voteQuorum" "maxTotalSupply" "maxAuctionDuration"}
                          (map (fn [k] (when-let [v (records->values (web3-utils/sha3 @web3 k))] [k v])))
                          (into {}))]
    (db/upsert-params!
     (map (fn [[k v]]
            {:param/key k
             :param/db address
             :param/value (bn/number v)
             :param/set-on timestamp})
          keys->values))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End of events handlers   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn apply-blacklist-patches! []
  (let [{:keys [blacklist-file]} @config
        blacklisted-addresses (:blacklisted-image-addresses (server-utils/load-edn-file blacklist-file))]
    (try-catch
     (doseq [address blacklisted-addresses]
       (log/info (str "Blacklisting address " address) ::apply-blacklist-patches)
       (db/patch-forbidden-reg-entry-image! address)))))

(defn- block-timestamp* [block-number]
  (let [out-ch (async/promise-chan)]
    (web3-eth/get-block @web3 block-number false (fn [err {:keys [:timestamp] :as res}]
                                                   (if err
                                                     (async/put! out-ch err)
                                                     (let [{:keys [:timestamp] :as result} (js->clj res :keywordize-keys true)]
                                                       (log/debug "cache miss for block-timestamp" {:block-number block-number :timestamp timestamp})
                                                       (async/put! out-ch timestamp)))))
    out-ch))

(def block-timestamp
  (memoize block-timestamp*))

(defn- dispatcher [callback]
  (fn [err {:keys [:block-number] :as event}]
    (safe-go
     (try
       (let [block-timestamp (<? (block-timestamp block-number))
             event (-> event
                       (update :event camel-snake-kebab/->kebab-case)
                       (update-in [:args :version] bn/number)
                       (update-in [:args :timestamp] (fn [timestamp]
                                                       (if timestamp
                                                         (bn/number timestamp)
                                                         block-timestamp))))
             {:keys [:event/contract-key :event/event-name :event/block-number :event/log-index]} (get-event event)
             {:keys [:event/last-block-number :event/last-log-index :event/count]
              :or {last-block-number -1
                   last-log-index -1
                   count 0}} (db/get-last-event {:event/contract-key contract-key :event/event-name event-name} [:event/last-log-index :event/last-block-number :event/count])
             evt {:event/contract-key contract-key
                  :event/event-name event-name
                  :event/count count
                  :last-block-number last-block-number
                  :last-log-index last-log-index
                  :block-number block-number
                  :log-index log-index}]
         (if (or (> block-number last-block-number)
                 (and (= block-number last-block-number) (> log-index last-log-index)))
           (let [result (callback err event)]
             (log/info "Handling new event" evt)

             ;; block if we need
             (when (satisfies? cljs.core.async.impl.protocols/ReadPort result)
               (<! result))
             (db/upsert-event! {:event/last-log-index log-index
                                :event/last-block-number block-number
                                :event/count (inc count)
                                :event/event-name event-name
                                :event/contract-key contract-key}))

           (log/info "Skipping handling of a persisted event" evt)))
       (catch js/Error error
         (log/error "Exception when handling event" {:error error
                                                     :event event})
         ;; So we crash as fast as we can and don't drag errors that are harder to debug
         (js/process.exit 1))))))

(defn- assign-meme-registry-numbers!
  "if there are any memes with unassigned numbers but still assignable start the number assigners"
  []
  (let [now (server-utils/now-in-seconds)
        assignable-reg-entries (filter #(contains? #{:reg-entry.status/challenge-period
                                                     :reg-entry.status/commit-period
                                                     :reg-entry.status/reveal-period}
                                                   (shared-utils/reg-entry-status now %))
                                       (db/all-meme-reg-entries))
        assignable-whitelisted-reg-entries (filter #(and (not (:meme/number %))
                                                         (= :reg-entry.status/whitelisted (shared-utils/reg-entry-status now %)))
                                                   (db/all-meme-reg-entries))]

    (log/info "Assigning numbers to whitelisted memes already on db "
              {:count (count assignable-whitelisted-reg-entries)
               :current-meme-number (db/current-meme-number)}
              ::assign-meme-registry-numbers!)

    ;; add numbers to all assignable whitelisteds
    (doseq [{:keys [:reg-entry/address]} assignable-whitelisted-reg-entries]
      (assign-next-number! address))

    (log/info "Schedulling number assigner to memes already on db "
              {:count (count assignable-reg-entries)
               :current-meme-number (db/current-meme-number)}
              ::assign-meme-registry-numbers!)

    ;; schedule meme number assigners for all memes that need it
    (doseq [{:keys [:reg-entry/address :reg-entry/challenge-period-end :challenge/reveal-period-end]} assignable-reg-entries]
      (schedule-meme-number-assigner address (inc (- (if (> challenge-period-end now)
                                                       challenge-period-end
                                                       reveal-period-end)
                                                     now))))))

(defn start [opts]
  (safe-go
   (when-not (:disabled? opts)
     (when-not (web3-eth/connected? @web3)
       (throw (js/Error. "Can't connect to Ethereum node")))
     (when-not (= ::db/started @db/memefactory-db)
       (throw (js/Error. "Database module has not started")))
     (let [start-time (server-utils/now)
           event-callbacks {:meme-registry-db/eternal-db-event eternal-db-event
                            :meme-registry/meme-constructed-event meme-constructed-event
                            :meme-registry/challenge-created-event challenge-created-event
                            :meme-registry/vote-committed-event vote-committed-event
                            :meme-registry/vote-revealed-event vote-revealed-event
                            :meme-registry/vote-amount-claimed-event vote-amount-claimed-event
                            :meme-registry/vote-reward-claimed-event vote-reward-claimed-event
                            :meme-registry/challenge-reward-claimed-event challenge-reward-claimed-event
                            :meme-registry/meme-minted-event meme-minted-event
                            :meme-auction-factory/meme-auction-started-event meme-auction-started-event
                            :meme-auction-factory/meme-auction-buy-event meme-auction-buy-event
                            :meme-auction-factory/meme-auction-canceled-event meme-auction-canceled-event
                            :meme-token/transfer meme-token-transfer-event
                            :param-change-db/eternal-db-event eternal-db-event
                            :param-change-registry/param-change-constructed-event param-change-constructed-event
                            :param-change-registry/challenge-created-event challenge-created-event
                            :param-change-registry/vote-committed-event vote-committed-event
                            :param-change-registry/vote-revealed-event vote-revealed-event
                            :param-change-registry/vote-amount-claimed-event vote-amount-claimed-event
                            :param-change-registry/vote-reward-claimed-event vote-reward-claimed-event
                            :param-change-registry/challenge-reward-claimed-event challenge-reward-claimed-event
                            :param-change-registry/param-change-applied-event param-change-applied-event}
           callback-ids (doall (for [[event-key callback] event-callbacks]
                                 (web3-events/register-callback! event-key (dispatcher callback))))]
       (web3-events/register-after-past-events-dispatched-callback! (fn []
                                                                      (log/warn "Syncing past events finished" (time/time-units (- (server-utils/now) start-time)) ::start)
                                                                      (apply-blacklist-patches!)
                                                                      (assign-meme-registry-numbers!)))
       (assoc opts :callback-ids callback-ids)))))

(defn stop [syncer]
  (web3-events/unregister-callbacks! (:callback-ids @syncer)))
