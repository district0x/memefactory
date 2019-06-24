(ns memefactory.server.db
  (:require [district.server.config :refer [config]]
            [district.server.db :as db]
            [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
            [district.server.db.honeysql-extensions]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as psqlh]
            [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
            [medley.core :as medley]
            [mount.core :as mount :refer [defstate]]
            [print.foo :refer [look] :include-macros true]
            [taoensso.timbre :as log]))

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
   [:challenge/reward-amount :unsigned :BIG :INT default-nil]
   [:challenge/meta-hash ipfs-hash default-nil]
   [:challenge/comment :varchar default-nil]
   [:challenge/commit-period-end :unsigned :integer default-nil]
   [:challenge/reveal-period-end :unsigned :integer default-nil]
   [:challenge/votes-for :BIG :INT default-zero]
   [:challenge/votes-against :BIG :INT default-zero]
   [:challenge/votes-total :BIG :INT default-zero]
   [:challenge/claimed-reward-on :unsigned :integer default-nil]])

(def memes-columns
  [[:reg-entry/address address not-nil]
   [:meme/title :varchar not-nil]
   [:meme/comment :varchar default-nil]
   [:meme/number :integer default-nil]
   [:meme/image-hash ipfs-hash not-nil]
   [:meme/meta-hash ipfs-hash not-nil]
   [:meme/total-supply :unsigned :integer not-nil]
   [:meme/total-minted :unsigned :integer not-nil]
   [:meme/token-id-start :unsigned :integer default-nil]
   [:meme/total-trade-volume :BIG :INT default-zero]
   [:meme/first-mint-on :unsigned :integer default-nil]
   [(sql/call :primary-key :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address) (sql/raw "ON DELETE CASCADE")]])

(def meme-tokens-columns
  [[:meme-token/token-id :unsigned :integer not-nil]
   [:meme-token/number :unsigned :integer not-nil]
   [:reg-entry/address address not-nil]
   [(sql/call :primary-key :meme-token/token-id)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address) (sql/raw "ON DELETE CASCADE")]])

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
   [(sql/call :foreign-key :tag/name) (sql/call :references :tags :tag/name) (sql/raw "ON DELETE CASCADE")]])

(def meme-auctions-columns
  [[:meme-auction/address address not-nil]
   [:meme-auction/token-id :unsigned :integer not-nil]
   [:meme-auction/seller address not-nil]
   [:meme-auction/buyer address default-nil]
   [:meme-auction/start-price :BIG :INT not-nil]
   [:meme-auction/end-price :BIG :INT not-nil]
   [:meme-auction/started-on :unsigned :integer default-nil]
   [:meme-auction/duration :unsigned :integer default-nil]
   [:meme-auction/bought-on :unsigned :integer default-nil]
   [:meme-auction/canceled-on :unsigned :integer default-nil]
   [:meme-auction/bought-for :BIG :INT default-nil]
   [:meme-auction/description :varchar default-nil]
   [(sql/call :primary-key :meme-auction/address)]])

(def initial-params-columns
  [[:initial-param/key :varchar not-nil]
   [:initial-param/db address not-nil]
   [:initial-param/value :unsigned :integer not-nil]
   [:initial-param/set-on :unsigned :integer default-nil]
   [(sql/call :primary-key :initial-param/key :initial-param/db)]])

(def param-changes-columns
  [[:reg-entry/address address not-nil]
   [:param-change/db address not-nil]
   [:param-change/key :varchar not-nil]
   [:param-change/value :unsigned :integer not-nil]
   [:param-change/initial-value :unsigned :integer not-nil]
   [:param-change/applied-on :unsigned :integer default-nil]
   [(sql/call :primary-key :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address) (sql/raw "ON DELETE CASCADE")]])

(def votes-columns
  [[:reg-entry/address address not-nil]
   [:vote/voter address not-nil]
   [:vote/option :unsigned :integer not-nil]
   [:vote/amount :unsigned :BIG :INT default-nil]
   [:vote/created-on :unsigned :integer default-nil]
   [:vote/revealed-on :unsigned :integer default-nil]
   [:vote/claimed-reward-on :unsigned :integer default-nil]
   [:vote/reclaimed-amount-on :unsigned :integer default-nil]
   [(sql/call :primary-key :vote/voter :reg-entry/address)]
   [(sql/call :foreign-key :reg-entry/address) (sql/call :references :reg-entries :reg-entry/address) (sql/raw "ON DELETE CASCADE")]])

(def user-columns
  [[:user/address address not-nil]
   [:user/voter-total-earned :BIG :INT default-zero]
   [:user/challenger-total-earned :BIG :INT default-zero]
   [:user/total-earned :BIG :INT default-zero]
   [:user/best-single-card-sale :BIG :INT default-zero]
   [(sql/call :primary-key :user/address)]])

(def twitter-media-columns
  [[:reg-entry/address address not-nil]
   [:twitter/media-id :varchar not-nil]
   [(sql/call :primary-key :reg-entry/address)]])

(def events-columns
  [[:event/contract-key :varchar not-nil]
   [:event/event-name :varchar not-nil]
   [:event/last-log-index :integer not-nil]
   [:event/last-block-number :integer not-nil]
   [:event/count :integer not-nil]
   [(sql/call :primary-key :event/contract-key :event/event-name)]])

(def registry-entry-column-names (map first registry-entries-columns))
(def meme-column-names (filter keyword? (map first memes-columns)))
(def meme-tokens-column-names (filter keyword? (map first meme-tokens-columns)))

(def meme-token-owners-column-names (map first meme-token-owners-columns))
(def meme-tags-column-names (map first meme-tags-columns))
(def meme-auctions-column-names (filter keyword? (map first meme-auctions-columns)))
(def param-change-column-names (filter keyword? (map first param-changes-columns)))
(def initial-params-column-names (filter keyword? (map first initial-params-columns)))
(def votes-column-names (map first votes-columns))
(def tags-column-names (map first tags-columns))
(def user-column-names (filter keyword? (map first user-columns)))
(def twitter-media-column-names (filter keyword? (map first twitter-media-columns)))
(def events-column-names (filter keyword? (map first events-columns)))

(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))

(defn clean-db []
  (let [tables [:reg-entries
                :votes
                :meme-auctions
                :meme-tags
                :tags
                :meme-tokens
                :meme-token-owners
                :memes
                :param-changes
                :users
                :initial-params
                :twitter-media
                :events]
        drop-table-if-exists (fn [t]
                               (psqlh/drop-table :if-exists t))]
    (doall
     (map (fn [t]
            (log/debug (str "Dropping table " t))
            (db/run! (drop-table-if-exists t)))
          tables))))

(defn start [{:keys [:resync?] :as opts}]
  (when resync?
    (log/info "Database module called with a resync flag. Cleaning db")
    (clean-db))

  (db/run! (-> (psqlh/create-table :reg-entries :if-not-exists)
               (psqlh/with-columns registry-entries-columns)))

  (db/run! (-> (psqlh/create-table :memes :if-not-exists)
               (psqlh/with-columns memes-columns)))

  (db/run! (-> (psqlh/create-table :meme-tokens :if-not-exists)
               (psqlh/with-columns meme-tokens-columns)))

  (db/run! (-> (psqlh/create-table :meme-token-owners :if-not-exists)
               (psqlh/with-columns meme-token-owners-columns)))

  (db/run! (-> (psqlh/create-table :tags :if-not-exists)
               (psqlh/with-columns tags-columns)))

  (db/run! (-> (psqlh/create-table :meme-tags :if-not-exists)
               (psqlh/with-columns meme-tags-columns)))

  (db/run! (-> (psqlh/create-table :meme-auctions :if-not-exists)
               (psqlh/with-columns meme-auctions-columns)))

  (db/run! (-> (psqlh/create-table :param-changes :if-not-exists)
               (psqlh/with-columns param-changes-columns)))

  (db/run! (-> (psqlh/create-table :votes :if-not-exists)
               (psqlh/with-columns votes-columns)))

  (db/run! (-> (psqlh/create-table :users :if-not-exists)
               (psqlh/with-columns user-columns)))

  (db/run! (-> (psqlh/create-table :initial-params :if-not-exists)
               (psqlh/with-columns initial-params-columns)))

  (db/run! (-> (psqlh/create-table :events :if-not-exists)
               (psqlh/with-columns events-columns)))

  ;; TODO create indexes
  #_(doseq [column (rest registry-entry-column-names)]
      (db/run! {:create-index (index-name column) :on [:offerings column]}))

  ::started)

(defn stop []
  ::stopped)

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

;; REG-ENTRIES

(def insert-registry-entry! (create-insert-fn :reg-entries registry-entry-column-names))
(def update-registry-entry! (create-update-fn :reg-entries registry-entry-column-names :reg-entry/address))
(def get-registry-entry (create-get-fn :reg-entries :reg-entry/address))

(defn all-reg-entries []
  (db/all {:select [:re.*]
           :from [[:reg-entries :re]]}))

;; MEMES

(def insert-meme! (create-insert-fn :memes (filter #(not= % :meme/number) meme-column-names)))
(def update-meme! (create-update-fn :memes meme-column-names :reg-entry/address))

(defn get-meme [address]
  (db/get {:select [:*]
           :from [[:reg-entries :re]]
           :join [[:memes :m] [:= :re.reg-entry/address :m.reg-entry/address]]
           :where [:= :m.reg-entry/address address]}))

(defn inc-meme-field! [address field & [amount]]
  (db/run! {:update :memes
            :set {field (sql/call :+ field (or amount 1))}
            :where [:= :reg-entry/address address]}))

(defn get-meme-by-token-id [token-id]
  (db/get {:select [:m.* :mt.*]
           :from [[:meme-tokens :mt]]
           :join [[:memes :m]
                  [:= :mt.reg-entry/address :m.reg-entry/address] ]
           :where [:= :mt.meme-token/token-id token-id]}))

(defn current-meme-number []
  (-> (db/get {:select [[(sql/call :max :memes.meme/number) :max-number]]
               :from [:memes]})
      :max-number
      (or 0)))

(defn patch-forbidden-reg-entry-image! [address]
  (db/run! {:update :memes
            :set {:meme/image-hash "forbidden-image-hash"}
            :where [:= :reg-entry/address address]}))

(defn assign-meme-number! [address n]
  (db/run! {:update :memes
            :set {:meme/number n}
            :where [:= :reg-entry/address address]}))

;; MEME-TOKENS

(defn insert-meme-tokens! [{:keys [:token-id-start :token-id-end :reg-entry/address]}]
  (let [max-token-number (-> (db/get {:select [[(sql/call :max :meme-tokens.meme-token/number) :max-number]]
                                      :from [:meme-tokens]
                                      :where [:= :meme-tokens.reg-entry/address address]})
                             :max-number
                             (or 0))
        token-id-range (range token-id-start (inc token-id-end))
        token-number-range (range (inc max-token-number) (+ max-token-number (count token-id-range) 1))]
    (db/run! {:insert-into :meme-tokens
              :columns meme-tokens-column-names
              :values (map (fn [tnum tid]
                             [tid tnum address])
                           token-number-range
                           token-id-range)})))

;; MEME-TOKEN-OWNERS

(defn upsert-meme-token-owner! [args]
  (db/run! {:insert-into :meme-token-owners
            :values [(select-keys args meme-token-owners-column-names)]
            :upsert {:on-conflict [:meme-token/token_id]
                     :do-update-set (keys args)}}))

;; TAGS / MEME-TAGS

(defn tag-meme! [{:keys [:reg-entry/address :tag/name] :as args}]
  (db/run! {:insert-into :tags
            :values [(select-keys args tags-column-names)]
            :upsert {:on-conflict [:tag/name]
                     :do-nothing []}})
  (db/run! {:insert-into :meme-tags
            :values [(select-keys args meme-tags-column-names)]}))

;; MEME-AUCTIONS

(def insert-meme-auction! (create-insert-fn :meme-auctions meme-auctions-column-names))
(def update-meme-auction! (create-update-fn :meme-auctions meme-auctions-column-names :meme-auction/address))

(defn get-meme-auction [auction-address]
  (db/get {:select [:*]
           :from [[:meme-auctions :ma]]
           :where [:= :ma.meme-auction/address auction-address]}))

(defn get-meme-by-auction-address [auction-address]
  (db/get {:select [:m.*]
           :from [[:meme-auctions :ma]]
           :join [[:meme-tokens :mt] [:= :mt.meme-token/token-id :ma.meme-auction/token-id]
                  [:memes :m] [:= :mt.reg-entry/address :m.reg-entry/address]]
           :where [:= :ma.meme-auction/address auction-address]}))

;; INITIAL-PARAMS

(defn insert-initial-params! [rows]
  (db/run! {:insert-into :initial-params
            :values rows}))

(defn get-initial-param [key db]
  (db/get {:select [:*]
           :from [:initial-params]
           :where [:and [:= :initial-param/key key]
                   [:= :initial-param/db db]]}))

;; PARAM-CHANGES

(def insert-param-change! (create-insert-fn :param-changes param-change-column-names))
(def update-param-change! (create-update-fn :param-changes param-change-column-names :reg-entry/address))

(def insert-or-replace-param-change! (create-insert-fn :param-changes
                                                       param-change-column-names
                                                       {:insert-or-replace? true}))

;; VOTES

(def insert-vote! (create-insert-fn :votes votes-column-names))
(def update-vote! (create-update-fn :votes votes-column-names [:reg-entry/address :vote/voter]))
(def get-vote (create-get-fn :votes [:reg-entry/address :vote/voter]))

;; USERS

(def get-user (create-get-fn :users :user/address))

(defn upsert-user! [args]
  (db/run! {:insert-into :users
            :values [(select-keys args user-column-names)]
            :upsert {:on-conflict [:user/address]
                     :do-update-set (keys args)}}))

(defn inc-user-field! [user-address user-field & [amount]]
  (db/run! {:update :users
            :set {user-field (sql/call :+ user-field (or amount 1))}
            :where [:= :user/address user-address]}))

;; TWITTER-MEDIA

(defn save-meme-media-id! [registry-entry media-id]
  (db/run! {:insert-into :twitter-media
            :columns [:reg-entry/address :twitter/media-id]
            :values [[registry-entry media-id]]}))

(defn get-meme-media-id [registry-entry]
  (-> (db/get {:select [:twitter/media-id]
               :from [[:twitter-media :tm]]
               :where [:= :tm.reg-entry/address registry-entry]})
      :twitter/media-id))

;; EVENTS

(def get-last-event (create-get-fn :events [:event/contract-key :event/event-name]))

(defn upsert-event! [{:keys [:event/event-name :event/contract-key
                             :event/last-log-index :event/last-block-number] :as args}]
  (db/run! {:insert-into :events
            :values [(select-keys args events-column-names)]
            :upsert {:on-conflict [:event/event-name :event/contract-key]
                     :do-update-set [:event/last-log-index :event/last-block-number :event/count]}}))
