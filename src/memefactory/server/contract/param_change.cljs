(ns memefactory.server.contract.param-change
  (:require
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [memefactory.shared.contract.param-change :refer [parse-load-param-change]]))

(defn load-param-change [contract-addr]
  (parse-load-param-change
    contract-addr
    (contract-call (instance :param-change contract-addr) :load-param-change)))

