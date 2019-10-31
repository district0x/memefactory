(ns cljs-web3.utils
  (:require [cljs-web3.api :as api]
            [camel-snake-kebab.core :as camel-snake]
            [camel-snake-kebab.extras :as camel-snake-extras]
            [clojure.string :as string]))

(defn sha3 [{:keys [:instance :provider]} args]
  (api/-sha3 instance provider args))

(defn solidity-sha3 [{:keys [:instance :provider]} arg & args]
  (api/-solidity-sha3 instance provider [arg args]))

(defn from-ascii [{:keys [:instance :provider]} args]
  (api/-from-ascii instance provider args))

(defn to-ascii [{:keys [:instance :provider]} args]
  (api/-to-ascii instance provider args))

(defn number-to-hex [{:keys [:instance :provider]} number]
  (api/-number-to-hex instance provider number))

(defn from-wei [{:keys [:instance :provider]} number & [unit]]
  (api/-from-wei instance provider number unit))

(defn to-wei [{:keys [:instance :provider]} number & [unit]]
  (api/-to-wei instance provider number unit))

(defn address? [{:keys [:instance :provider]} address]
  (api/-address? instance provider address))

;; web3.utils.hexToNumberString

;; TODO move non api methods to helpers ns

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
