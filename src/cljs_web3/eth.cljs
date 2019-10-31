(ns cljs-web3.eth
  (:require [cljs-web3.api :as api]))

(defn connected? [{:keys [:instance :provider]} & [callback]]
  (api/-connected? instance provider callback))

(defn contract-at [{:keys [:instance :provider]} abi address]
 (api/-contract-at instance provider abi address))

(defn get-transaction-receipt [{:keys [:instance :provider]} tx-hash]
 (api/-get-transaction-receipt instance provider tx-hash))

(defn accounts [{:keys [:instance :provider]}]
  (api/-accounts instance provider))

(defn get-block-number [{:keys [:instance :provider]} & [callback]]
  (api/-get-block-number instance provider callback))

(defn get-block [{:keys [:instance :provider]} block-hash-or-number return-transactions? & [callback]]
  (api/-get-block instance provider block-hash-or-number return-transactions? callback))

(defn encode-abi [{:keys [:instance]} contract-instance method args]
  (api/-encode-abi instance contract-instance method args))

(defn contract-call [{:keys [:instance]} contract-instance method args opts]
  (api/-contract-call instance contract-instance method args opts))

(defn contract-send [{:keys [:instance]} contract-instance method args opts]
  (api/-contract-send instance contract-instance method args opts))

(defn subscribe-events [{:keys [:instance]} contract-instance event opts & [callback]]
  (api/-subscribe-events instance contract-instance event opts callback))

(defn subscribe-logs [{:keys [:instance :provider]} contract-instance opts & [callback]]
  (api/-subscribe-logs instance provider contract-instance opts callback))

(defn decode-log [{:keys [:instance :provider]} abi data topics]
  (api/-decode-log instance provider abi data topics))

(defn unsubscribe [{:keys [:instance]} subscription & [callback]]
  (api/-unsubscribe instance subscription callback))

(defn clear-subscriptions [{:keys [:instance :provider]}]
  (api/-clear-subscriptions instance provider))

(defn get-past-events [{:keys [:instance]} contract-instance event opts & [callback]]
  (api/-get-past-events instance contract-instance event opts callback))

(defn on [{:keys [:instance]} event-emitter evt callback]
  (api/-on instance event-emitter evt callback))
