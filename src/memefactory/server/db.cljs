(ns memefactory.server.db
  (:require [district.server.config :refer [config]]
            [district.server.db :as db]
            [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
            [district.server.db.honeysql-extensions]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
            [medley.core :as medley]
            [mount.core :as mount :refer [defstate]]
            [print.foo :refer [look] :include-macros true]
            [taoensso.timbre :as logging :refer-macros [info warn error]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} memefactory-db
  :start (start (merge (:memefactory/db @config)
                       (:memefactory/db (mount/args))))
  :stop (stop))

(def ipfs-hash (sql/call :char (sql/inline 46)))

(def registry-entries-columns
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
   [:challenge/comment :varchar default-nil]
   [:challenge/commit-period-end :unsigned :integer default-nil]
   [:challenge/reveal-period-end :unsigned :integer default-nil]
   [:challenge/votes-for :BIG :INT default-nil]
   [:challenge/votes-against :BIG :INT default-nil]
   [:challenge/claimed-reward-on :unsigned :integer default-nil]])

(def memes-columns
  [[:reg-entry/address address not-nil]
   [:meme/title :varchar not-nil]
   [:meme/number :integer not-nil]
   [:meme/image-hash ipfs-hash not-nil]
   [:meme/meta-hash ipfs-hash not-nil]
   [:meme/total-supply :unsigned :integer not-nil]
   [:meme/total-minted :unsigned :integer not-nil]
   [:meme/token-id-start :unsigned :integer not-nil]
   [:meme/total-trade-volume :BIG :INT default-nil]
   [:meme/first-mint-on :unsigned :integer default-nil]
   [(sql/call :primary-key :meme/number)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def meme-tokens-columns
  [[:meme-token/token-id :unsigned :integer not-nil]
   [:meme-token/number :unsigned :integer not-nil]
   [:reg-entry/address address not-nil]
   [(sql/call :primary-key :meme-token/token-id)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]])

(def meme-token-owners-columns
  [[:meme-token/token-id :unsigned :integer not-nil]
   [:meme-token/owner address not-nil]
   [:meme-token/transferred-on :unsigned :integer default-nil]
   [(sql/call :primary-key :meme-token/token-id)]])

(def tags-columns
  [[:tag/name :varchar primary-key not-nil]])

(def meme-tags-columns
  [[:reg-entry/address address not-nil]
   [:tag/name :varchar not-nil]
   [(sql/call :primary-key :reg-entry/address :tag/name)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]
   [(sql/call :foreign-key :tag/name) (sql/call :references :tags :tag/name)]])

(def meme-auctions-columns
  [[:meme-auction/address address not-nil]
   [:meme-auction/token-id :unsigned :integer #_not-nil]
   [:meme-auction/seller address #_not-nil]
   [:meme-auction/buyer address default-nil]
   [:meme-auction/start-price :BIG :INT #_not-nil]
   [:meme-auction/end-price :BIG :INT #_not-nil]
   [:meme-auction/started-on :unsigned :integer default-nil]
   [:meme-auction/duration :unsigned :integer default-nil]
   [:meme-auction/bought-on :unsigned :integer default-nil]
   [:meme-auction/canceled-on :unsigned :integer default-nil]
   [:meme-auction/bought-for :BIG :INT default-nil]
   [:meme-auction/description :varchar default-nil]
   [(sql/call :primary-key :meme-auction/address)]])

(def param-changes-columns
  [[:reg-entry/address address not-nil]
   [:param-change/db address not-nil]
   [:param-change/key :varchar not-nil]
   [:param-change/value :unsigned :integer not-nil]
   [:param-change/applied-on :unsigned :integer default-nil]
   [(sql/call :primary-key :reg-entry/address)]
   #_[[(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address)]]])

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

(def user-columns
  [[:user/address address not-nil]
   ;[:user/creator-largest-sale address default-nil]
   ;[:user/largest-sale address default-nil]
   ;[:user/largest-buy address default-nil]
   [:user/voter-total-earned :BIG :INT default-zero]
   [:user/challenger-total-earned :BIG :INT default-zero]
   [(sql/call :primary-key :user/address)]])

(def registry-entry-column-names (map first registry-entries-columns))
(def meme-column-names (filter keyword? (map first memes-columns)))
(def meme-tokens-column-names (filter keyword? (map first meme-tokens-columns)))

(def meme-token-owners-column-names (map first meme-token-owners-columns))
(def meme-tags-column-names (map first meme-tags-columns))
(def meme-auctions-column-names (filter keyword? (map first meme-auctions-columns)))
(def param-change-column-names (filter keyword? (map first param-changes-columns)))
(def votes-column-names (map first votes-columns))
(def tags-column-names (map first tags-columns))
(def user-column-names (filter keyword? (map first user-columns)))

(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))

(defn start [opts]
  (db/run! {:create-table [:reg-entries]
            :with-columns [registry-entries-columns]})

  (db/run! {:create-table [:memes]
            :with-columns [memes-columns]})

  (db/run! {:create-table [:meme-tokens]
            :with-columns [meme-tokens-columns]})

  (db/run! {:create-table [:meme-token-owners]
            :with-columns [meme-token-owners-columns]})

  (db/run! {:create-table [:tags]
            :with-columns [tags-columns]})

  (db/run! {:create-table [:meme-tags]
            :with-columns [meme-tags-columns]})

  (db/run! {:create-table [:meme-auctions]
            :with-columns [meme-auctions-columns]})

  (db/run! {:create-table [:param-changes]
            :with-columns [param-changes-columns]})

  (db/run! {:create-table [:votes]
            :with-columns [votes-columns]})

  (db/run! {:create-table [:users]
            :with-columns [user-columns]})

  ;; TODO create indexes
  #_(doseq [column (rest registry-entry-column-names)]
      (db/run! {:create-index (index-name column) :on [:offerings column]})))


(defn stop []
  (db/run! {:drop-table [:votes]})
  (db/run! {:drop-table [:meme-auctions]})
  (db/run! {:drop-table [:meme-tags]})
  (db/run! {:drop-table [:tags]})
  (db/run! {:drop-table [:meme-tokens]})
  (db/run! {:drop-table [:meme-token-owners]})
  (db/run! {:drop-table [:memes]})
  (db/run! {:drop-table [:param-changes]})
  (db/run! {:drop-table [:reg-entries]})
  (db/run! {:drop-table [:users]}))

(defn create-insert-fn [table-name column-names & [{:keys [:insert-or-replace?]}]]
  (fn [item]
    (let [item (select-keys item column-names)]
      (db/run! {(if insert-or-replace? :insert-or-replace-into :insert-into) table-name
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

(defn create-get-fn [table-name id-keys]
  (let [id-keys (if (sequential? id-keys) id-keys [id-keys])]
    (fn [item fields]
      (cond-> (db/get {:select (if (sequential? fields) fields [fields])
                       :from [table-name]
                       :where (concat
                                [:and]
                                (for [id-key id-keys]
                                  [:= id-key (get item id-key)]))})
        (keyword? fields) fields))))

(def insert-registry-entry! (create-insert-fn :reg-entries registry-entry-column-names))
(def update-registry-entry! (create-update-fn :reg-entries registry-entry-column-names :reg-entry/address))
(def get-registry-entry (create-get-fn :reg-entries :reg-entry/address))

(def insert-meme! (create-insert-fn :memes (filter #(not= % :meme/number) meme-column-names)))

(def update-meme! (create-update-fn :memes meme-column-names :reg-entry/address))

(def insert-meme-auction! (create-insert-fn :meme-auctions meme-auctions-column-names))
(def update-meme-auction! (create-update-fn :meme-auctions meme-auctions-column-names :meme-auction/address))

(def insert-param-change! (create-insert-fn :param-changes param-change-column-names))
(def update-param-change! (create-update-fn :param-changes param-change-column-names :reg-entry/address))

(def insert-vote! (create-insert-fn :votes votes-column-names))
(def update-vote! (create-update-fn :votes votes-column-names [:reg-entry/address :vote/voter]))

(def get-user (create-get-fn :users :user/address))

(defn insert-meme-tokens! [{:keys [:token-id-start :token-id-end :reg-entry/address]}]
  (db/run! {:insert-into :meme-tokens
            :columns meme-tokens-column-names
            :values (for [[i token-id] (medley/indexed (range token-id-start (inc token-id-end)))]
                      [token-id (inc i) address])}))

(def insert-or-replace-meme-token-owner (create-insert-fn :meme-token-owners
                                                          meme-token-owners-column-names
                                                          {:insert-or-replace? true}))

(def insert-or-replace-param-change! (create-insert-fn :param-changes
                                                       param-change-column-names
                                                       {:insert-or-replace? true}))

(defn meme-auction-exists? [address]
  (boolean (seq (db/get {:select [1]
                         :from [:meme-auctions]
                         :where [:= :meme-auction/address address]}))))

(defn insert-or-update-meme-auction! [{:keys [:meme-auction/address] :as args}]
  (if (meme-auction-exists? address)
    (update-meme-auction! args)
    (insert-meme-auction! args)))

(defn meme-total-trade-volume [reg-entry-address]
  (-> (db/get {:select [:meme/total-trade-volume]
               :from [:memes]
               :where [:= :reg-entry/address reg-entry-address]})
    :meme/total-trade-volume))


(defn update-meme-first-mint-on! [{:keys [:reg-entry/address :meme/first-mint-on]}]
  (when-not (-> (db/get {:select [:meme/first-mint-on]
                         :from [:memes]
                         :where [:= :reg-entry/address address]})
              :meme/first-mint-on)
    (update-meme! {:reg-entry/address address
                   :meme/first-mint-on first-mint-on})))


(defn inc-meme-total-trade-volume! [{:keys [:reg-entry/address :amount]}]
  (db/run! {:update :memes
            :set {:meme/total-trade-volume (+ (meme-total-trade-volume address) amount)}
            :where [:= :reg-entry/address address]}))


(defn user-exists? [user-address]
  (boolean (seq (db/get {:select [1]
                         :from [:users]
                         :where [:= :user/address user-address]}))))

(defn tag-exists? [name]
  (boolean (seq (db/get {:select [1]
                         :from [:tags]
                         :where [:= :tag/name name]}))))

(defn tag-meme! [reg-entry-address tag-name]
  (when-not (tag-exists? tag-name)
    (db/run! {:insert-into :tags
              :columns [:tag/name]
              :values [[tag-name]]}))

  (db/run! {:insert-into :meme-tags
            :columns [:reg-entry/address :tag/name]
            :values [[reg-entry-address tag-name]]}))

(defn update-user! [{:keys [:user/address] :as params}]
  (when-not (user-exists? address)
    (db/run! {:insert-into :users
              :columns [:user/address]
              :values [[address]]}))
  (db/run! {:update :users
            :set params
            :where [:= :user/address address]}))


(defn inc-user-field! [user-address user-field & [amount]]
  (update-user! {:user/address user-address
                 user-field (+ (get-user {:user/address user-address} user-field)
                               (or amount 1))}))
