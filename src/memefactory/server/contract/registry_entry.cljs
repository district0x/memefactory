(ns memefactory.server.contract.registry-entry
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.eth :as web3-eth]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [memefactory.server.contract.dank-token :as dank-token]))

(defn registry [contract-key]
  (contract-call contract-key :registry))

(defn status
  [contract-key]
  (contract-call contract-key :status))

(defn create-challenge [contract-addr {:keys [:meta-hash]} & [opts]]
  (contract-call (instance :meme contract-addr) :create-challenge meta-hash (merge opts {:gas 1200000})))

(defn create-challenge-data [contract-addr {:keys [:meta-hash]}]
  (web3-eth/contract-get-data (instance :meme contract-addr) :create-challenge meta-hash))

(defn approve-and-create-challenge [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount amount
                                :extra-data (create-challenge-data contract-addr args)}
                               (merge opts {:gas 1200000})))
