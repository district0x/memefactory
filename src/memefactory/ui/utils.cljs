(ns memefactory.ui.utils
  (:require [cljs-time.coerce :as time-coerce]
            [district.ui.router.utils :as router-utils]
            [cemerick.url :as url]
            [district.format :as format]))

(defn gql-date->date
  "parse GraphQL Date type as JS Date object ready to be formatted"
  [gql-date]
  (time-coerce/from-long (* 1000 gql-date)))

(defn path [& args]
  (str "#" (apply router-utils/resolve args)))

(defn path-with-query [path query-params-map]
  (str path "?" (url/map->query query-params-map)))

(defn format-price [price]
  (format/format-eth (/ price 1e18) {:max-fraction-digits 2
                                     :min-fraction-digits 2}))

(defn format-dank [dank]
  (format/format-token (/ dank 1e18) {:max-fraction-digits 2
                                      :token "DANK"
                                      :min-fraction-digits 0}))
