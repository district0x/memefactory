(ns memefactory.server.contract.meme-token
  (:require [district.server.smart-contracts :as smart-contracts]
            [memefactory.server.contract.meme-auction :as meme-auction]))

;; (defn total-supply []
;;   (contract-call :meme-token :total-supply))

;; (defn token-uri [token-id]
;;   (contract-call :meme-token token-id :token-uri))

(defn owner-of [token-id]
  (smart-contracts/contract-call :meme-token :owner-of [token-id]))

;; (defn safe-transfer-from [{:keys [:from :to :token-id]} & [opts]]
;;   (contract-call :meme-token :safe-transfer-from [from to token-id] (merge {:gas 3000000} opts)))

(defn safe-transfer-from-multi [{:keys [:from :to :token-ids :data] :as args} & [opts]]
  (smart-contracts/contract-send :meme-token :safe-transfer-from-multi [from to token-ids data] (merge {:gas 6000000} opts)))

(defn transfer-multi-and-start-auction [{:keys [:from :token-ids :start-price :end-price :duration :description] :as params} & [opts]]
  (safe-transfer-from-multi {:from from
                             :to (smart-contracts/contract-address :meme-auction-factory-fwd)
                             :token-ids token-ids
                             :data (meme-auction/start-auction-data (select-keys params [:start-price :end-price :duration :description]))}
                            opts))

;; (defn meme-token-transfer-event [opts on-event]
;;   (create-event-filter :meme-token :Transfer {} opts on-event))
