(ns memefactory.server.graphql-resolvers
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [district.server.db :as db]
            [district.graphql-utils :as graphql-utils]
            [honeysql.core :as sql]
            [honeysql.helpers :as sqlh]
            [district.server.web3 :as web3]
            [cljs-web3.core :as web3-core]
            [cljs-web3.eth :as web3-eth]
            [taoensso.timbre :as log]
            [memefactory.server.db :as meme-db]
            [clojure.string :as str]
            [print.foo :refer [look] :include-macros true])
  (:require-macros [memefactory.server.macros :refer [try-catch-throw]]))

(def enum graphql-utils/kw->gql-name)

(def graphql-fields (nodejs/require "graphql-fields"))

(defn- query-fields
  "Returns the first order fields"
  [document & [path]]
  (->> (-> document
           graphql-fields
           (js->clj))
       (#(if-let [p (name path)]
           (get-in % [p])
           %))
       keys
       (map graphql-utils/gql-name->kw)
       set))

(defn- last-block-timestamp []
  (->> (web3-eth/block-number @web3/web3) (web3-eth/get-block @web3/web3) :timestamp))

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
    {:items result
     :total-count total-count
     :end-cursor (str last-idx)
     :has-next-page (not= last-idx total-count)}))

(defn meme-query-resolver [_ {:keys [:reg-entry/address] :as args}]
  (db/get {:select [:*]
           :from [[:memes :m] [:reg-entries :re]]
           :where [:and
                   [:= :m.reg-entry/address :re.reg-entry/address]
                   [:= :re.reg-entry/address address]]}))

(defn reg-entry-status [now {:keys [:reg-entry/created-on :reg-entry/challenge-period-end :challenge/challenger
                                    :challenge/commit-period-end :challenge/commit-period-end
                                    :challenge/reveal-period-end :challenge/votes-for :challenge/votes-against]}]

  (cond
    (and (< now challenge-period-end) (not challenger)) :reg-entry.status/challenge-period
    (< now commit-period-end)                           :reg-entry.status/commit-period
    (< now reveal-period-end)                           :reg-entry.status/reveal-period
    (or (< votes-against votes-for)
        (< challenge-period-end now))                   :reg-entry.status/whitelisted
    :else                                               :reg-entry.status/blacklisted))

(defn meme-status-sql-clause [now]
  (sql/call ;; TODO: can we remove aliases here?
   :case
   [:and
    [:< now :re.reg-entry/challenge-period-end]
    [:= :re.challenge/challenger nil]]                        (enum :reg-entry.status/challenge-period)
   [:< now :re.challenge/commit-period-end]                   (enum :reg-entry.status/commit-period)
   [:< now :re.challenge/reveal-period-end]                   (enum :reg-entry.status/reveal-period)
   [:or
    [:< :re.challenge/votes-against :re.challenge/votes-for]
    [:< :re.reg-entry/challenge-period-end now]]              (enum :reg-entry.status/whitelisted)
   :else                                                      (enum :reg-entry.status/blacklisted)))

(defn search-memes-query-resolver [_ {:keys [:statuses :order-by :order-dir :owner :creator :curator :first :after] :as args}]
  (log/debug "search-memes-query-resolver" args)
  (try-catch-throw
   (let [statuses-set (when statuses (set statuses))
         now (last-block-timestamp)
         page-start-idx (when after (js/parseInt after))
         page-size first
         query (cond-> {:select [:re.* :m.*]
                        :modifiers [:distinct]
                        :from [[:memes :m]]
                        :join [[:reg-entries :re] [:= :m.reg-entry/address :re.reg-entry/address]]
                        :left-join [[{:select [:mt.reg-entry/address :mt.meme-token/token-id :mto.meme-token/owner]
                                      :from [[:meme-tokens :mt]]
                                      :join [[:meme-token-owners :mto]
                                             [:= :mto.meme-token/token-id :mt.meme-token/token-id]]} :tokens]
                                    [:= :m.reg-entry/address :tokens.reg-entry/address]]}

                 creator      (sqlh/merge-where [:= :re.reg-entry/creator creator])
                 curator      (sqlh/merge-where [:= :re.challenge/challenger curator])
                 owner        (sqlh/merge-where [:= :tokens.meme-token/owner owner])
                 statuses-set (sqlh/merge-where [:in (meme-status-sql-clause now) statuses-set])
                 order-by     (sqlh/merge-order-by [[(get {:memes.order-by/reveal-period-end    :re.challenge/reveal-period-end
                                                           :memes.order-by/commited-period-end  :re.challenge/commit-period-end
                                                           :memes.order-by/challenge-period-end :re.reg-entry/challenge-period-end
                                                           :memes.order-by/total-trade-volume   :m.meme/total-trade-volume
                                                           :memes.order-by/created-on           :re.reg-entry/created-on
                                                           :memes.order-by/number               :m.meme/number
                                                           :memes.order-by/total-minted         :m.meme/total-minted}
                                                          ;; TODO: move this transformation to district-server-graphql
                                                          (graphql-utils/gql-name->kw order-by))
                                                     (or (keyword order-dir) :asc)]]))]
     (paged-query query page-size page-start-idx))))

(defn search-meme-tokens-query-resolver [_ {:keys [:statuses :order-by :order-dir :owner :first :after] :as args}]
  (log/debug "search-meme-tokens-query-resolver" args)
  (try-catch-throw
   (let [statuses-set (when statuses (set statuses))
         now (last-block-timestamp)
         page-start-idx (when after (js/parseInt after))
         page-size first
         query (cond-> {:select [:mto.* :mt.*]
                        :from [[:meme-tokens :mt]]
                        :join [[:reg-entries :re] [:= :mt.reg-entry/address :re.reg-entry/address]
                               [:memes :m] [:= :mt.reg-entry/address :m.reg-entry/address]
                               [:meme-auctions :ma] [:= :ma.meme-auction/token-id :mt.meme-token/token-id]]
                        :left-join [[:meme-token-owners :mto] [:= :mto.meme-token/token-id :mt.meme-token/token-id]]}
                 owner        (sqlh/merge-where [:= :mto.meme-token/owner owner])
                 statuses-set (sqlh/merge-where [:in (meme-status-sql-clause now) statuses-set])
                 order-by     (sqlh/merge-order-by [[(get {:meme-tokens.order-by/meme-number    :mt.meme-token/number
                                                           :meme-tokens.order-by/meme-title     :m.meme/title
                                                           :meme-tokens.order-by/transferred-on :mt.meme-token/transferred-on
                                                           :meme-tokens.order-by/token-id       :mt.meme-token/token-id}
                                                          ;; TODO: move this transformation to district-server-graphql
                                                          (graphql-utils/gql-name->kw order-by))
                                                     (or (keyword order-dir) :asc)]]))]
     (paged-query query page-size page-start-idx))))

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
   [:not= :ma.meme-auction/canceled-on nil]                                       (enum :meme-auction.status/canceled)
   [:and
    [:< [:+ :ma.meme-auction/started-on :ma.meme-auction/duration] now]
    [:= :ma.meme-auction/bought-on nil]]                                          (enum :meme-auction.status/active)
   :else                                                                          (enum :meme-auction.status/done)))

(defn search-meme-auctions-query-resolver [_ {:keys [:title :tags :tags-or :order-by :order-dir :group-by :statuses :seller :first :after] :as args}]
  (log/debug "search-meme-auctions-query-resolver" args)
  (try-catch-throw
   (let [statuses-set (when statuses (set statuses))
         now (last-block-timestamp)
         page-start-idx (when after (js/parseInt after))
         page-size first
         query (cond-> {:select (cond-> [:ma.*]
                                  (and group-by
                                       (= (graphql-utils/gql-name->kw group-by)
                                          :meme-auctions.group-by/cheapest))
                                  (conj (sql/call :min :ma.meme-auction/bought-for)))
                        :modifiers [:distinct]
                        :from [[:meme-auctions :ma]]
                        :join [[:meme-tokens :mt] [:= :mt.meme-token/token-id :ma.meme-auction/token-id]
                               [:memes :m] [:= :mt.reg-entry/address :m.reg-entry/address]]
                        :left-join [[:meme-tags :mtags] [:= :mtags.reg-entry/address :m.reg-entry/address]]}
                 title         (sqlh/merge-where [:= :m.meme/title title])
                 seller        (sqlh/merge-where [:= :ma.meme-auction/seller seller])
                 tags          (sqlh/merge-where [:=
                                                  (count tags)
                                                  {:select [(sql/call :count :*)]
                                                   :from [[:meme-tags :mtts]]
                                                   :where [:and
                                                                     [:= :mtts.reg-entry/address :m.reg-entry/address]
                                                                     [:in :mtts.tag/name tags]]}])
                 tags-or      (sqlh/merge-where [:in :mtags.tag/name tags-or])
                 statuses-set (sqlh/merge-where [:in (meme-auction-status-sql-clause now) statuses-set])
                 order-by     (sqlh/merge-order-by [[(get {:meme-auctions.order-by/price      :ma.meme-auction/bought-for
                                                           :meme-auctions.order-by/started-on :ma.meme-auction/started-on
                                                           :meme-auctions.order-by/bought-on  :ma.meme-auction/bought-on
                                                           :meme-auctions.order-by/token-id   :ma.meme-auction/token-id}
                                                          ;; TODO: move this transformation to district-server-graphql
                                                          (graphql-utils/gql-name->kw order-by))
                                                     (or (keyword order-dir) :asc)]])
                 group-by     (merge {:group-by [:m.reg-entry/address]}))]
     (paged-query query page-size page-start-idx))))

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

(defn search-param-changes-query-resolver [_ {:keys [:first :after] :as args}]
  (log/debug "search-param-changes args" args)
  (try-catch-throw
   (paged-query {:select [:*]
                 :from [:param-changes]
                 :join [:reg-entries [:= :reg-entries.reg-entry/address :param-changes.reg-entry/address]]}
                first
                (when after
                  (js/parseInt after)))))

(defn user-query-resolver [_ {:keys [:user/address] :as args} context debug]
  (log/debug "user args" args)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:users]
                            :where [:= address :users.user/address]})]
     (log/debug "user query" sql-query)
     (when-not (empty? sql-query)
       sql-query))))

(defn search-users-query-resolver [_ {:keys [:order-by :order-dir :first :after] :as args} _ document]
  (log/debug "search-users-query-resolver args" args)
  (try-catch-throw
   (let [now (last-block-timestamp)
         order-by-clause (when order-by (get {:users.order-by/address :user/address
                                              :users.order-by/voter-total-earned :user/voter-total-earned
                                              :users.order-by/challenger-total-earned :user/challenger-total-earned
                                              :users.order-by/curator-total-earned :user/curator-total-earned
                                              :users.order-by/total-participated-votes-success :user/total-participated-votes-success
                                              :users.order-by/total-participated-votes :user/total-participated-votes
                                              :users.order-by/total-created-challenges-success :user/total-created-challenges-success
                                              :users.order-by/total-created-challenges :user/total-created-challenges
                                              :users.order-by/total-collected-memes :user/total-collected-memes
                                              :users.order-by/total-collected-token-ids :user/total-collected-token-ids
                                              :users.order-by/total-created-memes-whitelisted :user/total-created-memes-whitelisted
                                              :users.order-by/total-created-memes :user/total-created-memes}
                                             (graphql-utils/gql-name->kw order-by)))
         fields (conj (query-fields document :items) order-by-clause)
         select? (fn [c] (contains? fields c))
         query (paged-query {:select (remove nil?
                                             [:*
                                              (when (select? :user/curator-total-earned)
                                                [[:+ :users.user/voter-total-earned :users.user/challenger-total-earned] :user/curator-total-earned])
                                              (when (select? :user/total-participated-votes-success)
                                                [{:select [:%count.*]
                                                  :from [:votes]
                                                  :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
                                                  :where [:and [:> now :reg-entries.challenge/reveal-period-end]
                                                          [:> :reg-entries.challenge/votes-for :reg-entries.challenge/votes-against]
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
                                                          [:> :reg-entries.challenge/votes-for :reg-entries.challenge/votes-against]
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
                                                  :join [:reg-entries [:= :reg-entries.reg-entry/address :memes.reg-entry/address]]
                                                  :where [:and [:= :reg-entries.reg-entry/creator :user/address]
                                                          [:or
                                                           [:< :reg-entries.challenge/votes-against :reg-entries.challenge/votes-for]
                                                           [:< :reg-entries.reg-entry/challenge-period-end now]]]}
                                                 :user/total-created-memes-whitelisted])
                                              (when (select? :user/total-created-memes)
                                                [{:select [:%count.* ]
                                                  :from [:memes]
                                                  :join [:reg-entries [:= :reg-entries.reg-entry/address :memes.reg-entry/address]]
                                                  :where [:= :reg-entries.reg-entry/creator :user/address]}
                                                 :user/total-created-memes])])
                             :from [:users]
                             :order-by [[order-by-clause (or (keyword order-dir) :asc)]]}
                            first
                            (when after
                              (js/parseInt after)))]
     (log/debug "search-users-query-resolver query" query)
     query)))

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
  (try-catch-throw
   (let [sql-query (db/all {:select [[:param-change/db :param/db]
                                     [:param-change/key :param/key]
                                     [:param-change/value :param/value] ]
                            :from [:param-changes]
                            :where [:and [:= db :param-changes.param-change/db]
                                    [:in :param-changes.param-change/key keys]]
                            :order-by [:param-changes.param-change/applied-on]})]
     (log/debug "params-query-resolver" sql-query)
     sql-query)))

(defn vote->option-resolver [{:keys [:vote/option] :as vote}]
  (cond
    (= 1 option)
    (enum :vote-option/vote-for)

    (= 0 option)
    (enum :vote-option/vote-against)

    :else (enum :vote-option/no-vote)))

(defn reg-entry->status-resolver [reg-entry]
  (enum (reg-entry-status (last-block-timestamp) reg-entry)))

(defn reg-entry->creator-resolver [{:keys [:reg-entry/creator] :as reg-entry}]
  (log/debug "reg-entry->creator-resolver args" reg-entry)
  {:user/address creator})

(defn reg-entry->challenger [{:keys [:challenge/challenger] :as reg-entry}]
  (log/debug "reg-entry->challenger-resolver args" reg-entry)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:users]
                            :where [:= challenger :users.user/address]})]
     (log/debug "reg-entry->challenger-resolver query" sql-query)
     (when challenger
       sql-query))))

(defn reg-entry->votes-total-resolver [{:keys [:challenge/votes-against :challenge/votes-for] :as reg-entry}]
  (log/debug "challenge->votes-total-resolver args" reg-entry)
  (+ votes-against votes-for))

(defn vote->reward-resolver [{:keys [:reg-entry/address :challenge/reward-pool :vote/option] :as vote}]
  (log/debug "vote->reward-resolver args" vote)
  (try-catch-throw
   (let [now (last-block-timestamp)
         status (reg-entry-status now vote)
         {:keys [:votes/for :votes/against] :as sql-query} (db/get {:select [[{:select [:%count.*]
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
            (= option 0))
       (/ reward-pool against)

       :else nil))))

(defn reg-entry->vote [{:keys [:reg-entry/address] :as reg-entry} {:keys [:vote/voter]}]
  (log/debug "reg-entry->vote args" {:reg-entry reg-entry :voter voter} )
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:votes]
                            :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
                            :where [:= voter :votes.vote/voter]})]
     (log/debug "reg-entry->vote query" sql-query)
     sql-query)))

(defn meme->owned-meme-tokens [{:keys [:reg-entry/address] :as meme} {:keys [:owner] :as args}]
  (try-catch-throw
   (let [query (merge {:select [:mto.* :mt.*]
                       :modifiers [:distinct]
                       :from [[:meme-tokens :mt]]
                       :join [[:reg-entries :re] [:= :mt.reg-entry/address :re.reg-entry/address]
                              [:meme-auctions :ma] [:= :ma.meme-auction/token-id :mt.meme-token/token-id]]
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
  (log/debug "meme-auction->buyer-resolver args" bought-on)
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

    (and (nil? bought-on) (< (last-block-timestamp) (+ started-on duration)))
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
  (try-catch-throw
   (if total-created-memes-whitelisted
     total-created-memes-whitelisted
     (when address
       (let [sql-query (when address
                         (db/all {:select [:*]
                                  :from [:memes]
                                  :join [:reg-entries [:= :reg-entries.reg-entry/address :memes.reg-entry/address]]
                                  :where [:= address :reg-entries.reg-entry/creator]}))]
         (log/debug "user->total-created-memes-whitelisted-resolver query" sql-query)
         (count (filter (fn [e] (= :reg-entry.status/whitelisted (reg-entry-status (last-block-timestamp) e)))
                        sql-query)))))))

(defn user->creator-largest-sale-resolver
  "Largest sale creator has done with his newly minted meme"
  [{:keys [:user/address] :as user}]
  (log/debug "user->creator-largest-sale-resolver args" user)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-auctions]
                            :join [:reg-entries [:= address :reg-entries.reg-entry/creator]
                                   :memes [:= :memes.reg-entry/address :reg-entries.reg-entry/address]]
                            :where [:and [:= {:select [(sql/call :max :meme-auctions.meme-auction/end-price)]
                                              :from [:meme-auctions]}
                                          :meme-auctions.meme-auction/end-price]
                                    [:= address :meme-auction/seller]]})]
     (log/debug "user->creator-largest-sale-resolver query" sql-query)
     sql-query)))

(defn user->largest-sale-resolver [{:keys [:user/address] :as user}]
  (log/debug "user->largest-sale-resolver args" user)
  (try-catch-throw
   (let [sql-query (db/get {:select [:*]
                            :from [:meme-auctions]
                            :where [:and [:= {:select [(sql/call :max :meme-auctions.meme-auction/end-price)]
                                              :from [:meme-auctions]}
                                          :meme-auctions.meme-auction/end-price]
                                    [:= address :meme-auction/seller]]})]
     (log/debug "user->largest-sale-resolver query" sql-query)
     sql-query)))

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
                            :where [:and [:= {:select [(sql/call :max :meme-auctions.meme-auction/end-price)]
                                              :from [:meme-auctions]}
                                          :meme-auctions.meme-auction/end-price]
                                    [:= address :meme-auction/buyer]]})
         {:keys [:meme-auction/buyer]} sql-query]
     (log/debug "user->largest-buy-resolver query" sql-query)
     (when buyer
       sql-query))))

(defn user->total-created-challenges-resolver
  [{:keys [:user/address
           :user/total-created-challenges] :as user}]
  (log/debug "user->total-created-challenges-resolver args" user)
  (try-catch-throw
   (if total-created-challenges
     total-created-challenges
     (let [sql-query (when address
                       (db/get {:select [[:%count.* :user/total-created-challenges]]
                                :from [:reg-entries]
                                :where [:= address :reg-entries.challenge/challenger]}))]
       (log/debug "user->total-created-challenges-resolver query" sql-query)
       (:user/total-created-challenges sql-query)))))

(defn user->total-created-challenges-success-resolver
  [{:keys [:user/address :user/total-created-challenges-success] :as user}]
  (log/debug "user->total-created-challenges-success-resolver args" user)
  (try-catch-throw
   (if total-created-challenges-success
     total-created-challenges-success
     (let [sql-query (when address
                       (db/get {:select [[:%count.* :user/total-created-challenges-success]]
                                :from [:reg-entries]
                                :where [:and [:> (last-block-timestamp) :reg-entries.challenge/reveal-period-end]
                                        [:> :reg-entries.challenge/votes-for :reg-entries.challenge/votes-against]
                                        [:= address :reg-entries.challenge/challenger]]}))]
       (log/debug "user->total-created-challenges-success-resolver query" sql-query)
       (:user/total-created-challenges-success sql-query)))))

(defn user->total-participated-votes-resolver
  "Amount of different votes user participated in"
  [{:keys [:user/address
           :user/total-participated-votes] :as user}]
  (log/debug "user->total-participated-votes-resolver args" user)
  (try-catch-throw
   (if total-participated-votes
     total-participated-votes
     (let [sql-query (when address
                       (db/get {:select [[:%count.* :user/total-participated-votes]]
                                :from [:votes]
                                :where [:= address :votes.vote/voter]}))]
       (log/debug "user->total-participated-votes-resolver query" sql-query)
       (:user/total-participated-votes sql-query)))))

(defn user->total-participated-votes-success-resolver
  "Amount of different votes user voted for winning option"
  [{:keys [:user/address :user/total-participated-votes-success] :as user}]
  (log/debug "user->total-participated-votes-success-resolver args" user)
  (try-catch-throw
   (if total-participated-votes-success
     total-participated-votes-success
     (let [now (last-block-timestamp)
           sql-query (when address
                       (db/get {:select [[:%count.* :user/total-participated-votes-success]]
                                :from [:votes]
                                :join [:reg-entries [:= :reg-entries.reg-entry/address :votes.reg-entry/address]]
                                :where [:and [:> now :reg-entries.challenge/reveal-period-end]
                                        [:> :reg-entries.challenge/votes-for :reg-entries.challenge/votes-against]
                                        [:= address :votes.vote/voter]]}))]
       (log/debug "user->total-participated-votes-success-resolver query" sql-query)
       (:user/total-participated-votes-success sql-query)))))

(defn user->curator-total-earned-resolver
  [{:keys [:user/voter-total-earned
           :user/challenger-total-earned
           :user/curator-total-earned] :as user} parent]
  (log/debug "user->curator-total-earned-resolver args" user)
  (try-catch-throw
   (if curator-total-earned
     curator-total-earned
     (when (and voter-total-earned challenger-total-earned)
       (+ voter-total-earned challenger-total-earned)))))

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
           :params params-query-resolver}
   :Vote {:vote/option vote->option-resolver
          :vote/reward vote->reward-resolver}
   :Meme {:reg-entry/status reg-entry->status-resolver
          :reg-entry/creator reg-entry->creator-resolver
          :challenge/challenger reg-entry->challenger
          :challenge/vote reg-entry->vote
          :meme/owned-meme-tokens meme->owned-meme-tokens
          :meme/tags meme->tags}
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
   :ParamChange {:reg-entry/status reg-entry->status-resolver
                 :reg-entry/creator reg-entry->creator-resolver
                 :challenge/challenger reg-entry->challenger
                 :challenge/votes-total reg-entry->votes-total-resolver
                 :challenge/vote reg-entry->vote}
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
          :user/curator-total-earned user->curator-total-earned-resolver}
   :UserList {:items user-list->items-resolver}})

