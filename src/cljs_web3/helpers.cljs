(ns cljs-web3.helpers
  (:require [bignumber.core :as bn]
            [camel-snake-kebab.core :as camel-snake]
            [camel-snake-kebab.extras :as camel-snake-extras]
            [clojure.string :as string]))

(def zero-address "0x0000000000000000000000000000000000000000")

(defn safe-case [case-f]
  (fn [x]
    (cond-> (subs (name x) 1)
      true (string/replace "_" "*")
      true case-f
      true (string/replace "*" "_")
      true (->> (str (first (name x))))
      (keyword? x) keyword)))

(def camel-case (safe-case camel-snake/->camelCase))
(def kebab-case (safe-case camel-snake/->kebab-case))

(def js->cljk #(js->clj % :keywordize-keys true))

(def js->cljkk
  "From JavaScript to Clojure with kebab-cased keywords."
  (comp (partial camel-snake-extras/transform-keys kebab-case) js->cljk))

(def cljkk->js
  "From Clojure with kebab-cased keywords to JavaScript e.g.
  {:from-block 0 :to-block 'latest'} -> #js {:fromBlock 0, :toBlock 'latest'}"
  (comp clj->js (partial camel-snake-extras/transform-keys camel-case)))

(defn method [{:keys [:name :call :params :input-formatter :output-formatter]
               :as signature}]
  (js->cljkk signature))

(defn event-interface [contract-instance event-key]
  (reduce (fn [_ element]
            (when (= (:name element) (-> event-key camel-case name))
              (reduced element)))
          (js->cljk (aget contract-instance "_jsonInterface") )))

(defn return-values->clj [return-values {:keys [:inputs] :as event-interface}]
  (reduce (fn [res value]
            (let [n (:name value)]
              (assoc res (-> n kebab-case keyword) (aget return-values n))))
          {}
          inputs))

(defn zero-address? [x]
  (or (= x zero-address)
      (= x "0x")))

(defn empty-address? [x]
  (or (empty? x)
      (zero-address? x)))

(defn remove-0x [s]
  (string/replace s #"0x" ""))

(defn prepend-address-zeros [address]
  (let [n (- 42 (count address))]
    (if (pos? n)
      (->> (subs address 2)
           (str (string/join (take n (repeat "0"))))
           (str "0x"))
      address)))

(defn remove-zero-chars [s]
  (string/join (take-while #(< 0 (.charCodeAt % 0)) s)))
