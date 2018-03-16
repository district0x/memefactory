(ns memefactory.ui.graphql.events
  (:require
    [cljsjs.apollo-fetch]
    [memefactory.shared.graphql-mock-resolvers :refer [graphql-mock-resolvers]]
    [memefactory.shared.graphql-utils :as graphql-utils]
    [memefactory.ui.graphql.effects :as effects]
    [memefactory.ui.graphql.queries :as queries]
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
             (queries/assoc-id-field id-field))
       })))

(reg-event-fx
  ::query
  interceptors
  (fn [{:keys [:db]} [{:keys [:query :variables :on-success :on-error :on-response]}]]
    (let [query-ast (parse-graphql (v/graphql-query query))
          query (-> query-ast
                  (graphql-utils/add-field-to-query (graphql-utils/clj->graphql (queries/typename-field db)))
                  (graphql-utils/add-field-to-query (graphql-utils/clj->graphql (queries/id-field db)))
                  print-str-graphql)]

      #_(print.foo/look query)

      #_(print.foo/look (graphql-utils/query-ast->query-clj (parse-graphql query)
                                                            {:transform-name-fn graphql-utils/graphql->clj
                                                             :variables variables}))

      #_(print.foo/look (graphql-utils/transform-response
                          (gql-sync (queries/schema db) query (fn [& args]
                                                                (println "heyu")))))


      {}
      {::effects/query {:schema (queries/schema db)
                        :fetcher (queries/fetcher db)
                        :query query
                        :query-ast query-ast
                        :variables variables
                        :on-success [::query-success* {:on-success on-success}]
                        :on-error [::query-error* {:on-error on-error}]
                        :on-response on-response}})))


(reg-event-fx
  ::store-response
  interceptors
  (fn [{:keys [:db]} [response {:keys [:query :query-ast :variables] :as opts}]]
    (let [query-ast (if-not query-ast (parse-graphql query) query-ast)
          query-clj (graphql-utils/query-ast->query-clj query-ast {:transform-name-fn graphql-utils/graphql->clj
                                                                   :variables variables})
          results (print.foo/look (graphql-utils/normalize-response response
                                                                    (print.foo/look query-clj)
                                                                    {:typename-field (queries/typename-field db)
                                                                     :id-field (queries/id-field db)}))]
      {:db (queries/merge-results db results)
       :dispatch [::print-query opts]})))


(reg-event-fx
  ::print-query
  interceptors
  (fn [{:keys [:db]} [{:keys [:query :variables]}]]
    (print.foo/look (graphql-utils/transform-response
                      (gql-sync (queries/schema db)
                                query
                                (queries/graphql db)
                                #_(graphql-utils/create-resolver (queries/graphql db)
                                                                 {:transform-name-fn graphql-utils/graphql->clj})
                                {}
                                variables
                                nil
                                (graphql-utils/create-field-resolver
                                  {:transform-name-fn graphql-utils/graphql->clj}))))
    nil))


(reg-event-fx
  ::query-success*
  interceptors
  (fn [{:keys [:db]} [{:keys [:on-success]} {:keys [:data]} opts]]
    (merge
      {:dispatch-n [[::store-response data opts]]}
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