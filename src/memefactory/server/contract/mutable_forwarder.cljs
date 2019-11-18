(ns memefactory.server.contract.mutable-forwarder
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn set-target [contract-key target & [opts]]
  (smart-contracts/contract-send contract-key :set-target [target] (merge {:ignore-forward? true :gas 100000} opts)))

(defn target [contract-key]
  (smart-contracts/contract-call contract-key :target [] {:ignore-forward? true}))
