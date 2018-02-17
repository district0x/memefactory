(ns memefactory.server.contract.registry
  (:require [district.server.smart-contracts :refer [contract-call]]))

(defn db [contract-key]
  (contract-call contract-key :db))

(defn set-factory [contract-key {:keys [:factory :factory?]} & [opts]]
  (contract-call contract-key :set-factory factory factory? (merge opts {:gas 100000})))
