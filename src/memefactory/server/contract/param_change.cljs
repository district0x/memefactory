(ns memefactory.server.contract.param-change
  (:require [memefactory.server.macros :refer [promise->]]
            [memefactory.shared.contract.param-change :refer [parse-load-param-change]]
            [district.server.smart-contracts :refer [contract-call instance contract-address]]))

(defn load-param-change [contract-addr]
  (promise-> (contract-call (instance :param-change contract-addr) :load-param-change)
             #(parse-load-param-change contract-addr %)))
