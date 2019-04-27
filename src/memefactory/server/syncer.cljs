(ns memefactory.server.syncer
  (:require
    [bignumber.core :as bn]
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
    [memefactory.server.graphql-resolvers :refer [reg-entry-status]]
    [memefactory.server.ipfs :as ipfs]
    [memefactory.server.macros :refer [promise->]]
    [memefactory.server.ranks-cache :as ranks-cache]
    [memefactory.server.utils :as server-utils]
    [memefactory.shared.contract.registry-entry :refer [vote-options]]
    [mount.core :as mount :refer [defstate]]
    [print.foo :refer [look] :include-macros true]
    [taoensso.timbre :as log])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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
          (when (= (reg-entry-status (server-utils/now-in-seconds) re)
                   :reg-entry.status/whitelisted)
            (assign-next-number! address))

          (schedule-meme-number-assigner address (inc (- reveal-period-end (server-utils/now-in-seconds)))))))))


(defn schedule-meme-number-assigner [address seconds]
  (when (pos? seconds)
    (log/info (str "Scheduling meme-number-assigner for meme " address " in " seconds " seconds"))
    (js/setTimeout (partial meme-number-assigner address)
                   (* 1000 seconds))))


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


(defn meme-constructed-event [_ {:keys [:args] :as ev}]
  (let [{:keys [:registry-entry :timestamp :creator :meta-hash
                :total-supply :version :deposit :challenge-period-end]} args]
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

        (if-not (empty? (:errors errors))                     ;; sanity check
          (log/warn (str "Dropping smart contract event " (:event ev))
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
              (promise-> (server-utils/get-ipfs-meta @ipfs/ipfs meta-hash)
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
                                 (db/tag-meme! (:reg-entry/address meme) t)))))))))))))


(defn param-change-constructed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :creator :version :deposit :challenge-period-end :db :key :value :timestamp]} args]
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
         :param-change/initial-value (:initial-param/value (db/get-initial-param key db))}))))


(defn param-change-applied-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp]} args]
      (db/update-param-change! {:reg-entry/address registry-entry
                                :param-change/applied-on timestamp}))))


(defn challenge-created-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :challenger :commit-period-end
                  :reveal-period-end :reward-pool :metahash :timestamp :version]} args]
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
        (promise-> (server-utils/get-ipfs-meta @ipfs/ipfs (:challenge/meta-hash challenge))
                   (fn [challenge-meta]
                     (db/update-registry-entry! (merge registry-entry
                                                       {:challenge/comment (:comment challenge-meta)}))))))))


(defn vote-committed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :voter :amount]} args]
      (let [vote {:reg-entry/address registry-entry
                  :vote/voter voter
                  :vote/amount (bn/number amount)
                  :vote/option 0}]                          ;; No vote
        (db/update-user! {:user/address voter})
        (db/get-registry-entry {:reg-entry/address registry-entry} [:reg-entry/created-on :reg-entry/address])
        (db/insert-vote! (merge vote {:vote/created-on timestamp}))))))


(defn vote-revealed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :version :voter :option]} args]
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
        (db/update-vote! vote)))))


(defn vote-reward-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :version :voter :amount]} args]
      (let [vote {:reg-entry/address registry-entry
                  :vote/voter voter
                  :vote/claimed-reward-on timestamp}]
        (db/update-vote! vote)
        (db/inc-user-field! voter :user/voter-total-earned (bn/number amount))))))


(defn vote-amount-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :version :voter :amount]} args]
      (let [vote {:reg-entry/address registry-entry
                  :vote/voter voter
                  :vote/reclaimed-amount-on timestamp}]
        (db/update-vote! vote)))))


(defn challenge-reward-claimed-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :version :challenger :amount]} args]
      (let [reg-entry (db/get-registry-entry {:reg-entry/address registry-entry}
                                             [:challenge/challenger :reg-entry/deposit])
            {:keys [:challenge/challenger :reg-entry/deposit]} reg-entry]
        (db/update-registry-entry! {:reg-entry/address registry-entry
                                    :challenge/claimed-reward-on timestamp
                                    :challenge/reward-amount (bn/number amount)})
        (db/inc-user-field! (:challenger args) :user/challenger-total-earned (bn/number amount))))))


(defn meme-minted-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:registry-entry :timestamp :version :creator :token-start-id :token-end-id :total-minted]} args]
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
        (db/update-meme-total-minted! registry-entry total-minted)))))


(defn meme-auction-started-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:meme-auction :timestamp :meme-auction :token-id :seller :start-price :end-price :duration :description :started-on]} args]
      (let [meme-auction {:meme-auction/address meme-auction
                          :meme-auction/token-id (bn/number token-id)
                          :meme-auction/seller seller
                          :meme-auction/start-price (bn/number start-price)
                          :meme-auction/end-price (bn/number end-price)
                          :meme-auction/duration (bn/number duration)
                          :meme-auction/description description
                          :meme-auction/started-on (bn/number started-on)}]
        (db/insert-or-update-meme-auction! meme-auction)))))


(defn meme-auction-buy-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds]} args]
      (let [auction (db/get-meme-auction meme-auction)
            reg-entry-address (-> (db/get-meme-by-auction-address meme-auction)
                                :reg-entry/address)
            seller-address (:meme-auction/seller auction)
            {:keys [:user/best-single-card-sale]} (db/get-user {:user/address seller-address}
                                                               [:user/best-single-card-sale])]
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
                                            :meme-auction/buyer buyer})))))


(defn meme-auction-canceled-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:meme-auction :timestamp]} args]
      (db/insert-or-update-meme-auction! {:meme-auction/address meme-auction
                                          :meme-auction/canceled-on timestamp}))))



(defn meme-token-transfer-event [_ {:keys [:args]}]
  (try-catch
    (let [{:keys [:_to :_token-id :_timestamp]} args]
      (db/insert-or-replace-meme-token-owner {:meme-token/token-id (bn/number _token-id)
                                              :meme-token/owner _to
                                              :meme-token/transferred-on (bn/number _timestamp)}))))


(defn eternal-db-event [_ {:keys [:args :address]}]
  (try-catch
    (let [{:keys [:records :values :timestamp]} args
          records->values (zipmap records values)
          keys->values (->> #{"challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration" "deposit"
                              "challengeDispensation" "voteQuorum" "maxTotalSupply" "maxAuctionDuration"}
                         (map (fn [k] (when-let [v (records->values (web3/sha3 k))] [k v])))
                         (into {}))]
      (doseq [[k v] keys->values]
        (db/insert-initial-param! {:initial-param/key k
                                   :initial-param/db address
                                   :initial-param/value (bn/number v)
                                   :initial-param/set-on timestamp})))))


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


(defn- dispatcher [callback]
  (fn [err event]
    (-> event
      (update-in [:args :timestamp] (fn [timestamp]
                                      (if timestamp
                                        (bn/number timestamp)
                                        (:timestamp (web3-eth/get-block @web3 (:block-number event))))))
      (update-in [:args :version] bn/number)
      (->> (callback err)))))


(defn- assign-meme-registry-numbers! []
  ;; if there are any memes with unasigned numbers but still assignable
  ;; start the number assigners
  (let [assignable-reg-entries (filter #(contains? #{:reg-entry.status/challenge-period
                                                     :reg-entry.status/commit-period
                                                     :reg-entry.status/reveal-period}
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
                                                     (server-utils/now-in-seconds)))))))


(defn start [opts]
  (when-not (:disabled? opts)

    (when-not (web3/connected? @web3)
      (throw (js/Error. "Can't connect to Ethereum node")))

    (when-not (= ::db/started @db/memefactory-db)
      (throw (js/Error. "Database module has not started")))

    (let [event-callbacks
          {:param-change-db/eternal-db-event eternal-db-event
           :param-change-registry/param-change-constructed-event param-change-constructed-event
           :param-change-registry/param-change-applied-event param-change-applied-event
           :meme-registry-db/eternal-db-event eternal-db-event
           :meme-registry/meme-constructed-event meme-constructed-event
           :meme-registry/meme-minted-event meme-minted-event
           :meme-registry/challenge-created-event challenge-created-event
           :meme-registry/vote-committed-event vote-committed-event
           :meme-registry/vote-revealed-event vote-revealed-event
           :meme-registry/vote-amount-claimed-event vote-amount-claimed-event
           :meme-registry/vote-reward-claimed-event vote-reward-claimed-event
           :meme-registry/challenge-reward-claimed-event challenge-reward-claimed-event
           :meme-auction-factory/meme-auction-started-event meme-auction-started-event
           :meme-auction-factory/meme-auction-buy-event meme-auction-buy-event
           :meme-auction-factory/meme-auction-canceled-event meme-auction-canceled-event
           :meme-token/transfer meme-token-transfer-event}
          callback-ids (doseq [[event-key callback] event-callbacks]
                         (register-callback! event-key (dispatcher callback)))]


      (register-after-past-events-dispatched-callback! (fn []
                                                         (apply-blacklist-patches!)
                                                         (assign-meme-registry-numbers!)))

      (assoc opts :callback-ids callback-ids))))

(defn stop [syncer]
  (when-not (:disabled? @syncer)
    (unregister-callbacks! (:callback-ids @syncer))))
