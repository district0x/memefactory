(ns memefactory.server.contract.minime-token
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn approve-and-call [contract-key {:keys [:spender :amount :extra-data]} & [opts]]
  (smart-contracts/contract-send contract-key :approve-and-call [spender amount extra-data] (merge {:gas 4000000} opts)))

(defn balance-of [contract-key owner]
  (smart-contracts/contract-call contract-key :balance-of [owner]))

(defn total-supply [contract-key]
  (smart-contracts/contract-call contract-key :total-supply))

(defn controller [contract-key]
  (smart-contracts/contract-call contract-key :controller))
