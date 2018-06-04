(ns memefactory.server.syncer
  (:require
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [replay-past-events]]
    [district.server.web3 :refer [web3]]
    [district.web3-utils :as web3-utils]
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
    [taoensso.timbre :refer-macros [info warn error]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop syncer))

(def info-text "smart-contract event")
(def error-text "smart-contract event error")

(defn on-constructed [{:keys [:registry-entry :timestamp] :as args} _ type]
  (info info-text {:args args} ::on-constructed)
  (try
    (db/insert-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                      (registry-entry/load-registry-entry-challenge registry-entry)
                                      {:reg-entry/created-on timestamp}))
    (if (= type :meme)
      (db/insert-meme! (merge (meme/load-meme registry-entry)
                              {:meme/image-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"
                               :meme/title "HapplyHarambe"}))
      (db/insert-param-change! (param-change/load-param-change registry-entry)))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-constructed))))


(defn on-challenge-created [{:keys [:registry-entry :timestamp] :as args}]
  (info info-text {:args args} ::on-challenge-created)
  (try
    (db/update-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                      (registry-entry/load-registry-entry-challenge registry-entry)
                                      {:challenge/created-on timestamp}))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-challenge-created))))


(defn on-vote-committed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-committed)
  (try
    (let [voter (web3-utils/uint->address (first data))
          vote (registry-entry/load-vote registry-entry voter)]
      (db/insert-vote! (merge vote {:vote/created-on timestamp})))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-committed))))


(defn on-vote-revealed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-revealed)
  (try
    (let [voter (web3-utils/uint->address (first data))]
      (db/update-registry-entry! (merge (registry-entry/load-registry-entry registry-entry)
                                        (registry-entry/load-registry-entry-challenge registry-entry)))
      (db/update-vote! (registry-entry/load-vote registry-entry voter)))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-revealed))))


(defn on-vote-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-reward-claimed)
  (try
    (let [voter (web3-utils/uint->address (first data))
          vote (registry-entry/load-vote registry-entry voter)]
      (do
        (db/update-vote! vote)
        (db/inc-user-field! voter :user/voter-total-earned (:vote/amount vote))))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-reward-claimed))))


(defn on-challenge-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-challenge-reward-claimed)
  (try
    (let [{:keys [:challenge/challenger :reg-entry/deposit] :as reg-entry}
          (merge (registry-entry/load-registry-entry registry-entry)
                 (registry-entry/load-registry-entry-challenge registry-entry))]

      (db/update-registry-entry! reg-entry)
      (db/inc-user-field! (:challenge/challenger reg-entry) :user/challenger-total-earned deposit))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-challenge-reward-claimed))))


(defn on-minted [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-minted)
  (try
    (let [[_ token-id-start token-id-end] data]
      (db/insert-meme-tokens! {:token-id-start (bn/number token-id-start)
                               :token-id-end (bn/number token-id-end)
                               :reg-entry/address registry-entry})
      (db/update-meme-first-mint-on! {:reg-entry/address registry-entry
                                      :meme/first-mint-on timestamp}))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-minted))))


(defn on-auction-started [{:keys [:meme-auction :timestamp :data] :as args}]
  (info info-text {:args args} ::on-auction-started)
  (try
    (db/insert-meme-auction! (meme-auction/load-meme-auction meme-auction))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-auction-started))))


(defn on-auction-buy [{:keys [:meme-auction :timestamp :data] :as args}]
  (info info-text {:args args} ::on-auction-buy)
  (try
    (let [[_ price] data
          price (bn/number price)
          {reg-entry-address :reg-entry/address
           auction-address :meme-auction/address} (meme-auction/load-meme-auction meme-auction)]
      (db/inc-meme-total-trade-volume! {:reg-entry/address reg-entry-address
                                        :amount price})
      (db/update-meme-auction! {:meme-auction/address auction-address
                                :meme-auction/bought-for price}))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-auction-buy))))


(defn on-meme-token-transfer [err {:keys [:args]}]
  (info info-text {:args args} ::on-meme-token-transfer)
  (try
    (let [{:keys [:_to :_token-id :_timestamp]} args]
      (db/insert-or-replace-meme-token-owner {:meme-token/token-id (bn/number _token-id)
                                              :meme-token/owner _to
                                              :meme-token/transferred-on (bn/number _timestamp)}))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-meme-token-transfer))))


(def registry-entry-events
  {:constructed on-constructed
   :challenge-created on-challenge-created
   :vote-committed on-vote-committed
   :vote-revealed on-vote-revealed
   :vote-reward-claimed on-vote-reward-claimed
   :challenge-reward-claimed on-challenge-reward-claimed
   :minted on-minted
   :auction-started on-auction-started
   :buy on-auction-buy})


(defn dispatch-registry-entry-event [type err {{:keys [:event-type] :as args} :args :as event}]
  (let [event-type (cs/->kebab-case-keyword (web3-utils/bytes32->str event-type))]
    ((get registry-entry-events event-type identity)
      (-> args
        (assoc :event-type event-type)
        (update :timestamp bn/number)
        (update :version bn/number))
      event
      type)))


(defn start [opts]
  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))
  (let [meme-auction-event-filter (meme-auction-factory/meme-auction-event {} {:from-block 0 :to-block "latest"})]
    [(-> (registry/registry-entry-event [:meme-registry :meme-registry-fwd] {} {:from-block 0 :to-block "latest"})
       (replay-past-events (partial dispatch-registry-entry-event :meme)
                           {:on-finish
                            (fn []
                              (-> meme-auction-event-filter
                                (replay-past-events (partial dispatch-registry-entry-event :meme-auction))))}))
     (-> (registry/registry-entry-event [:param-change-registry :param-change-registry-fwd] {} {:from-block 0 :to-block "latest"})
       (replay-past-events (partial dispatch-registry-entry-event :param-change)))
     (-> (meme-token/transfer-event {} {:from-block 0 :to-block "latest"})
       (replay-past-events on-meme-token-transfer))
     meme-auction-event-filter]))


(defn stop [syncer]
  (doseq [filter (remove nil? @syncer)]
    (web3-eth/stop-watching! filter (fn [err]))))

