(ns memefactory.server.db
  (:require
    [district.server.config :refer [config]]
    [district.server.db :as db]
    [district.server.db.column-types :refer [address not-nil default-nil default-zero default-false sha3-hash primary-key]]
    [honeysql.core :as sql]
    [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} memefactory-db
  :start (start (merge (:memefactory/db @config)
                       (:memefactory/db (mount/args))))
  :stop (stop))

(def some-table-columns
  [[:a :varchar not-nil]])

(def offering-column-names (map first some-table-columns))

(defn- index-name [col-name]
  (keyword (namespace col-name) (str (name col-name) "-index")))


(defn start [opts]
  #_ (db/run! {:create-table [:offerings]
            :with-columns [some-table-columns]})

  #_ (doseq [column (rest offering-column-names)]
    (db/run! {:create-index (index-name column) :on [:offerings column]})))


(defn stop []
  #_ (db/run! {:drop-table [:offering-request/rounds]}))