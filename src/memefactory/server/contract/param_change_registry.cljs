(ns memefactory.server.contract.param-change-registry
  (:require [district.server.smart-contracts :refer [contract-call instance contract-address]]
            [memefactory.server.contract.registry :as registry]))

(defn apply-param-change [param-change-address & [opts]]
  (contract-call [:param-change-registry :param-change-registry-fwd] :apply-param-change [param-change-address] (merge {:gas 700000} opts)))
