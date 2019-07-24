(ns memefactory.server.syncer
  (:require
   [bignumber.core :as bn]
   [district.time :as time]
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [district.server.config :refer [config]]
   [district.server.web3 :refer [web3]]
   [district.server.web3-events :refer [register-callback! unregister-callbacks! register-after-past-events-dispatched-callback!]]
   [district.shared.error-handling :refer [try-catch]]
   [memefactory.server.db :as db]
   [memefactory.server.generator]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.server.ipfs :as ipfs]
   [district.shared.async-helpers :refer [promise->]]
   [memefactory.server.ranks-cache :as ranks-cache]
   [memefactory.server.utils :as server-utils]
   [memefactory.shared.contract.registry-entry :refer [vote-options]]
   [mount.core :as mount :refer [defstate]]
   [print.foo :refer [look] :include-macros true]
   [taoensso.timbre :as log]))

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
  (let [{:keys [:challenge/challenger :reg-entry/challenge-period-end :challenge/reveal-period-end] :as re}
        (db/get-registry-entry {:reg-entry/address address} [:reg-entry/created-on :reg-entry/challenge-period-end :challenge/challenger
                                                             :challenge/commit-period-end :challenge/commit-period-end
                                                             :challenge/reveal-period-end :challenge/votes-for :challenge/votes-against])
        {:keys [:meme/number]} (db/get-meme address)]
    ;; if we have a number assigned we don't do anything
    (when-not number
      (if (not challenger)
        (assign-next-number! address)
        (if (> (server-utils/now-in-seconds) reveal-period-end)
          (when (= (shared-utils/reg-entry-status (server-utils/now-in-seconds) re)
                   :reg-entry.status/whitelisted)
            (assign-next-number! address))
          (schedule-meme-number-assigner address (inc (- reveal-period-end (server-utils/now-in-seconds)))))))))

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

(defn meme-sanity-check [{:keys [:reg-entry/address
                                 :meme/meta-hash
                                 :meme/total-supply
                                 :meme/total-minted]}]
  (let [add-error (fn [m err-txt] (assoc m :errors (conj (:errors m) err-txt)))]
    (cond-> {:errors []}
      (not (ipfs/ipfs-hash? meta-hash)) (add-error "Malformed ipfs meta hash")
      (not (web3/address? address)) (add-error "Malformed registry entry address")
      (not (int? total-supply)) (add-error "Malformed total-suply number")
      (not (int? total-minted)) (add-error "Malformed total minted number"))))

(defn meme-constructed-event [_ {:keys [:args] :as event}]
  (let [{:keys [:registry-entry :timestamp :creator :meta-hash
                :total-supply :version :deposit :challenge-period-end]} args
        registry-entry-data {:reg-entry/address registry-entry
                             :reg-entry/creator creator
                             :reg-entry/version version
                             :reg-entry/created-on timestamp
                             :reg-entry/deposit (bn/number deposit)
                             :reg-entry/challenge-period-end (bn/number challenge-period-end)}
        meme {:reg-entry/address registry-entry
              :meme/meta-hash (web3/to-ascii meta-hash)
              :meme/total-supply (bn/number total-supply)
              :meme/total-minted 0}
        {:keys [:meme/meta-hash]} meme
        errors (meme-sanity-check meme)]
    (if-not (empty? (:errors errors))
      (do
        (log/warn (str "Dropping smart contract event " (:event event))
                  (merge errors registry-entry-data meme)
                  ::meme-constructed-event)
        (js/Promise.resolve errors))
      (promise-> (js/Promise.resolve (db/insert-registry-entry! registry-entry-data))
                 #(db/upsert-user! {:user/address creator})
                 #(server-utils/get-ipfs-meta @ipfs/ipfs meta-hash)
                 (fn [meme-meta]
                   (let [{:keys [:title :image-hash :search-tags :comment]} meme-meta]
                     (db/insert-meme! (merge meme {:reg-entry/address registry-entry
                                                   :meme/comment comment
                                                   :meme/image-hash image-hash
                                                   :meme/title title}))
                     (schedule-meme-number-assigner registry-entry (inc (- (bn/number challenge-period-end)
                                                                           (server-utils/now-in-seconds))))
                     (when search-tags
                       (doseq [t (into #{} search-tags)]
                         (db/tag-meme! {:reg-entry/address registry-entry :tag/name t})))))))))

(defn param-change-constructed-event [_ {:keys [:args]}]
  (try-catch
   (let [{:keys [:registry-entry :creator :version :deposit :challenge-period-end :db :key :value :timestamp :meta-hash]} args
         hash (web3/to-ascii meta-hash)]
      (add-registry-entry {:reg-entry/address registry-entry
                           :reg-entry/creator creator
                           :reg-entry/version version
                           :reg-entry/created-on timestamp
                           :reg-entry/deposit (bn/number deposit)
                           :reg-entry/challenge-period-end (bn/number challenge-period-end)}
                          timestamp)
      (promise-> (server-utils/get-ipfs-meta @ipfs/ipfs hash)
                 (fn [param-meta]
                   (let [{:keys [reason]} param-meta]
                     (db/insert-or-replace-param-change!
                      {:reg-entry/address registry-entry
                       :param-change/db db
                       :param-change/key key
                       :param-change/value (bn/number value)
                       :param-change/original-value (:param/value (db/get-param key db))
                       :param-change/reason reason
                       :param-change/meta-hash hash})))))))

(defn param-change-applied-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp]} args]
    (js/Promise.resolve (db/update-param-change! {:reg-entry/address registry-entry
                                                  :param-change/applied-on timestamp}))))

(defn challenge-created-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :challenger :commit-period-end
                :reveal-period-end :reward-pool :metahash :timestamp :version]} args
        challenge {:reg-entry/address registry-entry
                   :challenge/challenger challenger
                   :challenge/commit-period-end (bn/number commit-period-end)
                   :challenge/reveal-period-end (bn/number reveal-period-end)
                   :challenge/reward-pool (bn/number reward-pool)
                   :challenge/meta-hash (web3/to-ascii metahash)}
        registry-entry {:reg-entry/address registry-entry
                        :reg-entry/version version}]
    (promise-> (js/Promise.resolve (db/upsert-user! {:user/address challenger}))
               #(server-utils/get-ipfs-meta @ipfs/ipfs (:challenge/meta-hash challenge))
               #(db/update-registry-entry! (merge registry-entry
                                                  challenge
                                                  {:challenge/created-on timestamp
                                                   :challenge/comment (:comment %)})))))

(defn vote-committed-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp :voter :amount]} args
        vote {:reg-entry/address registry-entry
              :vote/voter voter
              :vote/amount (bn/number amount)
              :vote/option 0}]
    (promise-> (js/Promise.resolve (db/upsert-user! {:user/address voter}))
               #(db/insert-vote! (merge vote {:vote/created-on timestamp})))))

(defn vote-revealed-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp :version :voter :option]} args]
    (promise-> (js/Promise.all [(js/Promise.resolve (db/get-vote {:reg-entry/address registry-entry :vote/voter voter} [:vote/voter :reg-entry/address :vote/amount]))
                                (js/Promise.resolve (db/get-registry-entry {:reg-entry/address registry-entry} [:challenge/votes-against :challenge/votes-for :challenge/votes-total]))])
               (fn [[vote re]]
                 (let [{:keys [:vote/amount :vote/option :challenge/votes-for]} (merge vote
                                                                                       {:vote/option (bn/number option)
                                                                                        :vote/revealed-on timestamp})
                       {:keys [:challenge/votes-total :challenge/votes-against]} re
                       amount (bn/number (or amount 0))
                       votes-total (bn/number (or votes-total 0))
                       votes-against (bn/number (or votes-against 0))
                       votes-for (bn/number (or votes-for 0))]
                   (promise-> (js/Promise.resolve (db/update-registry-entry! (cond-> {:reg-entry/address registry-entry
                                                                                      :reg-entry/version version}

                                                                               true
                                                                               (assoc :challenge/votes-total (+ votes-total amount))

                                                                               (= (vote-options option) :vote.option/vote-against)
                                                                               (assoc :challenge/votes-against (+ votes-against amount))

                                                                               (= (vote-options option) :vote.option/vote-for)
                                                                               (assoc :challenge/votes-for (+ votes-for amount)))))
                              #(db/update-vote! (merge vote
                                                       {:vote/option (bn/number option)
                                                        :vote/revealed-on timestamp})))))
               #(db/upsert-user! {:user/address voter}))))

(defn vote-reward-claimed-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp :version :voter :amount]} args]
    (promise-> (js/Promise.resolve (db/update-vote! {:reg-entry/address registry-entry
                                                     :vote/voter voter
                                                     :vote/claimed-reward-on timestamp}))
               #(db/inc-user-field! voter :user/voter-total-earned (bn/number amount)))))

(defn vote-amount-reclaimed-event [_ {:keys [:args]} ]
  (let [{:keys [:registry-entry :timestamp :version :voter :amount]} args]
    (js/Promise.resolve (db/update-vote! {:reg-entry/address registry-entry
                                          :vote/voter voter
                                          :vote/reclaimed-amount-on timestamp}))))

(defn challenge-reward-claimed-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp :version :challenger :amount]} args]
    (promise-> (js/Promise.resolve
                (db/get-registry-entry {:reg-entry/address registry-entry} [:challenge/challenger :reg-entry/deposit]))
               (fn [{:keys [:challenge/challenger :reg-entry/deposit]}]
                 (db/update-registry-entry! {:reg-entry/address registry-entry
                                             :challenge/claimed-reward-on timestamp
                                             :challenge/reward-amount (bn/number amount)}))
               (db/inc-user-field! challenger :user/challenger-total-earned (bn/number amount)))))

(defn meme-minted-event [_ {:keys [:args]}]
  (let [{:keys [:registry-entry :timestamp :version :creator :token-start-id :token-end-id :total-minted]} args
        token-start-id (bn/number token-start-id)
        token-end-id (bn/number token-end-id)
        total-minted (bn/number total-minted)]
    (promise-> (js/Promise.resolve (db/insert-meme-tokens! {:token-id-start token-start-id
                                                            :token-id-end token-end-id
                                                            :reg-entry/address registry-entry}))
               #(db/update-meme! {:reg-entry/address registry-entry
                                  :meme/first-mint-on timestamp
                                  :meme/token-id-start token-start-id
                                  :meme/total-minted total-minted})
               #(map (fn [tid]
                       (db/upsert-meme-token-owner! {:meme-token/token-id tid
                                                     :meme-token/owner creator
                                                     :meme-token/transferred-on timestamp}))
                     (range token-start-id (inc token-end-id))))))

(defn meme-auction-started-event [_ {:keys [:args]}]
  (let [{:keys [:meme-auction :timestamp :meme-auction :token-id :seller :start-price :end-price :duration :description :started-on]} args]
    (js/Promise.resolve (db/insert-meme-auction! {:meme-auction/address meme-auction
                                                  :meme-auction/token-id (bn/number token-id)
                                                  :meme-auction/seller seller
                                                  :meme-auction/start-price (bn/number start-price)
                                                  :meme-auction/end-price (bn/number end-price)
                                                  :meme-auction/duration (bn/number duration)
                                                  :meme-auction/description description
                                                  :meme-auction/started-on (bn/number started-on)}))))

(defn meme-auction-canceled-event [_ {:keys [:args]}]
  (let [{:keys [:meme-auction :timestamp :token-id]} args]
    (js/Promise.resolve (db/update-meme-auction! {:meme-auction/address meme-auction
                                                  :meme-auction/canceled-on timestamp}))))

(defn meme-auction-buy-event [_ {:keys [:args]}]
  (let [{:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds]} args
        auction (db/get-meme-auction meme-auction)
        reg-entry-address (-> (db/get-meme-by-auction-address meme-auction)
                              :reg-entry/address)
        seller-address (:meme-auction/seller auction)
        {:keys [:user/best-single-card-sale]} (db/get-user {:user/address seller-address}
                                                           [:user/best-single-card-sale])]
    (promise-> (js/Promise.resolve (db/upsert-user! {:user/address seller-address
                                                     :user/best-single-card-sale (max best-single-card-sale
                                                                                      (bn/number seller-proceeds))}))
               #(db/upsert-user! {:user/address buyer})
               #(db/inc-user-field! (:meme-auction/seller auction) :user/total-earned (bn/number seller-proceeds))
               #(db/inc-meme-field! reg-entry-address :meme/total-trade-volume (bn/number price))
               #(db/update-meme-auction! {:meme-auction/address meme-auction
                                          :meme-auction/bought-for (bn/number price)
                                          :meme-auction/bought-on timestamp
                                          :meme-auction/buyer buyer}))))

(defn meme-token-transfer-event [_ {:keys [:args]}]
  (let [{:keys [:_to :_token-id :_timestamp]} args]
    (js/Promise.resolve (db/upsert-meme-token-owner! {:meme-token/token-id (bn/number _token-id)
                                                      :meme-token/owner _to
                                                      :meme-token/transferred-on (bn/number _timestamp)}))))

(defn eternal-db-event [_ {:keys [:args :address]}]
  (let [{:keys [:records :values :timestamp]} args
        records->values (zipmap records values)
        keys->values (->> #{"challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration" "deposit"
                            "challengeDispensation" "voteQuorum" "maxTotalSupply" "maxAuctionDuration"}
                          (map (fn [k] (when-let [v (records->values (web3/sha3 k))] [k v])))
                          (into {}))
        rows (reduce (fn [res [k v]]
                       (conj res {:initial-param/key k
                                  :initial-param/db address
                                  :initial-param/value (bn/number v)
                                  :initial-param/set-on timestamp}))
                     []
                     keys->values)]
    (js/Promise.resolve (db/insert-initial-params! rows))))

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
  (js/Promise.
   (fn [resolve reject]
     (web3-eth/get-block @web3 block-number false (fn [err {:keys [:timestamp] :as res}]
                                                    (if err
                                                      (reject err)
                                                      (do
                                                        (log/debug "cache miss for block-timestamp" {:block-number block-number :timestamp timestamp})
                                                        (resolve timestamp))))))))

(def block-timestamp
  (memoize block-timestamp*))

(defn- dispatcher [callback]
  (fn [err {:keys [:block-number] :as event}]
    (-> (block-timestamp block-number)
        (.then (fn [block-timestamp]
                 (let [event (-> event
                                 (update :event cs/->kebab-case)
                                 (update-in [:args :timestamp] (fn [timestamp]
                                                                 (if timestamp
                                                                   (bn/number timestamp)
                                                                   block-timestamp)))
                                 (update-in [:args :version] bn/number))
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
                     (do
                       (log/info "Handling new event" evt)
                       (promise-> (callback err event)
                                  #(db/upsert-event! {:event/last-log-index log-index
                                                      :event/last-block-number block-number
                                                      :event/count (inc count)
                                                      :event/event-name event-name
                                                      :event/contract-key contract-key})
                                  (constantly ::processed)))
                     (do
                       (log/info "Skipping handling of a persisted event" evt)
                       (js/Promise.resolve ::skipped))))))
        (.catch (fn [error]
                  (log/error "Exception when handling event" {:error error
                                                              :event event})
                  (js/Promise.resolve ::error))))))

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

    (log/info "Schedulling number assigner to  memes already on db "
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
  (when-not (:disabled? opts)
    (when-not (web3/connected? @web3)
      (throw (js/Error. "Can't connect to Ethereum node")))
    (when-not (= ::db/started @db/memefactory-db)
      (throw (js/Error. "Database module has not started")))
    (let [event-callbacks
          {:param-change-db/eternal-db-event eternal-db-event
           :param-change-registry/param-change-constructed-event param-change-constructed-event
           :param-change-registry/challenge-created-event challenge-created-event
           :param-change-registry/vote-committed-event vote-committed-event
           :param-change-registry/vote-revealed-event vote-revealed-event
           :param-change-registry/vote-amount-claimed-event vote-amount-reclaimed-event
           :param-change-registry/vote-reward-claimed-event vote-reward-claimed-event
           :param-change-registry/challenge-reward-claimed-event challenge-reward-claimed-event
           :param-change-registry/param-change-applied-event param-change-applied-event
           :meme-registry-db/eternal-db-event eternal-db-event
           :meme-registry/meme-constructed-event meme-constructed-event
           :meme-registry/challenge-created-event challenge-created-event
           :meme-registry/vote-committed-event vote-committed-event
           :meme-registry/vote-revealed-event vote-revealed-event
           :meme-registry/vote-amount-claimed-event vote-amount-reclaimed-event
           :meme-registry/vote-reward-claimed-event vote-reward-claimed-event
           :meme-registry/challenge-reward-claimed-event challenge-reward-claimed-event
           :meme-registry/meme-minted-event meme-minted-event
           :meme-auction-factory/meme-auction-started-event meme-auction-started-event
           :meme-auction-factory/meme-auction-buy-event meme-auction-buy-event
           :meme-auction-factory/meme-auction-canceled-event meme-auction-canceled-event
           :meme-token/transfer meme-token-transfer-event}
          start-time (server-utils/now)
          callback-ids (doseq [[event-key callback] event-callbacks]
                         (register-callback! event-key (dispatcher callback)))]
      (register-after-past-events-dispatched-callback! (fn []
                                                         (log/warn "Syncing past events finished" (time/time-units (- (server-utils/now) start-time)) ::start)
                                                         (apply-blacklist-patches!)
                                                         (assign-meme-registry-numbers!)))
      (assoc opts :callback-ids callback-ids))))

(defn stop [syncer]
  (when-not (:disabled? @syncer)
    (unregister-callbacks! (:callback-ids @syncer))))
