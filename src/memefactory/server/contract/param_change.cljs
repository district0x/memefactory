(ns memefactory.server.contract.param-change
  (:require [district.shared.async-helpers :refer [promise->]]
            [district.server.smart-contracts :as smart-contracts]))

(defn load-param-change [contract-addr]
  (promise-> (smart-contracts/contract-call (instance :param-change contract-addr) :load-param-change)
             (fn [param-change]
               {:reg-entry/address contract-addr
                :db (aget param-change "0")
                :key (aget param-change "1")
                :value (aget param-change "2")
                :meta-hash (aget param-change "3")})))
