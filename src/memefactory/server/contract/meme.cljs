(ns memefactory.server.contract.meme
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn mint [contract-addr & [amount opts]]
  (smart-contracts/contract-send [:meme contract-addr] :mint [(or amount 0)] (merge {:gas 6000000} opts)))
