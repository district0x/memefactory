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
    [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
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
      (db/update-registry-entry! (registry-entry/load-registry-entry registry-entry))
      (db/update-vote! (registry-entry/load-vote registry-entry voter)))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-revealed))))


(defn on-vote-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-vote-reward-claimed)
  (try
    (let [voter (web3-utils/uint->address (first data))]
      (db/update-vote! (registry-entry/load-vote registry-entry voter)))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-vote-reward-claimed))))


(defn on-challenge-reward-claimed [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-challenge-reward-claimed)
  (try
    (db/update-registry-entry! (registry-entry/load-registry-entry registry-entry))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-challenge-reward-claimed))))


(defn on-buy [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-buy)
  (try
    (let [[buyer price amount] data]
      (db/update-meme! (meme/load-meme registry-entry))
      (db/insert-meme-purchase! {:reg-entry/address registry-entry
                                 :buyer (web3-utils/uint->address buyer)
                                 :price (bn/number price)
                                 :amount (bn/number amount)
                                 :bought-on timestamp}))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-buy))))


(defn on-meme-token-transfer [{:keys [:registry-entry :timestamp :data] :as args}]
  (info info-text {:args args} ::on-meme-token-transfer)
  (try
    (let [[from to value] data
          from (web3-utils/uint->address from)
          to (web3-utils/uint->address to)]
      (when-not (= registry-entry to)                       ;; Initial mint, skip increasing balance
        (db/inc-meme-token-balance {:reg-entry/address registry-entry
                                    :owner to
                                    :amount (bn/number value)}))

      (when-not (= registry-entry from)                     ;; Initial meme offering, skip decrementing balance
        (db/dec-meme-token-balance {:reg-entry/address registry-entry
                                    :owner from
                                    :amount (bn/number value)})))
    (catch :default e
      (error error-text {:args args :error (ex-message e)} ::on-buy))))


(def registry-entry-events
  {:constructed on-constructed
   :challenge-created on-challenge-created
   :vote-committed on-vote-committed
   :vote-revealed on-vote-revealed
   :vote-reward-claimed on-vote-reward-claimed
   :challenge-reward-claimed on-challenge-reward-claimed
   :buy on-buy
   :meme-token-transfer on-meme-token-transfer})


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
  [(-> (registry/registry-entry-event [:meme-registry :meme-registry-fwd] {} {:from-block 0 :to-block "latest"})
     (replay-past-events (partial dispatch-registry-entry-event :meme)))
   (-> (registry/registry-entry-event [:param-change-registry :param-change-registry-fwd] {} {:from-block 0 :to-block "latest"})
     (replay-past-events (partial dispatch-registry-entry-event :param-change)))
   (-> (meme-auction-factory/meme-auction-event {} {:from-block 0 :to-block "latest"})
     (replay-past-events (fn [& args]
                           )))])


(defn stop [syncer]
  (doseq [filter (remove nil? @syncer)]
    (web3-eth/stop-watching! filter (fn [err]))))
