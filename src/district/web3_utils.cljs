(ns district.web3-utils
  (:require
    [bignumber.core :as bn]
    [cljs-time.coerce :as time-coerce]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3-core]
    [clojure.string :as string]))

;; TODO : move all to web3-cljs.utils

(defn wei->eth [x]
  #_(when x
    (try
      (web3-core/from-wei x :ether)
      (catch :default _
        nil))))

(def wei->eth-number (comp bn/number wei->eth))

(defn eth->wei [x]
  #_(when x
    (try
      (web3-core/to-wei (if (string? x)
                     (string/replace x \, \.)
                     x)
                   :ether)
      (catch :default _
        nil))))

(def eth->wei-number (comp bn/number eth->wei))

(def zero-address "0x0000000000000000000000000000000000000000")

(defn zero-address? [x]
  (or (= x zero-address)
      (= x "0x")))

(defn empty-address? [x]
  (or (empty? x)
      (zero-address? x)))

(defn remove-0x [s]
  (string/replace s #"0x" ""))

(defn web3-time->date-time [x]
  (let [x (bn/number x)]
    (when (pos? x)
      (time-coerce/from-long (* x 1000)))))

(defn web3-time->local-date-time [x]
  (when-let [dt (web3-time->date-time x)]
    (t/to-default-time-zone dt)))

(defn prepend-address-zeros [address]
  (let [n (- 42 (count address))]
    (if (pos? n)
      (->> (subs address 2)
        (str (string/join (take n (repeat "0"))))
        (str "0x"))
      address)))

(defn remove-zero-chars [s]
  (string/join (take-while #(< 0 (.charCodeAt % 0)) s)))

(def bytes32->str nil #_(comp remove-zero-chars web3-core/to-ascii))

(def uint->address nil #_(comp prepend-address-zeros web3-core/from-decimal))
