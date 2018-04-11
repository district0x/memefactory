(ns memefactory.server.contract.minime-token
  (:require
    [bignumber.core :as bn]
    [district.server.smart-contracts :refer [contract-call]]))

(defn approve-and-call [contract-key {:keys [:spender :amount :extra-data]} & [opts]]
  (contract-call contract-key :approve-and-call spender (bn/number amount) extra-data (merge {:gas 4000000} opts)))

(defn approve [contract-key {:keys [:spender :amount]} & [opts]]
  (contract-call contract-key :approve spender (bn/number amount) (merge {:gas 1000000} opts)))

(defn transfer [contract-key {:keys [:to :amount]} & [opts]]
  (contract-call contract-key :transfer to (bn/number amount) (merge {:gas 200000} opts)))

(defn balance-of [contract-key owner]
  (contract-call contract-key :balance-of owner))

(defn total-supply [contract-key]
  (contract-call contract-key :total-supply))

(defn controller [contract-key]
  (contract-call contract-key :controller))
