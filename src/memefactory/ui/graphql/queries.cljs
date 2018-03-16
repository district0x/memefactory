(ns memefactory.ui.graphql.queries
  (:require [district.cljs-utils :as cljs-utils]))

(def db-key :graphql)

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

(defn merge-results [db results]
  (update db db-key cljs-utils/merge-in results))

(defn query-results-keys [db]
  (keys (get-in db [db-key :query-results])))

(defn graphql [db]
  (get db db-key))