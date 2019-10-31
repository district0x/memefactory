(ns memefactory.server.contract.meme
  (:require [district.server.smart-contracts :as smart-contracts]
            ;; [district.shared.async-helpers :refer [promise->]]
            [memefactory.shared.contract.meme :refer [parse-load-meme]]))

(defn mint [contract-addr & [amount opts]]
  (smart-contracts/contract-send [:meme contract-addr] :mint [(or amount 0)] (merge {:gas 6000000} opts)))

;; (defn load-meme [contract-addr]
;;   (promise-> (contract-call (instance :meme contract-addr) :load-meme)
;;              #(parse-load-meme contract-addr %)))

;; (defn transfer-deposit [contract-addr & [opts]]
;;   (contract-call (instance :meme contract-addr) :transfer-deposit [] (merge {:gas 300000} opts)))
