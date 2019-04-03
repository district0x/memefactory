(ns memefactory.ui.utils
  (:require
    [bignumber.core :as bn]
    [cemerick.url :as url]
    [cljs-time.coerce :as time-coerce]
    [cljs-web3.core :as web3]
    [district.format :as format]
    [district.ui.router.utils :as router-utils]))

(defn gql-date->date
  "parse GraphQL Date type as JS Date object ready to be formatted"
  [gql-date]
  (time-coerce/from-long (* 1000 gql-date)))

(defn format-price [price]
  (format/format-eth (bn/number (web3/from-wei price :ether)) {:max-fraction-digits 2
                                                               :min-fraction-digits 2}))

(defn format-dank [dank]
  (format/format-token (bn/number (web3/from-wei dank :ether)) {:max-fraction-digits 2
                                                                :token "DANK"
                                                                :min-fraction-digits 0}))
