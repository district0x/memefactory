(ns memefactory.server.contract.ds-auth
  (:require
    [district.server.smart-contracts :refer [contract-call instance contract-address]]))

(defn owner [contract-key]
  (contract-call contract-key :owner))

(defn set-owner [contract-key new-owner & [opts]]
  (contract-call contract-key :set-owner new-owner (merge opts {:gas 100000})))

(defn set-authority [contract-key new-authority & [opts]]
  (contract-call contract-key :set-authority new-authority (merge opts {:gas 100000})))