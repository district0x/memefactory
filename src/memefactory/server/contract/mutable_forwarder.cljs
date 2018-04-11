(ns memefactory.server.contract.mutable-forwarder
  (:require [district.server.smart-contracts :refer [contract-call]]))

(defn set-target [contract-key target & [opts]]
  (contract-call contract-key :set-target target (merge {:gas 100000} opts)))

(defn target [contract-key]
  (contract-call contract-key :target))
