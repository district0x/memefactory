(ns memefactory.ui.graphql.events
  (:require
    [cljsjs.apollo-fetch]
    [memefactory.shared.graphql-mock-resolvers :refer [graphql-mock-resolvers]]
    [memefactory.shared.graphql-utils :as graphql-utils]
    [memefactory.ui.graphql.effects :as effects]
    [memefactory.ui.graphql.queries :as queries]
    [memefactory.ui.graphql.utils :as utils]
    [print.foo :include-macros true]
    [re-frame.core :refer [reg-event-fx trim-v]]
    [venia.core :as v]))

(def interceptors [trim-v])
(def build-schema (aget js/GraphQL "buildSchema"))
(def parse-graphql (aget js/GraphQL "parse"))
(def print-str-graphql (aget js/GraphQL "print"))
(def gql (aget js/GraphQL "graphql"))
(def gql-sync (aget js/GraphQL "graphqlSync"))

(reg-event-fx
  ::start
  interceptors
  (fn [{:keys [:db]} [{:keys [:schema :url :id-field :typename-field]
                       :or {id-field :id typename-field :__typename}}]]
    (let [fetcher (js/apolloFetch.createApolloFetch (clj->js {:uri url}))]
      {:db (-> db
             (queries/assoc-schema (build-schema schema))
             (queries/assoc-fetcher fetcher)
             (queries/assoc-typename-field typename-field)
             (queries/assoc-id-field id-field)
             (queries/assoc-dataloader (utils/create-dataloader {:on-success [::normalize-response]
                                                                 :transform-name-fn graphql-utils/graphql->clj})))})))


(reg-event-fx
  ::query
  interceptors
  (fn [{:keys [:db]} [{:keys [:query :variables :on-success :on-error :on-response :enqueue?]}]]
    (let [query-ast (cond
                      (map? query) (parse-graphql (v/graphql-query query))
                      (string? query) (parse-graphql query)
                      :else query)
          query (-> query-ast
                  (graphql-utils/add-field-to-query (graphql-utils/clj->graphql (queries/typename-field db)))
                  (graphql-utils/add-field-to-query (graphql-utils/clj->graphql (queries/id-field db)))
                  print-str-graphql)]

      {(if enqueue? ::effects/enqueue-query ::effects/query)
       {:schema (queries/schema db)
        :fetcher (queries/fetcher db)
        :dataloader (queries/dataloader db)
        :query query
        :query-ast query-ast
        :variables variables
        :on-success [::query-success* {:on-success on-success}]
        :on-error [::query-error* {:on-error on-error}]
        :on-response on-response}})))


(reg-event-fx
  ::normalize-response
  interceptors
  (fn [{:keys [:db]} [response {:keys [:query :query-ast :query-clj :variables]}]]
    (let [query-clj (if-not query-clj
                      (graphql-utils/query-ast->query-clj query-ast
                                                          {:transform-name-fn graphql-utils/graphql->clj
                                                           :variables variables})
                      query-clj)
          results (graphql-utils/normalize-response response
                                                    query-clj
                                                    {:typename-field (queries/typename-field db)
                                                     :id-field (queries/id-field db)})]
      {:db (print.foo/look (queries/merge-results db results))})))


(reg-event-fx
  ::query-success*
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-success]} {:keys [:data]} opts]]
    (merge
      {:dispatch-n [[::normalize-response data opts]]}
      (when on-success
        {:dispatch (vec (concat on-success [data opts]))}))))


(reg-event-fx
  ::query-error*
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-error]} {:keys [:errors]} opts]]
    (when on-error
      {:dispatch (vec (concat on-error [errors opts]))})))


(reg-event-fx
  ::stop
  interceptors
  (fn [{:keys [:db]}]
    ))