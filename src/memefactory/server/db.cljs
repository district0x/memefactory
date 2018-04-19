(ns memefactory.server.db
  (:require
    [district.server.config :refer [config]]
    [district.server.db :as db]
    [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
    [honeysql.core :as sql]
    [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as logging :refer-macros [info warn error]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} memefactory-db
  :start (start (merge (:memefactory/db @config)
                       (:memefactory/db (mount/args))))
  :stop (stop))

(def ipfs-hash (sql/call :char (sql/inline 46)))

(def registry-entry-columns
  [[:reg-entry/address address primary-key not-nil]
   [:reg-entry/version :unsigned :integer not-nil]
   [:reg-entry/creator address not-nil]
   [:reg-entry/deposit :unsigned :BIG :INT not-nil]
   [:reg-entry/created-on :unsigned :integer not-nil]
   [:reg-entry/challenge-period-end :unsigned :integer not-nil]
   [:challenge/challenger address default-nil]
   [:challenge/created-on :unsigned :integer default-nil]
   [:challenge/voting-token address default-nil]
   [:challenge/reward-pool :unsigned :BIG :INT default-nil]
   [:challenge/meta-hash ipfs-hash default-nil]
   [:challenge/commit-period-end :unsigned :integer default-nil]
   [:challenge/reveal-period-end :unsigned :integer default-nil]
   [:challenge/votes-for :BIG :INT default-nil]
   [:challenge/votes-against :BIG :INT default-nil]
   [:challenge/claimed-reward-on :unsigned :integer default-nil]])

(def meme-columns
  [[:reg-entry/address address not-nil]
   [:meme/title :varchar not-nil]
   [:meme/token address not-nil]
   [:meme/image-hash ipfs-hash not-nil]
   [:meme/meta-hash ipfs-hash not-nil]
   [:meme/total-supply :unsigned :integer not-nil]
   [:meme/total-minted :unsigned :integer not-nil]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def meme-token-balances-columns
  [[:reg-entry/address address not-nil]
   [:owner address not-nil]
   [:balance :BIG :INT not-nil]
   [(sql/call :primary-key :reg-entry/address :owner)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def tags-columns
  [[:tag/id :unsigned :integer primary-key not-nil]
   [:tag/name :varchar :unique not-nil]])

(def meme-tags-columns
  [[:reg-entry/address address not-nil]
   [:tag/id :varchar not-nil]
   [(sql/call :primary-key :reg-entry/address :tag/id)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]
   [(sql/call :foreign-key :tag/id) (sql/call :references :tags :tag/id)]])

(def meme-purchases-columns
  [[:reg-entry/address address not-nil]
   [:buyer address not-nil]
   [:amount :unsigned :integer not-nil]
   [:price :BIG :INT not-nil]
   [:bought-on :unsigned :integer default-nil]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def param-change-columns
  [[:param-change/db address not-nil]
   [:param-change/key :varchar not-nil]
   [:param-change/value :unsigned :integer not-nil]
   [:param-change/applied-on :unsigned :integer default-nil]])

(def votes-columns
  [[:reg-entry/address address not-nil]
   [:vote/voter address not-nil]
   [:vote/option :unsigned :integer not-nil]
   [:vote/amount :unsigned :BIG :INT default-nil]
   [:vote/created-on :unsigned :integer default-nil]
   [:vote/revealed-on :unsigned :integer default-nil]
   [:vote/claimed-reward-on :unsigned :integer default-nil]
   [(sql/call :primary-key :vote/voter :reg-entry/address)]
   [[(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]]])

(def registry-entry-column-names (map first registry-entry-columns))
(def meme-column-names (map first meme-columns))
(def meme-token-balances-column-names (map first meme-token-balances-columns))
(def meme-tags-column-names (map first meme-tags-columns))
(def meme-purchases-column-names (map first meme-purchases-columns))
(def param-change-column-names (map first param-change-columns))
(def votes-column-names (map first votes-columns))
(def tags-column-names (map first tags-columns))

(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))


(defn start [opts]
  (db/run! {:create-table [:reg-entries]
            :with-columns [registry-entry-columns]})

  (db/run! {:create-table [:memes]
            :with-columns [meme-columns]})

  (db/run! {:create-table [:meme-token-balances]
            :with-columns [meme-token-balances-columns]})

  (db/run! {:create-table [:tags]
            :with-columns [tags-columns]})

  (db/run! {:create-table [:meme-tags]
            :with-columns [meme-tags-columns]})

  (db/run! {:create-table [:meme-purchases]
            :with-columns [meme-purchases-columns]})

  (db/run! {:create-table [:param-changes]
            :with-columns [param-change-columns]})

  (db/run! {:create-table [:votes]
            :with-columns [votes-columns]})


  ;; TODO create indexes
  #_(doseq [column (rest registry-entry-column-names)]
      (db/run! {:create-index (index-name column) :on [:offerings column]})))


(defn stop []
  (db/run! {:drop-table [:votes]})
  (db/run! {:drop-table [:meme-purchases]})
  (db/run! {:drop-table [:meme-tags]})
  (db/run! {:drop-table [:tags]})
  (db/run! {:drop-table [:meme-token-balances]})
  (db/run! {:drop-table [:memes]})
  (db/run! {:drop-table [:param-changes]})
  (db/run! {:drop-table [:reg-entries]}))

(defn create-insert-fn [table-name column-names]
  (fn [item]
    (let [item (select-keys item column-names)]
      (db/run! {:insert-into table-name
                :columns (keys item)
                :values [(vals item)]}))))

(defn create-update-fn [table-name column-names id-keys]
  (fn [item]
    (let [item (select-keys item column-names)
          id-keys (if (sequential? id-keys) id-keys [id-keys])]
      (db/run! {:update table-name
                :set item
                :where (concat
                         [:and]
                         (for [id-key id-keys]
                           [:= id-key (get item id-key)]))}))))

(def insert-registry-entry! (create-insert-fn :reg-entries registry-entry-column-names))
(def update-registry-entry! (create-update-fn :reg-entries registry-entry-column-names :reg-entry/address))

(def insert-meme! (create-insert-fn :memes meme-column-names))
(def update-meme! (create-update-fn :memes meme-column-names :reg-entry/address))

(def insert-meme-purchase! (create-insert-fn :meme-purchases meme-purchases-column-names))

(def insert-param-change! (create-insert-fn :param-changes param-change-column-names))
(def update-param-change! (create-update-fn :param-changes param-change-column-names :reg-entry/address))

(def insert-vote! (create-insert-fn :votes votes-column-names))
(def update-vote! (create-update-fn :votes votes-column-names [:reg-entry/address :vote/voter]))

(defn meme-token-balance [{:keys [:reg-entry/address :owner]}]
  (or (:balance (db/get {:select [:balance]
                         :from [:meme-token-balances]
                         :where [:and
                                 [:= :reg-entry/address address]
                                 [:= :owner owner]]}))
      0))

(defn update-meme-token-balance [update-fn {:keys [:amount] :as item}]
  (let [balance (meme-token-balance item)
        item (-> item
               (assoc :balance (update-fn balance amount))
               (select-keys meme-token-balances-column-names))]
    (db/run! {:insert-or-replace-into :meme-token-balances
              :columns (keys item)
              :values [(vals item)]})))

(def inc-meme-token-balance (partial update-meme-token-balance +))
(def dec-meme-token-balance (partial update-meme-token-balance -))