(ns memefactory.server.contract.meme
  (:require
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [memefactory.shared.contract.meme :refer [parse-load-meme]]))

(defn buy [contract-addr amount & [opts]]
  (contract-call (instance :meme contract-addr) :buy amount (merge opts {:gas 500000})))

(defn load-meme [contract-addr]
  (parse-load-meme contract-addr (contract-call (instance :meme contract-addr) :load-meme)))

(defn transfer-deposit [contract-addr & [opts]]
  (contract-call (instance :meme contract-addr) :transfer-deposit (merge opts {:gas 300000})))



