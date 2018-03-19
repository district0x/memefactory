(ns memefactory.ui.graphql.queries
  (:require [district.cljs-utils :as cljs-utils]
            [memefactory.shared.graphql-utils :as graphql-utils]))

(def gql-sync (aget js/GraphQL "graphqlSync"))
(def db-key :district.ui.graphql)

(defn assoc-schema [db schema]
  (assoc-in db [db-key :schema] schema))

(defn schema [db]
  (get-in db [db-key :schema]))

(defn assoc-fetcher [db fetcher]
  (assoc-in db [db-key :fetcher] fetcher))

(defn fetcher [db]
  (get-in db [db-key :fetcher]))

(defn assoc-id-field [db id-field]
  (assoc-in db [db-key :id-field] id-field))

(defn id-field [db]
  (get-in db [db-key :id-field]))

(defn assoc-typename-field [db typename-field]
  (assoc-in db [db-key :typename-field] typename-field))

(defn typename-field [db]
  (get-in db [db-key :typename-field]))

(defn assoc-dataloader [db dataloader]
  (assoc-in db [db-key :dataloader] dataloader))

(defn dataloader [db]
  (get-in db [db-key :dataloader]))

(defn merge-results [db results]
  (update db db-key cljs-utils/merge-in results))

(defn query-results-keys [db]
  (keys (get-in db [db-key :query-results])))

(defn graphql [db]
  (get db db-key))

(defn query [db query variables]
  (let [{:keys [:data :errors]}
        (-> (gql-sync (schema db)
                      query
                      (graphql db)
                      {}
                      (clj->js variables)
                      nil
                      (graphql-utils/create-field-resolver
                        {:transform-name-fn graphql-utils/graphql->clj}))
          graphql-utils/transform-response)]
    (merge data
           (when errors
             {:graphql/errors (map #(aget % "message") (vec errors))}))))