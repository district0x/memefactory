(ns memefactory.server.contract.meme
  (:require [district.server.smart-contracts :refer [contract-call instance contract-address]]
            [memefactory.server.macros :refer [promise->]]
            [memefactory.shared.contract.meme :refer [parse-load-meme]]))

(defn mint [contract-addr & [amount opts]]
  (contract-call [:meme contract-addr] :mint [(or amount 0)] (merge {:gas 6000000} opts)))

(defn load-meme [contract-addr]
  (promise-> (contract-call (instance :meme contract-addr) :load-meme)
             #(parse-load-meme contract-addr %)))

(defn transfer-deposit [contract-addr & [opts]]
  (contract-call (instance :meme contract-addr) :transfer-deposit [] (merge {:gas 300000} opts)))
