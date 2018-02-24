(ns memefactory.server.contract.meme
  (:require
    [district.server.smart-contracts :refer [contract-call instance contract-address]]))

(defn buy [contract-addr amount & [opts]]
  (contract-call (instance :meme contract-addr) :buy amount (merge opts {:gas 500000})))

