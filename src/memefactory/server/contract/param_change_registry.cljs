(ns memefactory.server.contract.param-change-registry
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn apply-param-change [param-change-address & [opts]]
  (smart-contracts/contract-send [:param-change-registry :param-change-registry-fwd] :apply-param-change [param-change-address] (merge {:gas 700000} opts)))
