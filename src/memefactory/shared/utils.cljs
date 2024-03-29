(ns memefactory.shared.utils
  (:require [bignumber.core :as bn]
            [cljs-time.coerce :as time-coerce]
            [cljs-time.core :as time]
            [cljs.core.match :refer-macros [match]]
            [cljsjs.filesaverjs]
            [district.graphql-utils :as gql-utils])
  (:import goog.async.Debouncer))

;; started-on, duration and now are expected in seconds
(defn calculate-meme-auction-price [{:keys [:meme-auction/start-price
                                            :meme-auction/end-price
                                            :meme-auction/duration
                                            :meme-auction/started-on]}
                                    now]
  (let [seconds-passed (- now started-on)
        total-price-change (- start-price end-price)
        current-price-change (quot (* total-price-change seconds-passed) duration)]
    (if (<= duration seconds-passed)
      end-price
      (- start-price current-price-change))))

(defn parse-uint
  "parse uint treating 0 as nil"
  [uint]
  (let [uint (bn/number uint)]
    (if (= 0 uint) nil uint)))

(defn web3-time->date-time [x]
  (let [x (bn/number x)]
    (when (pos? x)
      (time-coerce/from-long (* x 1000)))))

(defn web3-time->local-date-time [x]
  (when-let [dt (web3-time->date-time x)]
    (time/to-default-time-zone dt)))

(defn parse-uint-date [date parse-as-date?]
  (let [date (bn/number date)]
    (match [(= 0 date) parse-as-date?]
           [true _] nil
           [false true] (web3-time->local-date-time date)
           [false (:or nil false)] date)))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn reg-entry-status [now {:keys [:reg-entry/challenge-period-end :challenge/challenger
                                    :challenge/commit-period-end :challenge/reveal-period-end
                                    :challenge/votes-for :challenge/votes-against]}]
  (cond
    (and (< now challenge-period-end) (not challenger)) :reg-entry.status/challenge-period
    (< now commit-period-end)                           :reg-entry.status/commit-period
    (< now reveal-period-end)                           :reg-entry.status/reveal-period
    (and (pos? reveal-period-end)
         (> now reveal-period-end)) (if (< votes-against votes-for)
                                      :reg-entry.status/whitelisted
                                      :reg-entry.status/blacklisted)
    :else :reg-entry.status/whitelisted))

(defn file-write [filename content & [mime-type]]
  (js/saveAs (new js/Blob
                  (clj->js [content])
                  (clj->js {:type (or mime-type (str "application/json;charset=UTF-8"))}))
             filename))

(defn second-date-keys
  "Convert given map keys from date to seconds since epoch"
  [m ks]
  (reduce (fn [r k]
            (update r k #(let [r (when-let [d (gql-utils/gql-date->date %)]
                                   (quot (.getTime d) 1000))]
                           r)))
   m
   ks))

(defn reg-entry-dates-to-seconds [entry]
  (second-date-keys entry #{:reg-entry/created-on :reg-entry/challenge-period-end
                            :challenge/commit-period-end :challenge/reveal-period-end}))

(def tweet-url-regex #"(?:https?://)?(?:www\.)?twitter\.com/@?(?:[a-zA-Z0-9_]{1,15})/?status/([0-9]+)/?(?:\?.+)?")
