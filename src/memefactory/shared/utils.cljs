(ns memefactory.shared.utils
  (:require [bignumber.core :as bn]
            [cljs.core.match :refer-macros [match]]
            [district.web3-utils :as web3-utils]
            [print.foo :refer [look] :include-macros true]))

(def not-nil? (complement nil?))

(defn calculate-meme-auction-price [{:keys [:meme-auction/start-price
                                            :meme-auction/end-price
                                            :meme-auction/duration
                                            :meme-auction/started-on] :as auction} now]
  (let [seconds-passed (quot (- now started-on) 1000)
        total-price-change (- start-price end-price)
        current-price-change (/ (* total-price-change seconds-passed) duration)]
    (if (<= duration seconds-passed)
      end-price
      (- start-price current-price-change))))

(defn parse-uint
  "parse uint treating 0 as nil"
  [uint]
  (let [uint (bn/number uint)]
    (if (= 0 uint) nil uint)))

(defn parse-uint-date [date parse-as-date?]
  (let [date (bn/number date)]
    (match [(= 0 date) parse-as-date?]
           [true _] nil
           [false true] (web3-utils/web3-time->local-date-time date)
           [false (:or nil false)] date)))

#_(defn seconds->days [seconds]
  (js/Math.floor (/ seconds 24 60 60)))

#_(defn days->seconds [days]
  (* days 24 60 60))

(defn seconds->days [seconds]
  (/ seconds 86400))

(defn days->seconds [days]
  (* days 86400))

(defn round [num prec]
  (let [rounding (js/Math.pow 10 prec )]
    (/ (js/Math.floor (* num rounding)) rounding)))

(defn deep-merge
  "Merge nested values from left to right.
   Examples:
   (deep-merge {:a {:b {:c 3}}}
               {:a {:b 3}})
   => {:a {:b 3}}"
  ([] nil)
  ([m] m)
  ([m1 m2]
   (reduce-kv (fn [out k v]
                (let [v1 (get out k)]
                  (cond (nil? v1)
                        (assoc out k v)

                        (and (map? v) (map? v1))
                        (assoc out k (deep-merge v1 v))

                        (= v v1)
                        out

                        :else
                        (assoc out k v))))
              m1
              m2))
  ([m1 m2 & ms]
   (apply deep-merge (deep-merge m1 m2) ms)))
