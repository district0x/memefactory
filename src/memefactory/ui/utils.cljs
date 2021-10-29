(ns memefactory.ui.utils
  (:require [bignumber.core :as bn]
            [cljs-time.core :as t]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [clojure.string :as string]
            [district.format :as format]
            [district.ui.now.subs]
            [memefactory.ui.config :refer [config]]
            [reagent.ratom :refer [reaction]]))

(defn format-price [price]
  (format/format-token (bn/number (web3/from-wei price :ether)) {:max-fraction-digits 3
                                                                 :token "MATIC"
                                                                 :min-fraction-digits 2}))


(defn format-dank [dank]
  (format/format-token (bn/number (web3/from-wei dank :ether)) {:max-fraction-digits 2
                                                                :token "DANK"
                                                                :min-fraction-digits 0}))

(defn copy-to-clipboard! [s]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) s)
    (.setAttribute el "readonly" "")
    (set! (-> el .-style .-position) "absolute")
    (set! (-> el .-style .-left) "-9999px")
    (js/document.body.appendChild el)
    (.select el)
    (js/document.execCommand "copy")
    (js/document.body.removeChild el)))


(defn order-dir-from-key-name [key order-vec]
  (some #(when (= (-> % :key name keyword) (keyword key))
           (:order-dir %))
        order-vec))

(defn parse-ipfs-response [s]
  (->> s
       (string/split-lines)
       (mapv #(.parse js/JSON %))
       (mapv #(js->clj % :keywordize-keys true))))

(defn build-order-by [prefix order-by]
  (keyword (str
            (cljs.core/name prefix)
            ".order-by")
           ;; HACK: this nasty hack is because our select form component doesn't support two options with the same key
           ;; for auctions we want to sort by price asc and desc, so we create price-asc and price-desc
           ;; and remove it here
           (if (string/starts-with? (name order-by) "price")
             "price"
             (name order-by))))

(defn now-in-seconds
  "A reaction that returns time since epoch in seconds"
  []
  (let [now-subs (re-frame.core/subscribe [:district.ui.now.subs/now])]
    (reaction (quot (.getTime @now-subs) 1000))))
