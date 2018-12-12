(ns memefactory.server.contract.ownable
  (:require [district.server.smart-contracts :refer [contract-call]]))

(defn owner [contract-key]
  #_(contract-call contract-key :owner))

(defn transfer-ownership [contract-key {:keys [:new-owner]} & [opts]]
  #_(contract-call contract-key :transfer-ownership new-owner (merge {:gas 100000} opts)))
