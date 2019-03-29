(ns memefactory.server.syncer
  (:require
   [bignumber.core :as bn]
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs.core.async :as async]
   [cljs.nodejs :as nodejs]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts :refer [replay-past-events-in-order]]
   [district.server.web3 :refer [web3]]
   [district.shared.error-handling :refer [try-catch]]
   [district.web3-utils :as web3-utils]
   [memefactory.server.contract.eternal-db :as eternal-db]
   [memefactory.server.contract.meme :as meme]
   [memefactory.server.emailer :as emailer]
   [memefactory.server.contract.meme-auction :as meme-auction]
   [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
   [memefactory.server.contract.meme-token :as meme-token]
   [memefactory.server.contract.param-change :as param-change]
   [memefactory.server.contract.registry :as registry]
   [memefactory.server.contract.registry-entry :as registry-entry]
   [memefactory.server.db :as db]
   [memefactory.server.generator]
   [memefactory.server.graphql-resolvers :refer [reg-entry-status]]
   [memefactory.server.ipfs :as ipfs]
   [memefactory.server.macros :refer [promise->]]
   [memefactory.server.ranks-cache :as ranks-cache]
   [memefactory.server.utils :as server-utils]
   [memefactory.shared.contract.registry-entry :refer [vote-options]]
   [memefactory.shared.smart-contracts :as sc]
   [mount.core :as mount :refer [defstate]]
   [print.foo :refer [look] :include-macros true]
   [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; this HACK is because mount doesn't support async, so mount start will return
;; a chan. We then store created filters in an atom so we can stop them at component stop.
(defonce all-filters (atom []))

(declare start)
(declare stop)

(defstate ^{:on-reload :noop}
  syncer
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
    (log/info (str "Meme" address " got number " (inc current-meme-number) " assigned."))))

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
          (when (= (reg-entry-status (server-utils/now-in-seconds) re)
                   :reg-entry.status/whitelisted)
            (assign-next-number! address))

          (schedule-meme-number-assigner address (inc (- reveal-period-end (server-utils/now-in-seconds)))))))))

(defn schedule-meme-number-assigner [address seconds]
  (when (pos? seconds)
    (log/info (str "Scheduling meme-number-assigner for meme " address " in " seconds " seconds"))
    (js/setTimeout (partial meme-number-assigner address)
                   (* 1000 seconds))))


(defn get-ipfs-meta [meta-hash]
  (js/Promise.
   (fn [resolve reject]
     (log/info (str "Downloading: " "/ipfs/" meta-hash) ::get-ipfs-meta)
     (ipfs-files/fget (str "/ipfs/" meta-hash)
                      {:req-opts {:compress false}}
                      (fn [err content]
                        (cond
                          err
                          (let [err-txt "Error when retrieving metadata from ipfs"]
                            (log/error err-txt (merge {:meta-hash meta-hash
                                                       :connection @ipfs/ipfs
                                                       :error err})
                                       ::get-ipfs-meta)
                            (reject (str err-txt " : " err)))

                          (empty? content)
                          (let [err-txt "Empty ipfs content"]
                            (log/error err-txt {:meta-hash meta-hash
                                                :connection @ipfs/ipfs} ::get-ipfs-meta)
                            (reject err-txt))

                          :else (-> (re-find #".+(\{.+\})" content)
                                    second
                                    js/JSON.parse
                                    (js->clj :keywordize-keys true)
                                    resolve)))))))

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


(defmulti process-event (fn [contract-type ev] [contract-type (:event ev)]))

(defn meme-sanity-check [{:keys [:reg-entry/address
                                 :meme/meta-hash
                                 :meme/total-supply
                                 :meme/total-minted] :as meme}]
  (let [add-error (fn [m err-txt] (assoc m :errors (conj (:errors m) err-txt)))]
    (cond-> {:errors []}
      (not (ipfs/ipfs-hash? meta-hash)) (add-error "Malformed ipfs meta hash")
      (not (web3/address? address)) (add-error "Malformed registry entry address")
      (not (int? total-supply)) (add-error "Malformed total-suply number")
      (not (int? total-minted)) (add-error "Malformed total minted number"))))

(defmethod process-event [:contract/meme :MemeConstructedEvent]
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
               :meme/total-minted 0}
         errors (meme-sanity-check meme)]

     (if-not (empty? (:errors errors)) ;; sanity check
       (log/warn (str "Dropping smart contract event "  contract-type " " (:event ev))
                 (merge errors registry-entry-data meme)
                 ::MemeConstructedEvent)

       (do
         (add-registry-entry registry-entry-data timestamp)

         (db/update-user! {:user/address creator})

         ;; This HACK is added so we can insert the meme before waiting for ipfs to return
         ;; To fix the whole thing we need to make process-event async and replay-past-events-ordered
         ;; "async aware"
         (db/insert-meme! (assoc meme
                                 :meme/title ""
                                 :meme/image-hash ""))

         (let [{:keys [:meme/meta-hash]} meme]
           (promise-> (get-ipfs-meta meta-hash)
                      (fn [meme-meta]
                        (let [{:keys [title image-hash search-tags comment]} meme-meta]
                          (db/update-meme! {:reg-entry/address registry-entry
                                            :meme/comment comment
                                            :meme/image-hash image-hash
                                            :meme/title title})
                          (schedule-meme-number-assigner registry-entry (inc (- (bn/number challenge-period-end)
                                                                                (server-utils/now-in-seconds))))
                          (when search-tags
                            (doseq [t (into #{} search-tags)]
                              (db/tag-meme! (:reg-entry/address meme) t))))))))))))

(defmethod process-event [:contract/param-change :ParamChangeConstructedEvent]
  [contract-type {:keys [:registry-entry :creator :version :deposit :challenge-period-end :db :key :value :timestamp] :as ev}]
  (try-catch
   (add-registry-entry {:reg-entry/address registry-entry
                        :reg-entry/creator creator
                        :reg-entry/version version
                        :reg-entry/created-on timestamp
                        :reg-entry/deposit (bn/number deposit)
                        :reg-entry/challenge-period-end (bn/number challenge-period-end)}
                       timestamp)
   ;; TODO Fix this call
   (add-param-change {:reg-entry/address registry-entry
                      :param-change/db db
                      :param-change/key key
                      :param-change/value (bn/number value)
                      :param-change/initial-value nil})))

(defmethod process-event [:contract/param-change :change-applied]
  [contract-type {:keys [:registry-entry :timestamp] :as ev}]
  (try-catch
   (add-param-change registry-entry)))

(defmethod process-event [:contract/registry-entry :ChallengeCreatedEvent]
  [_ {:keys [:registry-entry :challenger :commit-period-end
             :reveal-period-end :reward-pool :metahash :timestamp :version] :as ev}]
  (emailer/send-challenge-created-email ev)
  (let [challenge {:reg-entry/address registry-entry
                   :challenge/challenger challenger
                   :challenge/commit-period-end (bn/number commit-period-end)
                   :challenge/reveal-period-end (bn/number reveal-period-end)
                   :challenge/reward-pool (bn/number reward-pool)
                   :challenge/meta-hash (web3/to-ascii metahash)}
        registry-entry {:reg-entry/address registry-entry
                        :reg-entry/version version}]
    (db/update-user! {:user/address challenger})
    (db/update-registry-entry! (merge registry-entry
                                      challenge
                                      {:challenge/created-on timestamp}))
    (promise-> (get-ipfs-meta (:challenge/meta-hash challenge))
               (fn [challenge-meta]
                 (db/update-registry-entry! (merge registry-entry
                                                   {:challenge/comment (:comment challenge-meta)}))))))

(defmethod process-event [:contract/registry-entry :VoteCommittedEvent]
  [_ {:keys [:registry-entry :timestamp :voter :amount] :as ev}]
  (try-catch
   (let [vote {:reg-entry/address registry-entry
               :vote/voter voter
               :vote/amount (bn/number amount)
               :vote/option 0}] ;; No vote
     (db/update-user! {:user/address voter})
     (db/insert-vote! (merge vote {:vote/created-on timestamp})))))

(defmethod process-event [:contract/registry-entry :VoteRevealedEvent]
  [_ {:keys [:registry-entry :timestamp :version :voter :option] :as ev}]
  (try-catch
   (let [vote (merge (db/get-vote {:reg-entry/address registry-entry :vote/voter voter} [:vote/voter :reg-entry/address :vote/amount])
                     {:vote/option (bn/number option)
                      :vote/revealed-on timestamp})
         re (db/get-registry-entry {:reg-entry/address registry-entry} [:challenge/votes-against :challenge/votes-for])]
     (db/update-registry-entry! (cond-> {:reg-entry/address registry-entry
                                         :reg-entry/version version}
                                  (= (vote-options (:vote/option vote))
                                     :vote.option/vote-against) (assoc :challenge/votes-against (+ (or (:challenge/votes-against re) 0)
                                                                                                   (:vote/amount vote)))
                                  (= (vote-options (:vote/option vote))
                                     :vote.option/vote-for) (assoc :challenge/votes-for (+ (or (:challenge/votes-for re) 0)
                                                                                           (:vote/amount vote)))))
     (db/update-user! {:user/address voter})
     (db/update-vote! vote))))

(defmethod process-event [:contract/registry-entry :VoteRewardClaimedEvent]
  [_ {:keys [:registry-entry :timestamp :version :voter :amount] :as ev}]
  (emailer/send-vote-reward-claimed-email ev)
  (let [vote {:reg-entry/address registry-entry
              :vote/voter voter
              :vote/claimed-reward-on timestamp}]
    (db/update-vote! vote)
    (db/inc-user-field! voter :user/voter-total-earned (bn/number amount))))

(defmethod process-event [:contract/registry-entry :VoteAmountClaimedEvent]
  [_ {:keys [:registry-entry :timestamp :version :voter :amount] :as ev}]
  (try-catch
   (let [vote {:reg-entry/address registry-entry
               :vote/voter voter
               :vote/reclaimed-amount-on timestamp}]
     (db/update-vote! vote))))

(defmethod process-event [:contract/registry-entry :ChallengeRewardClaimedEvent]
  [_ {:keys [:registry-entry :timestamp :version :challenger :amount] :as ev}]
  (emailer/send-challenge-reward-claimed-email ev)
  (let [reg-entry (db/get-registry-entry {:reg-entry/address registry-entry}
                                         [:challenge/challenger :reg-entry/deposit])
        {:keys [:challenge/challenger :reg-entry/deposit]} reg-entry]

    (db/update-registry-entry! {:reg-entry/address registry-entry
                                :challenge/claimed-reward-on timestamp
                                :challenge/reward-amount (bn/number amount)})
    (db/inc-user-field! (:challenger ev) :user/challenger-total-earned (bn/number amount))))

(defmethod process-event [:contract/meme :MemeMintedEvent]
  [_ {:keys [:registry-entry :timestamp :version :creator :token-start-id :token-end-id :total-minted] :as ev}]
  (try-catch
   (let [timestamp timestamp
         token-start-id (bn/number token-start-id)
         token-end-id (bn/number token-end-id)
         total-minted (bn/number total-minted)]

     (db/insert-meme-tokens! {:token-id-start token-start-id
                              :token-id-end token-end-id
                              :reg-entry/address registry-entry})

     (doseq [tid (range token-start-id (inc token-end-id))]
       (db/insert-or-replace-meme-token-owner {:meme-token/token-id tid
                                               :meme-token/owner creator
                                               :meme-token/transferred-on timestamp}))

     (db/update-meme-first-mint-on! registry-entry timestamp)
     (db/update-meme-token-id-start! registry-entry token-start-id)
     (db/update-meme-total-minted! registry-entry total-minted))))

(defmethod process-event [:contract/meme-auction :MemeAuctionStartedEvent]
  [_ {:keys [:meme-auction :timestamp :meme-auction :token-id :seller :start-price :end-price :duration :description :started-on] :as ev}]
  (try-catch
   (let [meme-auction {:meme-auction/address meme-auction
                       :meme-auction/token-id (bn/number token-id)
                       :meme-auction/seller seller
                       :meme-auction/start-price (bn/number start-price)
                       :meme-auction/end-price (bn/number end-price)
                       :meme-auction/duration (bn/number duration)
                       :meme-auction/description description
                       :meme-auction/started-on (bn/number started-on)}]
     (db/insert-or-update-meme-auction! meme-auction))))

(defmethod process-event [:contract/meme-auction :MemeAuctionBuyEvent]
  [_ {:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds] :as ev}]
  (emailer/send-auction-bought-email ev)
  (let [auction (db/get-meme-auction meme-auction)
        reg-entry-address (-> (db/get-meme-by-auction-address meme-auction)
                              :reg-entry/address)
        seller-address (:meme-auction/seller auction)
        {:keys [:user/best-single-card-sale]} (db/get-user {:user/address seller-address} [:user/best-single-card-sale])]
    (db/update-user! {:user/address seller-address
                      :user/best-single-card-sale (max best-single-card-sale
                                                       (bn/number seller-proceeds))})
    (db/update-user! {:user/address buyer})
    (db/inc-user-field! (:meme-auction/seller auction) :user/total-earned (bn/number seller-proceeds))
    (db/inc-meme-total-trade-volume! {:reg-entry/address reg-entry-address
                                      :amount (bn/number price)})
    (db/insert-or-update-meme-auction! {:meme-auction/address meme-auction
                                        :meme-auction/bought-for (bn/number price)
                                        :meme-auction/bought-on timestamp
                                        :meme-auction/buyer buyer})))

(defmethod process-event [:contract/meme-auction :MemeAuctionCanceledEvent]
  [_ {:keys [meme-auction timestamp] :as ev}]
  (try-catch
   (db/insert-or-update-meme-auction! {:meme-auction/address meme-auction
                                       :meme-auction/canceled-on timestamp})))

(defmethod process-event [:contract/meme-token :Transfer]
  [_ ev]
  (try-catch
   (let [{:keys [:_to :_token-id :_timestamp]} ev]
     (db/insert-or-replace-meme-token-owner {:meme-token/token-id (bn/number _token-id)
                                             :meme-token/owner _to
                                             :meme-token/transferred-on (bn/number _timestamp)}))))

(defmethod process-event [:contract/eternal-db :EternalDbEvent]
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

;; sc/smart-contracts goes for contract-keys -> addresss,
;; this created a map the otherway around
(def contract-key-from-address (reduce
                                (fn [r [contract-key {:keys [name address]}]]
                                  (assoc r address contract-key))
                                {}
                                sc/smart-contracts))

(defn dispatch-event
  ([ev] (dispatch-event nil ev))
  ([err {:keys [args event address block-number] :as evt}]
   (if err     
     (log/error "Error when dispatching event" {:error err
                                                :filter-ids (map (fn [filt] (.-filterId filt)) @all-filters)}
                ::dispatch-event)
     (when evt
       (let [contract-key (contract-key-from-address address)
             contract-type ({:meme-registry-db         :contract/eternal-db
                             :param-change-registry-db :contract/eternal-db
                             :meme-registry-fwd        :contract/meme
                             :meme-token               :contract/meme-token
                             :meme-auction-factory     :contract/meme-auction
                             :meme-auction-factory-fwd :contract/meme-auction} contract-key)
             ev (-> args
                    (assoc :contract-address address)
                    (assoc :event (keyword event))
                    (update :timestamp (fn [ts]
                                         (if ts
                                           (bn/number ts)
                                           (:timestamp (web3-eth/get-block @web3 block-number)))))
                    (update :version bn/number)
                    (assoc :block-number block-number))]
         (log/info (str "Dispatching smart contract event "  contract-type " " (:event ev)) {:ev ev} ::dispatch-event)
         (process-event contract-type ev))))))

(defn apply-blacklist-patches! []
  (let [{:keys [blacklist-file]} @config
        blacklisted-addresses (:blacklisted-image-addresses (server-utils/load-edn-file blacklist-file))]
    (try-catch
     (doseq [address blacklisted-addresses]
       (log/info (str "Blacklisting address " address) ::apply-blacklist-patches)
       (db/patch-forbidden-reg-entry-image! address)))))

(defn start [{:keys [:on-event-error] :as opts}]

  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))

  (when-not (= ::db/started @db/memefactory-db)
    (throw (js/Error. "Database module has not started")))

  (let [last-block-number (last-block-number)
        filters-builders [(partial eternal-db/change-applied-event :param-change-registry-db)
                          (partial eternal-db/change-applied-event :meme-registry-db)

                          (partial registry/meme-constructed-event [:meme-registry :meme-registry-fwd])
                          (partial registry/meme-minted-event [:meme-registry :meme-registry-fwd])
                          (partial registry/challenge-created-event [:meme-registry :meme-registry-fwd])
                          (partial registry/vote-committed-event [:meme-registry :meme-registry-fwd])
                          (partial registry/vote-revealed-event [:meme-registry :meme-registry-fwd])
                          (partial registry/vote-amount-claimed-event [:meme-registry :meme-registry-fwd])
                          (partial registry/vote-reward-claimed-event [:meme-registry :meme-registry-fwd])
                          (partial registry/challenge-reward-claimed-event [:meme-registry :meme-registry-fwd])

                          meme-auction-factory/meme-auction-started-event
                          meme-auction-factory/meme-auction-buy-event
                          meme-auction-factory/meme-auction-canceled-event

                          meme-token/meme-token-transfer-event]

        ;; just creates the past event filters but doesn't retrieve anything
        past-events-filters (->> filters-builders
                                 (map #(apply % [{:from-block 0 :to-block (dec last-block-number)}])))]

    (go
      (when (pos? last-block-number)
        ;; use past-events-filters to retrieve past events in order
        ;; block until all replayed before installing new events filters
        (async/<! (replay-past-events-in-order past-events-filters dispatch-event)))

      (apply-blacklist-patches!)

      ;; install the watch filters and start listening
      (let [watch-filters (->> filters-builders
                               (map #(apply % ["latest" dispatch-event]))
                               doall)]

        ;; if there are any memes with unasigned numbers but still assignable
        ;; start the number assigners
        (let [assignable-reg-entries (filter #(contains? #{:reg-entry.status/challenge-period :reg-entry.status/commit-period :reg-entry.status/reveal-period}
                                                         (reg-entry-status (server-utils/now-in-seconds) %))
                                             (db/all-reg-entries))
              whitelisted-reg-entries (filter #(= :reg-entry.status/whitelisted (reg-entry-status (server-utils/now-in-seconds) %))
                                              (db/all-reg-entries))]

          ;; add numbers to all whitelisteds
          (doseq [{:keys [:reg-entry/address]} whitelisted-reg-entries]
            (assign-next-number! address))

          ;; schedule meme number assigners for all memes that need it
          (doseq [{:keys [:reg-entry/address :reg-entry/challenge-period-end :challenge/reveal-period-end]} assignable-reg-entries]
            (schedule-meme-number-assigner address (inc (- (if (> challenge-period-end (server-utils/now-in-seconds))
                                                             challenge-period-end
                                                             reveal-period-end)
                                                           (server-utils/now-in-seconds))))))
        (reset! all-filters
                watch-filters
                #_(into past-events-filters watch-filters))))

    ;; block for some time untill filters are installed
    (js/setTimeout
     #(log/info "Starting syncer. Installed filters with ids:" {:filter-ids (map (fn [filt] (.-filterId filt)) @all-filters)})
     4000)

    opts))

(defn stop [syncer]
  (log/warn "Stopping syncer" {:filter-ids (map #(-> % .-filterId) @all-filters)})
  (doseq [f (remove nil? @all-filters)]
    (server-utils/uninstall-filter f)))
