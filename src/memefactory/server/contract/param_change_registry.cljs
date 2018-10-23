(ns memefactory.server.contract.param-change-registry
  (:require
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [memefactory.server.contract.registry :as registry]))

#_(def registry-entry-event (partial registry/registry-entry-event [:param-change-registry :param-change-registry-fwd]))
#_(def registry-entry-event-in-tx (partial registry/registry-entry-event-in-tx [:param-change-registry :param-change-registry-fwd]))

(defn apply-param-change [param-change-address & [opts]]
  (contract-call [:param-change-registry :param-change-registry-fwd] :apply-param-change param-change-address (merge {:gas 700000} opts)))
