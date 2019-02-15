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

(defn seconds->days [seconds]
  (js/Math.floor (/ seconds 24 60 60)))

(defn days->seconds [days]
  (* days 24 60 60))
