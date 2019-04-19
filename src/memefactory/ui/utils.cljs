(ns memefactory.ui.utils
  (:require
    [bignumber.core :as bn]
    [cemerick.url :as url]
    [cljs-time.coerce :as time-coerce]
    [cljs-web3.core :as web3]
    [district.format :as format]
    [district.ui.router.utils :as router-utils]
    [clojure.string :as str]))

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

(defn copy-to-clipboard [s]
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

(defn build-order-by [prefix order-by]
  (keyword (str
            (cljs.core/name prefix)
            ".order-by")
           ;; HACK: this nasty hack is because our select form component doesn't support two options with the same key
           ;; for auctions we want to sort by price asc and desc, so we create price-asc and price-desc
           ;; and remove it here
           (if (str/starts-with? (name order-by) "price")
             "price"
             (name order-by))))
