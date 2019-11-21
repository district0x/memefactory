(ns memefactory.server.graphql-resolvers
  (:require [ajax.core :refer [GET POST]]
            [bignumber.core :as bn]
            [cljs-time.core :as t]
            [cljs-web3.core :as web3-core]
            [cljs-web3.eth :as web3-eth]
            [cljs.core.match :refer-macros [match]]
            [cljs.nodejs :as nodejs]
            [clojure.set :as clj-set]
            [clojure.string :as string]
            [district.graphql-utils :as graphql-utils]
            [district.server.config :refer [config]]
            [district.server.db :as db]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :as web3]
            [district.shared.async-helpers :refer [safe-go <? promise->]]
            [district.shared.error-handling :refer [try-catch-throw try-catch]]
            [honeysql.core :as sql]
            [honeysql.helpers :as sqlh]
            [memefactory.server.db :as mf-db]
            [memefactory.server.ranks-cache :as ranks-cache]
            [memefactory.server.utils :as utils]
            [memefactory.shared.contract.registry-entry :as registry-entry]
            [memefactory.shared.utils :as shared-utils]
            [taoensso.timbre :as log]))

(def enum graphql-utils/kw->gql-name)

(def graphql-fields (nodejs/require "graphql-fields"))
(def child-process (js/require "child_process"))
(def util (js/require "util"))
(def request-promise (nodejs/require "request-promise"))
(def exec-promise (.promisify util (aget child-process "exec")))

(def whitelisted-config-keys [:ipfs :ui])

(def trending-votes-period (* 24 60 60)) ;; last 24 H

(defn- query-fields
  "Returns the first order fields"
  [document & [path]]
  (->> (-> document
           graphql-fields
           (js->clj))
       (#(if path
           (get-in % [(-> path enum name)])
           %))
       keys
       (map graphql-utils/gql-name->kw)
       set))

(defn- graphqlize
  "Given a map it will transform all the keys into graphql friendly
  names."
  [coll]
  (reduce-kv
   (fn [m k v]
     (assoc m k (graphql-utils/kw->gql-name v))) (empty coll) coll))

(defn paged-query
  "Execute a paged query.
  query: a map honeysql query.
  page-size: a int
  page-start-idx: a int
  Returns a map with [:items :total-count :end-cursor :has-next-page]"
  [query page-size page-start-idx]
  (let [paged-query (cond-> query
                      page-size (assoc :limit page-size)
                      page-start-idx (assoc :offset page-start-idx))
        total-count (count (db/all query))
        result (db/all paged-query)
        last-idx (cond-> (count result)
                   page-start-idx (+ page-start-idx))]
    (log/debug "Paged query result" result)
    {:items result
     :total-count total-count
     :end-cursor (str last-idx)
     :has-next-page (< last-idx total-count)}))

(defn meme-query-resolver [_ {:keys [:reg-entry/address] :as args}]
  (db/get {:select [:*]
           :from [[:memes :m] [:reg-entries :re]]
           :where [:and
                   [:= :m.reg-entry/address :re.reg-entry/address]
                   [:= :re.reg-entry/address address]]}))

(defn reg-entry-status [now {:keys [:reg-entry/created-on :reg-entry/challenge-period-end :challenge/challenger
                                    :challenge/commit-period-end :challenge/commit-period-end
                                    :challenge/reveal-period-end :challenge/votes-for :challenge/votes-against] :as reg-entry}]
  (cond
    (and (< now challenge-period-end) (not challenger)) :reg-entry.status/challenge-period
    (< now commit-period-end)                           :reg-entry.status/commit-period
    (< now reveal-period-end)                           :reg-entry.status/reveal-period
    (and (pos? reveal-period-end)
         (> now reveal-period-end)) (if (< votes-against votes-for)
                                      :reg-entry.status/whitelisted
                                      :reg-entry.status/blacklisted)
    :else :reg-entry.status/whitelisted))


(defn reg-entry-status-sql-clause [now]
  (sql/call ;; TODO: can we remove aliases here?
   :case
   [:and
    [:< now :re.reg-entry/challenge-period-end]
    [:= :re.challenge/challenger nil]]                        (enum :reg-entry.status/challenge-period)
   [:< now :re.challenge/commit-period-end]                   (enum :reg-entry.status/commit-period)
   [:< now :re.challenge/reveal-period-end]                   (enum :reg-entry.status/reveal-period)
   [:and
    [:> :re.challenge/reveal-period-end 0]
    [:> now :re.challenge/reveal-period-end]]  (sql/call :case
                                                         [:< :re.challenge/votes-against :re.challenge/votes-for]
                                                         (enum :reg-entry.status/whitelisted)

                                                         :else (enum :reg-entry.status/blacklisted))
   :else (enum :reg-entry.status/whitelisted)))

(defn search-memes-query-resolver [_ {:keys [:title :tags :tags-or :tags-not :statuses :challenged :order-by :order-dir :owner :creator :curator :challenger :voter :first :after] :as args}]
  (log/debug "search-memes-query-resolver" args)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [statuses-set (when statuses (set statuses))
                     page-start-idx (when after (js/parseInt after))
                     page-size first
                     query (cond-> {:select [:re.* :memes.* :votes.votes-total :meme/average-price :meme/highest-single-sale]
                                    :from [:memes]
                                    :modifiers [:distinct]
                                    :join [[:reg-entries :re] [:= :re.reg-entry/address :memes.reg-entry/address]]
                                    :left-join (cond-> [[{:select [:meme-tokens.reg-entry/address :meme-tokens.meme-token/token-id :meme-token-owners.meme-token/owner]
                                                          :from [:meme-tokens]
                                                          :join [:meme-token-owners
                                                                 [:= :meme-token-owners.meme-token/token-id :meme-tokens.meme-token/token-id]]} :tokens]
                                                        [:= :memes.reg-entry/address :tokens.reg-entry/address]

                                                        :meme-tags
                                                        [:= :meme-tags.reg-entry/address :memes.reg-entry/address]

                                                        [{:select [:votes.reg-entry/address [(sql/call :total :votes.vote/amount) :votes-total]]
                                                          :from [:votes]
                                                          :where [:> :votes.vote/created-on (- now trending-votes-period)]
                                                          :group-by [:votes.reg-entry/address]} :votes]
                                                        [:= :memes.reg-entry/address :votes.reg-entry/address]

                                                        [:votes :v]
                                                        [:= :v.reg-entry/address :re.reg-entry/address]

                                                        [{:select [:meme-tokens.reg-entry/address
                                                                   [(sql/call :max :meme-auctions.meme-auction/bought-for) :meme/highest-single-sale]
                                                                   [(sql/call :avg :meme-auctions.meme-auction/bought-for) :meme/average-price]]
                                                          :from [:meme-tokens]
                                                          :join [:meme-auctions
                                                                 [:= :meme-auctions.meme-auction/token-id :meme-tokens.meme-token/token-id]]
                                                          :group-by [:meme-tokens.reg-entry/address]} :auctions]
                                                        [:= :memes.reg-entry/address :auctions.reg-entry/address]])}
                             title        (sqlh/merge-where [:like :memes.meme/title (str "%" title "%")])
                             challenged   (sqlh/merge-where [:not= :re.challenge/challenger nil])
                             tags         (sqlh/merge-where [:=
                                                             (count tags)
                                                             {:select [(sql/call :count :*)]
                                                              :from [:meme-tags]
                                                              :where [:and
                                                                      [:= :meme-tags.reg-entry/address :memes.reg-entry/address]
                                                                      [:in (sql/call :lower :meme-tags.tag/name) tags]]}])
                             tags-or      (sqlh/merge-where [:in (sql/call :lower :meme-tags.tag/name) tags-or])
                             tags-not     (sqlh/merge-where [:not-in :re.reg-entry/address {:select [:meme-tags.reg-entry/address]
                                                                                            :modifiers [:distinct]
                                                                                            :from [:meme-tags]
                                                                                            :where [:in (sql/call :lower :meme-tags.tag/name) tags-not]}])
                             creator      (sqlh/merge-where [:= :re.reg-entry/creator creator])
                             curator      (sqlh/merge-where [:or [:= :re.challenge/challenger curator]
                                                             [:= :v.vote/voter curator]])
                             challenger   (sqlh/merge-where [:= :re.challenge/challenger challenger])
                             voter        (sqlh/merge-where [:= :v.vote/voter voter])
                             owner        (sqlh/merge-where [:= :tokens.meme-token/owner owner])
                             statuses-set (sqlh/merge-where [:in (reg-entry-status-sql-clause now) statuses-set])
                             order-by     (sqlh/merge-order-by [[(get {:memes.order-by/reveal-period-end    :re.challenge/reveal-period-end
                                                                       :memes.order-by/commit-period-end  :re.challenge/commit-period-end
                                                                       :memes.order-by/challenge-period-end :re.reg-entry/challenge-period-end
                                                                       :memes.order-by/total-trade-volume   :memes.meme/total-trade-volume
                                                                       :memes.order-by/created-on           :re.reg-entry/created-on
                                                                       :memes.order-by/number               :memes.meme/number
                                                                       :memes.order-by/total-minted         :memes.meme/total-minted
                                                                       :memes.order-by/daily-total-votes    :votes.votes-total
                                                                       :memes.order-by/average-price        :meme/average-price
                                                                       :memes.order-by/highest-single-sale  :meme/highest-single-sale}
                                                                      ;; TODO: move this transformation to district-server-graphql
                                                                      (graphql-utils/gql-name->kw order-by))
                                                                 (or (keyword order-dir) :asc)]]))]
                 (paged-query query page-size page-start-idx)))))

(defn search-meme-tokens-query-resolver [_ {:keys [:statuses :order-by :order-dir :owner :first :after] :as args}]
  (log/debug "search-meme-tokens-query-resolver" args)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [statuses-set (when statuses (set statuses))
                     page-start-idx (when after (js/parseInt after))
                     page-size first
                     query (cond-> {:select [:mto.* :mt.*]
                                    :from [[:meme-tokens :mt]]
                                    :join [[:reg-entries :re] [:= :mt.reg-entry/address :re.reg-entry/address]
                                           [:memes :m] [:= :mt.reg-entry/address :m.reg-entry/address]]
                                    :left-join [[:meme-token-owners :mto] [:= :mto.meme-token/token-id :mt.meme-token/token-id]]}
                             owner        (sqlh/merge-where [:= :mto.meme-token/owner owner])
                             statuses-set (sqlh/merge-where [:in (reg-entry-status-sql-clause now) statuses-set])
                             order-by     (sqlh/merge-order-by [[(get {:meme-tokens.order-by/meme-number    :mt.meme-token/number
                                                                       :meme-tokens.order-by/meme-title     :m.meme/title
                                                                       :meme-tokens.order-by/transferred-on :mt.meme-token/transferred-on
                                                                       :meme-tokens.order-by/token-id       :mt.meme-token/token-id}
                                                                      ;; TODO: move this transformation to district-server-graphql
                                                                      (graphql-utils/gql-name->kw order-by))
                                                                 (or (keyword order-dir) :asc)]]))]
                 (paged-query query page-size page-start-idx)))))

(defn meme-auction-query-resolver [_ {:keys [:meme-auction/address] :as args}]
  (log/debug "meme-auction args" args)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-auctions]
                            :where [:= address :meme-auctions.meme-auction/address]})]
     (log/debug "meme-auction query" sql-query)
     sql-query)))

(defn meme-auction-status-sql-clause [now]
  (sql/call ;; TODO: can we remove aliases here?
   :case
   [:not= :ma.meme-auction/canceled-on nil] (enum :meme-auction.status/canceled)
   [:= :ma.meme-auction/bought-on nil] (enum :meme-auction.status/active)
   :else (enum :meme-auction.status/done)))

(defn squash-by-min [k f coll]
  (->> coll
       (group-by k)
       (map (fn [[_ xs]]
              (first (sort-by f xs))))))

;; If testing this by hand remember params like statuses should be string and not keywords
;; like (search-meme-auctions-query-resolver nil {:statuses [(enum :meme-auction.status/active)]})
(defn search-meme-auctions-query-resolver [_ {:keys [:title :non-for-meme :for-meme
                                                     :tags :tags-or :tags-not :order-by :order-dir :group-by
                                                     :statuses :seller :first :after] :as args}]
  (log/debug "search-meme-auctions-query-resolver" args)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [statuses-set (when statuses (set statuses))
                     page-start-idx (when after (js/parseInt after))
                     page-size first
                     query (cond-> {:select [:ma.* :m.reg-entry/address :m.meme/total-minted :m.meme/number :mt.meme-token/number]
                                    :modifiers [:distinct]
                                    :from [[:meme-auctions :ma]]
                                    :join [[:meme-tokens :mt] [:= :mt.meme-token/token-id :ma.meme-auction/token-id]
                                           [:memes :m] [:= :mt.reg-entry/address :m.reg-entry/address]]
                                    :left-join [[:meme-tags :mtags] [:= :mtags.reg-entry/address :m.reg-entry/address]]
                                    :where [:= :ma.meme-auction/canceled-on nil]}
                             title         (sqlh/merge-where [:like :m.meme/title (str "%" title "%")])
                             seller        (sqlh/merge-where [:= :ma.meme-auction/seller seller])
                             tags          (sqlh/merge-where [:=
                                                              (count tags)
                                                              {:select [(sql/call :count :*)]
                                                               :from [[:meme-tags :mtts]]
                                                               :where [:and
                                                                       [:= :mtts.reg-entry/address :m.reg-entry/address]
                                                                       [:in :mtts.tag/name tags]]}])
                             non-for-meme (sqlh/merge-where [:not= :m.reg-entry/address non-for-meme])
                             for-meme     (sqlh/merge-where [:= :m.reg-entry/address for-meme])
                             tags-or      (sqlh/merge-where [:in :mtags.tag/name tags-or])
                             tags-not     (sqlh/merge-where [:or
                                                             [:not-in :mtags.tag/name tags-not]
                                                             [:= :mtags.tag/name nil]])
                             statuses-set (sqlh/merge-where [:in (meme-auction-status-sql-clause now) statuses-set])
                             order-by     (sqlh/merge-order-by [[(get {:meme-auctions.order-by/started-on :ma.meme-auction/started-on
                                                                       :meme-auctions.order-by/bought-on  :ma.meme-auction/bought-on
                                                                       :meme-auctions.order-by/token-id   :ma.meme-auction/token-id
                                                                       :meme-auctions.order-by/meme-total-minted :m.meme/total-minted
                                                                       :meme-auctions.order-by/meme-registry-number :m.meme/number
                                                                       :meme-auctions.order-by/random     (sql/call :random)}
                                                                      ;; TODO: move this transformation to district-server-graphql
                                                                      (graphql-utils/gql-name->kw order-by))
                                                                 (or (keyword order-dir) :asc)]]))]

                 ;; for this two is hard to do in sql since current price is calculated so for now doing it in Clojure
                 (if (or (and order-by (= (graphql-utils/gql-name->kw order-by) :meme-auctions.order-by/price))
                         (and group-by (#{:meme-auctions.group-by/cheapest :meme-auctions.group-by/lowest-card-number}
                                        (graphql-utils/gql-name->kw group-by))))

                   ;; group-by and order-by current price and pagination in Clojure
                   (let [result (cond->> (db/all query)

                                  ;; grouping cheapest
                                  group-by
                                  (squash-by-min :reg-entry/address (get {:meme-auctions.group-by/cheapest           (juxt #(shared-utils/calculate-meme-auction-price % now) :meme-token/number)
                                                                          :meme-auctions.group-by/lowest-card-number :meme-token/number}
                                                                         (graphql-utils/gql-name->kw group-by)))

                                  ;; ordering
                                  order-by
                                  (sort-by (get {:meme-auctions.order-by/started-on         :meme-auction/started-on
                                                 :meme-auctions.order-by/bought-on          :meme-auction/bought-on
                                                 :meme-auctions.order-by/token-id           :meme-auction/token-id
                                                 :meme-auctions.order-by/meme-total-minted  :meme/total-minted
                                                 :meme-auctions.order-by/meme-registry-number :meme/number
                                                 :meme-auctions.order-by/price              #(shared-utils/calculate-meme-auction-price % now)}
                                                (graphql-utils/gql-name->kw order-by)
                                                :meme-auction/started-on)

                                           (if (= (keyword order-dir) :desc) > <))

                                  ;; random is a special ordering case because we can't call sort-by for it
                                  (and order-by (= (graphql-utils/gql-name->kw order-by) :meme-auctions.order-by/random))
                                  (shuffle))
                         total-count (count result)
                         paged-result (cond->> result
                                        ;; pagination
                                        page-start-idx (drop page-start-idx)
                                        page-size (take page-size))
                         last-idx (cond-> (count paged-result)
                                    page-start-idx (+ page-start-idx))]

                     {:items paged-result
                      :total-count total-count
                      :end-cursor last-idx
                      :has-next-page (< (inc last-idx) total-count)})

                   ;; everything SQL
                   (paged-query query page-size page-start-idx))))))

(defn search-tags-query-resolver [_ {:keys [:first :after] :as args}]
  (log/debug "search-tags-query-resolver" args)
  (try-catch-throw
   (let [page-start-idx (when after (js/parseInt after))
         page-size first
         query {:select [:*]
                :from [:tags]}]
     (paged-query query page-size page-start-idx))))

(defn param-change-query-resolver [_ {:keys [:reg-entry/address] :as args}]
  (log/debug "param-change args" args)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:param-changes]
                            :join [:reg-entries [:= :reg-entries.reg-entry/address :param-changes.reg-entry/address]]
                            :where [:= address :param-changes.reg-entry/address]})]
     (log/debug "param-change query" sql-query)
     sql-query)))

(defn search-param-changes-query-resolver [_ {:keys [:key :db :order-by :order-dir :group-by :first :after :statuses :remove-applied]
                                              :or {order-dir :asc}
                                              :as args}]
  (log/debug "search-param-changes args" args)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [db (if (contains? #{"memeRegistryDb" "paramChangeRegistryDb"} db)
                          (smart-contracts/contract-address (graphql-utils/gql-name->kw db))
                          db)
                     statuses-set (when statuses (set statuses))
                     param-changes-query (cond-> {:select [:*]
                                                  :from [:param-changes]
                                                  :left-join [[:reg-entries :re] [:= :re.reg-entry/address :param-changes.reg-entry/address]]
                                                  :join [:params [:and
                                                                  [:= :param-changes.param-change/db :params.param/db]
                                                                  [:= :param-changes.param-change/key :params.param/key]]]
                                                  :where [:= :param-changes.param-change/original-value :params.param/value]}

                                           key (sqlh/merge-where [:= key :param-changes.param-change/key])

                                           remove-applied (sqlh/merge-where [:= nil :param-changes.param-change/applied-on])

                                           db (sqlh/merge-where [:= db :param-changes.param-change/db])
                                           statuses-set (sqlh/merge-where [:in (reg-entry-status-sql-clause now) statuses-set])
                                           ;; What is this for?
                                           ;; order-by (sqlh/merge-where [:not= nil :param-changes.param-change/applied-on])

                                           order-by (sqlh/merge-order-by [[(get {:param-changes.order-by/applied-on :param-changes.param-change/applied-on
                                                                                 :param-changes.order-by/created-on :re.reg-entry/created-on}
                                                                                (graphql-utils/gql-name->kw order-by))
                                                                           (keyword (or order-dir :desc))]])

                                           group-by (merge {:group-by [(get {:param-changes.group-by/key :param-changes.param-change/key}
                                                                            (graphql-utils/gql-name->kw group-by))]}))
                     param-changes-result (paged-query param-changes-query
                                                       first
                                                       (when after
                                                         (js/parseInt after)))]
                 param-changes-result))))

(defn user-query-resolver [_ {:keys [:user/address] :as args} context debug]
  (log/debug "user args" args)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:users]
                            :where [:= address :users.user/address]})]
     (log/debug "user query" sql-query)
     (when-not (empty? sql-query)
       sql-query))))

(defn search-users-query-resolver [_ {:keys [:order-by :order-dir :first :after] :or {order-dir :asc} :as args} _ document]
  (log/debug "search-users-query-resolver args" args)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [order-dir (keyword order-dir)
                     order-by-clause (when order-by (get {:users.order-by/address [[:user/address order-dir]]
                                                          :users.order-by/voter-total-earned [[:user/voter-total-earned order-dir]]
                                                          :users.order-by/challenger-total-earned [[:user/challenger-total-earned order-dir]
                                                                                                   [:user/total-created-challenges-success order-dir]]
                                                          :users.order-by/curator-total-earned [[:user/curator-total-earned order-dir]
                                                                                                [:user/total-participated-votes-success order-dir]]
                                                          :users.order-by/total-participated-votes-success [[:user/total-participated-votes-success order-dir]]
                                                          :users.order-by/total-participated-votes [[:user/total-participated-votes order-dir]]
                                                          :users.order-by/total-created-challenges-success [[:user/total-created-challenges-success order-dir]]
                                                          :users.order-by/total-created-challenges [[:user/total-created-challenges order-dir]]
                                                          :users.order-by/total-collected-memes [[:user/total-collected-memes order-dir]]
                                                          :users.order-by/total-collected-token-ids [[:user/total-collected-token-ids order-dir]]
                                                          :users.order-by/total-created-memes-whitelisted [[:user/total-created-memes-whitelisted order-dir]]
                                                          :users.order-by/total-created-memes [[:user/total-created-memes order-dir]]
                                                          :users.order-by/total-earned [[:user/total-earned order-dir]]
                                                          :users.order-by/best-single-card-sale [[:user/best-single-card-sale order-dir]]}
                                                         (graphql-utils/gql-name->kw order-by)))
                     fields (into (query-fields document :items) (map clojure.core/first order-by-clause))
                     select? #(contains? fields %)
                     query (paged-query {:select (remove nil?
                                                         [:*
                                                          (when (select? :user/curator-total-earned)
                                                            [(sql/call :+ :users.user/voter-total-earned :users.user/challenger-total-earned)
                                                             :user/curator-total-earned])
                                                          (when (select? :user/total-participated-votes-success)
                                                            [{:select [:%count.*]
                                                              :from [:votes]
                                                              :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
                                                              ;; This doesn't look right, looks like it should be counting user option for a challenge
                                                              ;; when that option is equal to the challenge winning result
                                                              :where [:and [:> now :reg-entries.challenge/reveal-period-end]
                                                                      [:>= :reg-entries.challenge/votes-against :reg-entries.challenge/votes-for]
                                                                      [:= :votes.vote/voter :users.user/address]]}
                                                             :user/total-participated-votes-success])
                                                          (when (select? :user/total-participated-votes)
                                                            [{:select [:%count.*]
                                                              :from [:votes]
                                                              :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
                                                              :where [:= :votes.vote/voter :users.user/address]}
                                                             :user/total-participated-votes])
                                                          (when (select? :user/total-created-challenges-success)
                                                            [{:select [:%count.* ]
                                                              :from [:reg-entries]
                                                              :where [:and [:> now :reg-entries.challenge/reveal-period-end]
                                                                      [:>= :reg-entries.challenge/votes-against :reg-entries.challenge/votes-for]
                                                                      [:= :reg-entries.challenge/challenger :users.user/address]]}
                                                             :user/total-created-challenges-success])
                                                          (when (select? :user/total-created-challenges)
                                                            [{:select [:%count.* ]
                                                              :from [:reg-entries]
                                                              :where [:= :reg-entries.challenge/challenger :users.user/address]}
                                                             :user/total-created-challenges])
                                                          (when (select? :user/total-collected-memes)
                                                            [{:select [(sql/call :count-distinct :meme-tokens.reg-entry/address)]
                                                              :from [:meme-token-owners]
                                                              :join [:meme-tokens [:= :meme-tokens.meme-token/token-id :meme-token-owners.meme-token/token-id]]
                                                              :where [:= :meme-token-owners.meme-token/owner :users.user/address]}
                                                             :user/total-collected-memes])
                                                          (when (select? :user/total-collected-token-ids)
                                                            [{:select [:%count.*]
                                                              :from [:meme-token-owners]
                                                              :where [:= :meme-token-owners.meme-token/owner :user/address]}
                                                             :user/total-collected-token-ids])
                                                          (when (select? :user/total-created-memes-whitelisted)
                                                            [{:select [:%count.* ]
                                                              :from [:memes]
                                                              :join [[:reg-entries :re] [:= :re.reg-entry/address :memes.reg-entry/address]]
                                                              :where [:and
                                                                      [:= :re.reg-entry/creator :user/address]
                                                                      [:= (reg-entry-status-sql-clause now) (enum :reg-entry.status/whitelisted)]]}
                                                             :user/total-created-memes-whitelisted])
                                                          (when (select? :user/total-created-memes)
                                                            [{:select [:%count.* ]
                                                              :from [:memes]
                                                              :join [:reg-entries [:= :reg-entries.reg-entry/address :memes.reg-entry/address]]
                                                              :where [:= :reg-entries.reg-entry/creator :user/address]}
                                                             :user/total-created-memes])])
                                         :from [:users]
                                         :order-by order-by-clause}
                                        first
                                        (when after
                                          (js/parseInt after)))]
                 (log/debug "search-users-query-resolver query" query)
                 query))))

(defn user-list->items-resolver [user-list]
  (log/debug "user-list->items-resolver args" user-list)
  (:items user-list))

(defn param-query-resolver [_ {:keys [:db :key] :as args}]
  (log/debug "param-query-resolver" args)
  (try-catch-throw
   (let [sql-query (db/get {:select [[:param-change/db :param/db]
                                     [:param-change/key :param/key]
                                     [:param-change/value :param/value] ]
                            :from [:param-changes]
                            :where [:and [:= db :param-changes.param-change/db]
                                    [:= key :param-changes.param-change/key]]
                            :order-by [:param-changes.param-change/applied-on]
                            :limit 1})]
     (log/debug "param-query-resolver" sql-query)
     sql-query)))

(defn params-query-resolver [_ {:keys [:db :keys] :as args}]
  (log/debug "params-query-resolver" args)
  (let [db (if (contains? #{"memeRegistryDb" "paramChangeRegistryDb"} db)
             (smart-contracts/contract-address (graphql-utils/gql-name->kw db))
             db)]
    (try-catch-throw
     (let [sql-query (mf-db/get-params db keys)]
       (log/debug "params-query-resolver" sql-query)
       sql-query))))

(defn events-query-resolver [_ {:keys [:event-name :contract-key] :as args}]
  (log/debug "events-query-resolver" args)
  (try-catch-throw
   (let [sql-query (mf-db/all-events)]
     (log/debug "events-query-resolver" sql-query)
     sql-query)))

(defn overall-stats-resolver [_ _]
  {:total-memes-count (:count (db/get {:select [[(sql/call :count :*) :count]]
                                       :from [[:memes :m]]
                                       :where [:not= :m.meme/number nil]}))
   :total-tokens-count (:count (db/get {:select [[(sql/call :count :*) :count]]
                                        :from [:meme-tokens]}))})

(defn config-query-resolver []
  (try-catch-throw
   (let [config-query (select-keys @config whitelisted-config-keys)]
     (log/debug "config-query-resolver" config-query ::config-query-resolver)
     config-query)))

(defn vote->option-resolver [{:keys [:vote/option :vote/amount] :as vote}]
  (log/debug "vote->option-resolver args" vote)
  (match [(nil? amount) (= 1 option) (= 2 option)]
         [true _ _] (enum :vote-option/no-vote)
         [false true false] (enum :vote-option/vote-for)
         [false false true] (enum :vote-option/vote-against)
         [false _ _] (enum :vote-option/not-revealed)))

(defn reg-entry->creator-resolver [{:keys [:reg-entry/creator] :as reg-entry}]
  (log/debug "reg-entry->creator-resolver args" reg-entry)
  {:user/address creator})

(defn reg-entry-winning-vote-option [{:keys [:reg-entry/address]}]
  (->> (db/all {:select [:vote/option [(sql/call :total :vote/amount) :count]]
                :from [:votes]
                :where [:and
                        [:not= :vote/revealed-on nil]
                        [:= :reg-entry/address address]]
                :group-by [:vote/option]})
       (apply (partial max-key :count))
       :vote/option
       registry-entry/vote-options))

(defn reg-entry->vote-winning-vote-option-resolver [{:keys [:reg-entry/address :reg-entry/status] :as reg-entry} {:keys [:vote/voter] :as args}]
  (log/debug "reg-entry->vote-winning-vote-option-resolver" {:reg-entry reg-entry :args args})
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (when (#{:reg-entry.status/blacklisted :reg-entry.status/whitelisted} (shared-utils/reg-entry-status now reg-entry))
                 (let [{:keys [:vote/option]} (db/get {:select [:vote/option]
                                                       :from [:votes]
                                                       :where [:and
                                                               [:= address :reg-entry/address]
                                                               [:= voter :vote/voter]]})]
                   (and option
                        (= (registry-entry/vote-options option)
                           (reg-entry-winning-vote-option reg-entry))))))))

(defn reg-entry->all-rewards-resolver [{:keys [:reg-entry/address :challenge/reward-pool :challenge/claimed-reward-on
                                               :reg-entry/deposit :challenge/challenger :challenge/votes-against :challenge/votes-for] :as reg-entry} args]
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [challenger-amount (if (and (or (zero? claimed-reward-on)
                                                    (not claimed-reward-on))
                                                (= challenger (:user/address args))
                                                (= :vote.option/vote-against (reg-entry-winning-vote-option reg-entry)))
                                         (+ deposit (- deposit reward-pool))
                                         0)
                     voter-amount (let [{:keys [:vote/option :vote/amount :vote/claimed-reward-on] :as v}
                                        (db/get {:select [:vote/option :vote/amount :vote/claimed-reward-on]
                                                 :from [:votes]
                                                 :where [:and
                                                         [:= address :reg-entry/address]
                                                         [:= (:user/address args) :vote/voter]]})
                                        winning-option (reg-entry-winning-vote-option reg-entry)
                                        winning-amount (case winning-option
                                                         :vote.option/vote-against votes-against
                                                         :vote.option/vote-for votes-for
                                                         0)]

                                    (if (and (not claimed-reward-on)
                                             amount
                                             (= winning-option (registry-entry/vote-options option))
                                             (#{:reg-entry.status/blacklisted :reg-entry.status/whitelisted} (shared-utils/reg-entry-status now reg-entry)))
                                      (/ (* amount reward-pool) winning-amount)
                                      0))]
                 ;; TODO how to do about this?
                 {:challenge/reward-amount (js/Math.ceil challenger-amount)
                  :vote/reward-amount (js/Math.ceil voter-amount)}))))

(defn reg-entry->challenger [{:keys [:challenge/challenger] :as reg-entry}]
  (log/debug "reg-entry->challenger-resolver args" reg-entry)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:users]
                            :where [:= challenger :users.user/address]})]
     (log/debug "reg-entry->challenger-resolver query" sql-query)
     (when challenger
       sql-query))))

(defn vote->reward-resolver [{:keys [:reg-entry/address :vote/option] :as vote}]
  (log/debug "vote->reward-resolver args" vote)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [status (shared-utils/reg-entry-status now (db/get {:select [:*]
                                                                        :from [:reg-entries]
                                                                        :where [:= address :reg-entry/address]}))
                     {:keys [:challenge/reward-pool :votes/for :votes/against] :as sql-query} (db/get {:select [[{:select [:challenge/reward-pool]
                                                                                                                  :from [:reg-entries]
                                                                                                                  :where [:= address :reg-entry/address]} :challenge/reward-pool]
                                                                                                                [{:select [:%count.*]
                                                                                                                  :from [:votes]
                                                                                                                  :where [:and [:= address :votes.reg-entry/address]
                                                                                                                          [:= 1 :votes.vote/option]]} :votes/for]
                                                                                                                [{:select [:%count.*]
                                                                                                                  :from [:votes]
                                                                                                                  :where [:and [:= address :votes.reg-entry/address]
                                                                                                                          [:= 2 :votes.vote/option]]} :votes/against]]})]
                 (log/debug "vote->reward-resolver query" sql-query)
                 (cond
                   (and (= :reg-entry.status/whitelisted status)
                        (= option 1))
                   (/ reward-pool for)

                   (and (= :reg-entry.status/blacklisted status)
                        (= option 2))
                   (/ reward-pool against)

                   :else nil)))))

(defn reg-entry->vote-resolver [{:keys [:reg-entry/address] :as reg-entry} {:keys [:vote/voter]}]
  (log/debug "reg-entry->vote args" {:reg-entry reg-entry :voter voter})
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:votes]
                            :where [:and
                                    [:= voter :votes.vote/voter]
                                    [:= address :votes.reg-entry/address]]})]
     (log/debug "reg-entry->vote query" sql-query)
     sql-query)))

(defn meme->owned-meme-tokens [{:keys [:reg-entry/address] :as meme} {:keys [:owner] :as args}]
  (try-catch-throw
   (let [query (merge {:select [:mto.* :mt.*]
                       :modifiers [:distinct]
                       :from [[:meme-tokens :mt]]
                       :join [[:reg-entries :re] [:= :mt.reg-entry/address :re.reg-entry/address]]
                       :left-join [[:meme-token-owners :mto] [:= :mto.meme-token/token-id :mt.meme-token/token-id]]
                       :where [:and
                               [:= :mt.reg-entry/address address]
                               [:= :mto.meme-token/owner owner]]})]
     (db/all query))))

(defn meme->tags [{:keys [:reg-entry/address] :as meme}]
  (try-catch-throw
   (db/all {:select [:tag/name]
            :from [:meme-tags]
            :where [:= :reg-entry/address address]})))

(defn meme->meme-auctions-resolver [{:keys [:reg-entry/address] :as meme} {:keys [:order-by :order-dir :completed :open] :as opts}]
  (log/debug "meme->meme-auctions-resolver" {:args meme :opts opts})
  (try-catch-throw
   (let [sql-query (db/all (cond-> {:select [:*]
                                    :from [:meme-auctions]
                                    :join [:meme-tokens [:= :meme-tokens.meme-token/token-id :meme-auctions.meme-auction/token-id]
                                           :memes [:= :memes.reg-entry/address :meme-tokens.reg-entry/address]]
                                    :where [:= :memes.reg-entry/address address]}
                             order-by (sqlh/merge-order-by [[(get {:meme-auctions.order-by/token-id :meme-auctions.meme-auction/token-id
                                                                   :meme-auctions.order-by/seller :meme-auctions.meme-auction/seller
                                                                   :meme-auctions.order-by/buyer :meme-auctions.meme-auction/buyer
                                                                   :meme-auctions.order-by/price :meme-auctions.meme-auction/bought-for
                                                                   :meme-auctions.order-by/bought-on :meme-auctions.meme-auction/bought-on}
                                                                  (graphql-utils/gql-name->kw order-by))
                                                             (or (keyword order-dir) :asc)]])
                             completed (sqlh/merge-where [:not= :meme-auctions.meme-auction/buyer nil])
                             open (sqlh/merge-where [:= :meme-auctions.meme-auction/buyer nil])))]

     (log/debug "meme->meme-auctions-resolver query" sql-query)
     sql-query)))

(defn meme-list->items-resolver [meme-list]
  (:items meme-list))

(defn tag-list->items-resolver [tag-list]
  (:items tag-list))

(defn meme-token->owner-resolver [{:keys [:meme-token/token-id] :as meme-token}]
  (log/debug "meme-token->owner-resolver args" meme-token)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [[:users :u]]
                            :join [[:meme-token-owners :mto] [:= :mto.meme-token/owner :u.user/address]]
                            :where [:= token-id :mto.meme-token/token-id]})]
     (log/debug "meme-token->owner-resolver query" sql-query)
     sql-query)))

(defn meme-token->meme-resolver [{:keys [:meme-token/token-id] :as meme-token}]
  (log/debug "meme-token->meme-resolver args" meme-token)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-tokens]
                            :join [:reg-entries [:= :reg-entries.reg-entry/address :meme-tokens.reg-entry/address]
                                   :memes [:= :reg-entries.reg-entry/address :memes.reg-entry/address]]
                            :where [:= token-id :meme-tokens.meme-token/token-id]})]
     (log/debug "meme-token->meme-resolver query" sql-query)
     sql-query)))

(defn meme-token-list->items-resolver [meme-token-list]
  (:items meme-token-list))

(defn meme-auction->seller-resolver [{:keys [:meme-auction/seller] :as meme-auction}]
  (log/debug "meme-auction->seller-resolver args" meme-auction)
  {:user/address seller})

(defn meme-auction->buyer-resolver [{:keys [:meme-auction/buyer :meme-auction/bought-on] :as meme-auction}]
  (log/debug "meme-auction->buyer-resolver args" meme-auction)
  (when bought-on
    {:user/address buyer}))

(defn meme-auction->status-resolver [{:keys [:meme-auction/started-on :meme-auction/duration
                                             :meme-auction/canceled-on :meme-auction/bought-on] :as meme-auction}]
  (log/debug "meme-auction->status-resolver args" meme-auction)
  (cond
    (nil? (or started-on duration canceled-on bought-on))
    nil

    (not (nil? canceled-on))
    (enum :meme-auction.status/canceled)

    (nil? bought-on)
    (enum :meme-auction.status/active)

    :else (enum :meme-auction.status/done)))

(defn meme-auction->meme-token-resolver [{:keys [:meme-auction/token-id] :as meme-auction}]
  (log/debug "meme-auction->meme-token-resolver args" meme-auction)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-tokens]
                            :where [:= token-id :meme-tokens.meme-token/token-id]})]
     (log/debug "meme-auction->meme-token-resolver query" sql-query)
     sql-query)))

(defn meme-auction-list->items-resolver [meme-auction-list]
  (:items meme-auction-list))

(defn param-change-list->items-resolver [param-change-list]
  (:items param-change-list))

(defn user->total-created-memes-resolver
  [{:keys [:user/address :user/total-created-memes] :as user}]
  (log/debug "user->total-created-memes-resolver args" user)
  (try-catch-throw
   (if total-created-memes
     total-created-memes
     (let [sql-query (when address
                       (db/get {:select [[:%count.* :user/total-created-memes]]
                                :from [:memes]
                                :join [:reg-entries [:= :reg-entries.reg-entry/address :memes.reg-entry/address]]
                                :where [:= address :reg-entries.reg-entry/creator]}))]
       (log/debug "user->total-created-memes-resolver query" sql-query)
       (:user/total-created-memes sql-query)))))

(defn user->total-created-memes-whitelisted-resolver
  [{:keys [:user/address
           :user/total-created-memes-whitelisted] :as user}]
  (log/debug "user->total-created-memes-whitelisted-resolver args" user)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (if total-created-memes-whitelisted
                 total-created-memes-whitelisted
                 (when address
                   (let [sql-query (when address
                                     (db/all {:select [:*]
                                              :from [:memes]
                                              :join [:reg-entries [:= :reg-entries.reg-entry/address :memes.reg-entry/address]]
                                              :where [:= address :reg-entries.reg-entry/creator]}))]
                     (log/debug "user->total-created-memes-whitelisted-resolver query" sql-query)
                     (count (filter (fn [e] (= :reg-entry.status/whitelisted (shared-utils/reg-entry-status now e)))
                                    sql-query))))))))

(defn user->creator-largest-sale-resolver
  "Largest sale creator has done with his newly minted meme"
  [{:keys [:user/address] :as user}]
  (log/debug "user->creator-largest-sale-resolver args" user)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-auctions]
                            :join [:reg-entries [:= address :reg-entries.reg-entry/creator]
                                   :memes [:= :memes.reg-entry/address :reg-entries.reg-entry/address]]
                            :where [:and [:= {:select [(sql/call :max :meme-auctions.meme-auction/bought-for)]
                                              :from [:meme-auctions]}
                                          :meme-auctions.meme-auction/bought-for]
                                    [:= address :meme-auction/seller]]})]
     (log/debug "user->creator-largest-sale-resolver query" sql-query)
     sql-query)))

(defn user->largest-sale-resolver [{:keys [:user/address] :as user}]
  (log/debug "user->largest-sale-resolver args" user)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-auctions]
                            :where [:and [:= {:select [(sql/call :max :meme-auctions.meme-auction/bought-for)]
                                              :from [:meme-auctions]
                                              :where [:= address :meme-auction/seller]}
                                          :meme-auctions.meme-auction/bought-for]
                                    [:= address :meme-auction/seller]]})]
     (log/debug "user->largest-sale-resolver query" sql-query)
     (when (not-empty sql-query)
       sql-query))))

(defn user->total-collected-token-ids-resolver
  "Amount of meme tokenIds owned by user"
  [{:keys [:user/address
           :user/total-collected-token-ids] :as user}]
  (log/debug "user->total-collected-token-ids-resolver args" user)
  (try-catch-throw
   (if total-collected-token-ids
     total-collected-token-ids
     (let [sql-query (when address
                       (db/get {:select [[:%count.* :user/total-collected-token-ids]]
                                :from [:meme-token-owners]
                                :where [:= address :meme-token-owners.meme-token/owner]}))]
       (log/debug "user->total-collected-token-ids-resolver query" sql-query)
       (:user/total-collected-token-ids sql-query)))))

(defn user->total-collected-memes-resolver
  [{:keys [:user/address :user/total-collected-memes] :as user}]
  (log/debug "user->total-collected-memes-resolver args" user)
  (try-catch-throw
   (if total-collected-memes
     total-collected-memes
     (let [sql-query (when address
                       (db/get {:select [[(sql/call :count-distinct :meme-tokens.reg-entry/address) :user/total-collected-memes]]
                                :from [:meme-token-owners]
                                :join [:meme-tokens [:= :meme-tokens.meme-token/token-id :meme-token-owners.meme-token/token-id]]
                                :where [:= address :meme-token-owners.meme-token/owner]}))]
       (log/debug "user->total-collected-memes-resolver query" sql-query)
       (:user/total-collected-memes sql-query)))))

(defn user->largest-buy-resolver [{:keys [:user/address] :as user}]
  (log/debug "user->largest-buy-resolver args" user)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-auctions]
                            :where [:and [:= {:select [(sql/call :max :meme-auctions.meme-auction/bought-for)]
                                              :from [:meme-auctions]
                                              :where [:= address :meme-auction/buyer]}
                                          :meme-auctions.meme-auction/bought-for]
                                    [:= address :meme-auction/buyer]]})
         {:keys [:meme-auction/buyer]} sql-query]
     (log/debug "user->largest-buy-resolver query" sql-query)
     (when buyer
       sql-query))))

(defn user->total-created-challenges-resolver
  [{:keys [:user/address] :as user}]
  (log/debug "user->total-created-challenges-resolver args" user)
  (try-catch-throw
   (let [sql-query (when address
                     (db/get {:select [[:%count.* :user/total-created-challenges]]
                              :from [:reg-entries]
                              :where [:= address :reg-entries.challenge/challenger]}))]
     (log/debug "user->total-created-challenges-resolver query" sql-query)
     (:user/total-created-challenges sql-query))))

(defn user->total-created-challenges-success-resolver
  [{:keys [:user/address :user/total-created-challenges-success] :as user}]
  (log/debug "user->total-created-challenges-success-resolver args" user)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (if (pos? total-created-challenges-success)
                 total-created-challenges-success
                 (let [sql-query (when address
                                   (db/get {:select [[:%count.* :user/total-created-challenges-success]]
                                            :from [:reg-entries]
                                            :where [:and [:> now :reg-entries.challenge/reveal-period-end]
                                                    [:>= :reg-entries.challenge/votes-against :reg-entries.challenge/votes-for]
                                                    [:= address :reg-entries.challenge/challenger]]}))]
                   (log/debug "user->total-created-challenges-success-resolver query" sql-query)
                   (:user/total-created-challenges-success sql-query))))))

(defn user->total-participated-votes-resolver
  "Amount of different votes user participated in"
  [{:keys [:user/address] :as user}]
  (log/debug "user->total-participated-votes-resolver args" user)
  (try-catch-throw
   (let [sql-query (when address
                     (db/get {:select [[:%count.* :user/total-participated-votes]]
                              :from [:votes]
                              :where [:= address :votes.vote/voter]}))]
     (log/debug "user->total-participated-votes-resolver query" sql-query)
     (:user/total-participated-votes sql-query))))

(defn user->total-participated-votes-success-resolver
  "Amount of different votes user voted for winning option"
  [{:keys [:user/address] :as user}]
  (log/debug "user->total-participated-votes-success-resolver args" user)
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (let [sql-query (when address
                                 (db/all {:select [:*]
                                          :from [:votes]
                                          :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
                                          :where [:= address :votes.vote/voter]}))]
                 (log/debug "user->total-participated-votes-success-resolver query" sql-query)
                 (reduce (fn [total {:keys [:vote/option] :as reg-entry}]
                           (let [ status (shared-utils/reg-entry-status (utils/now-in-seconds) reg-entry)]
                             (if (or (and (= :reg-entry.status/whitelisted status) (= 1 option))
                                     (and (= :reg-entry.status/blacklisted status) (= 2 option)))
                               (inc total)
                               total)))
                         0
                         sql-query)))))

(defn user->curator-total-earned-resolver
  [{:keys [:user/voter-total-earned
           :user/challenger-total-earned] :as user} parent]
  (log/debug "user->curator-total-earned-resolver args" user)
  (try-catch-throw
   (when (and voter-total-earned challenger-total-earned)
     (+ voter-total-earned challenger-total-earned))))

(defn user->creator-total-earned-resolver [user]
  (try-catch-throw
   (->> (db/all {:select [:*]
                 :from [:meme-auctions]
                 :where  [:= :meme-auction/seller (:user/address user)]})
        (map :meme-auction/bought-for)
        (reduce +))))

(defn all-users [] (db/all {:select [:*] :from [:users]}))

(defn creator-rank [now]
  (let [users-whitelisted-memes (->> (db/all {:select [:re.reg-entry/creator [(sql/call :count :*) :count] [(reg-entry-status-sql-clause now) :status]]
                                              :from [[:reg-entries :re]]
                                              :where [:= :status (graphql-utils/kw->gql-name :reg-entry.status/whitelisted)]
                                              :group-by [:re.reg-entry/creator]})
                                     (map (fn [{:keys [:reg-entry/creator :count]}] [creator count]))
                                     (into {}))]
    (->> (all-users)
         (map #(assoc % :white-listed-memes (get users-whitelisted-memes (:user/address %) 0)))
         (sort-by :white-listed-memes >)
         (map-indexed (fn [idx u] [(:user/address u) (inc idx)]))
         (into {}))))

(defn challenger-rank []
  (->> (db/all {:select [:u.user/address :u.user/challenger-total-earned]
                :from [[:users :u]]
                :order-by [[:u.user/challenger-total-earned :desc]]})
       (map-indexed (fn [idx {:keys [:user/address]}] [address (inc idx)]))
       (into {})))

(defn voter-rank []
  (->> (db/all {:select [:u.user/address :u.user/voter-total-earned]
                :from [[:users :u]]
                :order-by [[:u.user/voter-total-earned :desc]]})
       (map-indexed (fn [idx {:keys [:user/address]}] [address (inc idx)]))
       (into {})))

(defn curator-rank []
  (->> (db/all {:select [:u.user/address [(sql/call :+ :u.user/challenger-total-earned :u.user/voter-total-earned) :curator-total-earned]]
                :from [[:users :u]]
                :order-by [[:curator-total-earned :desc]]})
       (map-indexed (fn [idx {:keys [:user/address]}] [address (inc idx)]))
       (into {})))

(defn collector-rank []
  (->> (db/all {:select [:u.user/address [(sql/call :count :*) :uniq-memes]]
                :from [[:users :u]]
                :left-join [[:meme-token-owners :mto]
                            [:= :u.user/address :mto.meme-token/owner]]
                :group-by [:u.user/address]
                :order-by [[:uniq-memes :desc]]})
       (map-indexed (fn [idx {:keys [:user/address]}] [address (inc idx)]))
       (into {})))

(defn user->creator-rank-resolver [{:keys [:user/address]}]
  (promise-> (utils/now-in-seconds)
             (fn [now]
               (get (ranks-cache/get-rank :creator-rank (partial creator-rank now))
                    address))))

(defn user->challenger-rank-resolver [{:keys [:user/address]}]
  (get (ranks-cache/get-rank :challenger-rank challenger-rank)
       address))

(defn user->voter-rank-resolver [{:keys [:user/address]}]
  (get (ranks-cache/get-rank :voter-rank voter-rank)
       address))

(defn user->curator-rank-resolver [{:keys [:user/address]}]
  (get (ranks-cache/get-rank :curator-rank curator-rank)
       address))

(defn user->collector-rank-resolver [{:keys [:user/address]}]
  (get (ranks-cache/get-rank :collector-rank collector-rank)
       address))

;; TODO:
#_(defn param-change->original-value-resolver [{:keys [:param-change/db :param-change/key] :as args}]
    (log/debug "param-change->original-value-resolver" args)
    (:param-change/value (second (db/all {:select [:param-change/value]
                                          :from [:param-changes]
                                          :where [:and [:not= :param-changes.param-change/applied-on nil]
                                                  [:= key :param-changes.param-change/key]]
                                          :order-by [[:param-changes.param-change/applied-on :asc]]
                                          :limit 2}))))

(defn send-verification-code-resolver
  [_ {:keys [country-code phone-number] :as args}]
  (let [options {:url "https://api.authy.com/protected/json/phones/verification/start"
                 :method "POST"
                 :headers {"X-Authy-API-Key" (:twilio-api-key @config)}
                 :json true
                 :body {"via" "sms"
                        "phone_number" phone-number
                        "country_code" country-code}}]
    (log/info "Sending verification code" {:args args :options options})
    (promise-> (request-promise (clj->js options))
               (fn [response]
                 (let [twilio-response (-> response
                                           (js->clj :keywordize-keys true)
                                           (clj-set/rename-keys {:uuid :id}))]
                   (log/info "Phone verification response" twilio-response)
                   (if (:success twilio-response)
                     twilio-response
                     (log/error "Error calling phone verification API" twilio-response)))))))

(defn encrypt-verification-payload-resolver
  [_ {:keys [country-code phone-number verification-code] :as args}]
  (log/info "Received verification code" args)
  ;; Build Oraclize call
  (cond
    (not (re-matches #"[0-9\+]{2,6}" (str country-code)))
    (do
      (log/warn (str "Invalid country code: " country-code) ::faucet-invalid-input)
      {:success false :payload "Invalid country code"})

    (not (re-matches #"[0-9\-]{5,18}" (str phone-number)))
    (do
      (log/warn (str "Invalid phone number: " phone-number) ::faucet-invalid-input)
      {:success false :payload "Invalid phone number"})

    (not (re-matches #"[0-9]{4,4}" (str verification-code)))
    (do
      (log/warn (str "Invalid verification code format: " verification-code) ::faucet-invalid-input)
      {:success false :payload "Invalid verification code format"})

    :else
    (let [encryption-script "./resources/scripts/encrypted_queries_tools.py"
          oraclize-public-key (:oraclize-public-key @config)
          json-payload (->> (clj->js {:json {:via "sms"
                                             :phone_number phone-number
                                             :country_code country-code
                                             :verification_code verification-code}
                                      :headers {:content-type "application/json"
                                                "X-Authy-API-Key" (:twilio-api-key @config)}})
                            (.stringify js/JSON))
          full-encryption-command (str "python "
                                       encryption-script
                                       " -p "
                                       oraclize-public-key
                                       " -e "
                                       \' json-payload \')]

      (log/debug "Shell out to python" {:bash full-encryption-command})

      (-> (exec-promise full-encryption-command)
          (.then (fn [result]
                   (let [python-result (js->clj result)]
                     (log/debug "python encryption result:" python-result)
                     (let [stdout (get python-result "stdout")
                           stderr (get python-result "stderr")]
                       {:success true
                        :payload (string/trim-newline stdout)}))))
          (.catch (fn [ex]
                    (log/error "Error calling python" {:error ex})
                    {:success false
                     :payload (.getMessage ex)}))))))

(defn blacklist-reg-entry-resolver [_ {:keys [address token] :as args}]
  (let [{:keys [blacklist-token blacklist-file]} @config]
    (if (= token blacklist-token)
      (do
        ;; update the blacklist file
        (-> (utils/load-edn-file blacklist-file)
            (update :blacklisted-image-addresses conj address)
            (utils/save-to-edn-file blacklist-file))
        ;;  patch on db
        (mf-db/patch-forbidden-reg-entry-image! address)
        (log/info (str "Blacklisted " address " image.") ::blacklist-reg-entry-resolver))
      (log/warn (str "Tried to blacklist reg entry " address " with wrong token " token) ::blacklist-reg-entry-resolver))))

(def resolvers-map
  {:Query {:meme meme-query-resolver
           :search-memes search-memes-query-resolver
           :search-meme-tokens search-meme-tokens-query-resolver
           :meme-auction meme-auction-query-resolver
           :search-meme-auctions search-meme-auctions-query-resolver
           :search-tags search-tags-query-resolver
           :param-change param-change-query-resolver
           :search-param-changes search-param-changes-query-resolver
           :user user-query-resolver
           :search-users search-users-query-resolver
           :param param-query-resolver
           :params params-query-resolver
           :overall-stats overall-stats-resolver
           :config config-query-resolver
           :events events-query-resolver}
   :Mutation {:send-verification-code send-verification-code-resolver
              :encrypt-verification-payload encrypt-verification-payload-resolver
              :blacklist-reg-entry blacklist-reg-entry-resolver}
   :Vote {:vote/option vote->option-resolver
          :vote/reward vote->reward-resolver}
   :Meme {:reg-entry/creator reg-entry->creator-resolver
          :challenge/vote-winning-vote-option reg-entry->vote-winning-vote-option-resolver
          :challenge/all-rewards reg-entry->all-rewards-resolver
          :challenge/challenger reg-entry->challenger
          :challenge/vote reg-entry->vote-resolver
          :meme/owned-meme-tokens meme->owned-meme-tokens
          :meme/tags meme->tags
          :meme/meme-auctions meme->meme-auctions-resolver}
   :MemeList {:items meme-list->items-resolver}
   :TagList {:items tag-list->items-resolver}
   :MemeToken {:meme-token/owner meme-token->owner-resolver
               :meme-token/meme meme-token->meme-resolver}
   :MemeTokenList {:items meme-token-list->items-resolver}
   :MemeAuction {:meme-auction/seller meme-auction->seller-resolver
                 :meme-auction/buyer meme-auction->buyer-resolver
                 :meme-auction/status meme-auction->status-resolver
                 :meme-auction/meme-token meme-auction->meme-token-resolver}
   :MemeAuctionList {:items meme-auction-list->items-resolver}
   :ParamChange {:reg-entry/creator reg-entry->creator-resolver
                 :challenge/challenger reg-entry->challenger
                 :challenge/all-rewards reg-entry->all-rewards-resolver
                 :challenge/vote reg-entry->vote-resolver}
   :ParamChangeList {:items param-change-list->items-resolver}
   :User {:user/total-created-memes user->total-created-memes-resolver
          :user/total-created-memes-whitelisted user->total-created-memes-whitelisted-resolver
          :user/creator-largest-sale user->creator-largest-sale-resolver
          :user/total-collected-token-ids user->total-collected-token-ids-resolver
          :user/total-collected-memes user->total-collected-memes-resolver
          :user/largest-sale user->largest-sale-resolver
          :user/largest-buy user->largest-buy-resolver
          :user/total-created-challenges user->total-created-challenges-resolver
          :user/total-created-challenges-success user->total-created-challenges-success-resolver
          :user/total-participated-votes user->total-participated-votes-resolver
          :user/total-participated-votes-success user->total-participated-votes-success-resolver
          :user/curator-total-earned user->curator-total-earned-resolver
          :user/creator-total-earned user->creator-total-earned-resolver
          :user/creator-rank user->creator-rank-resolver
          :user/challenger-rank user->challenger-rank-resolver
          :user/voter-rank user->voter-rank-resolver
          :user/curator-rank user->curator-rank-resolver
          :user/collector-rank user->collector-rank-resolver
          }
   :UserList {:items user-list->items-resolver}})
