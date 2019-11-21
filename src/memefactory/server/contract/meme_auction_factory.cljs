(ns memefactory.server.contract.meme-auction-factory
  (:require [district.server.smart-contracts :as smart-contracts]))

;; (defn construct [{:keys [:meme-token]} & [opts]]
;;   (contract-call [:meme-auction-factory :meme-auction-factory-fwd] :construct [meme-token] (merge {:gas 200000} opts)))

;; (defn meme-auction-started-event [opts on-event]
;;   (create-event-filter [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent {} opts on-event))

;; (defn meme-auction-buy-event [opts on-event]
;;   (create-event-filter [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionBuyEvent {} opts on-event))

;; (defn meme-auction-canceled-event [opts on-event]
;;   (create-event-filter [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionCanceledEvent {} opts on-event))

(defn meme-auction-started-event-in-tx [tx]
  (smart-contracts/contract-event-in-tx [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent tx))

;; (defn meme-auction-started-events-in-tx [tx-hash & args]
;;   (apply contract-events-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent args))

(defn meme-auction-buy-event-in-tx [tx]
  (smart-contracts/contract-event-in-tx [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionBuyEvent tx))

;; (defn meme-auction-canceled-event-in-tx [tx-hash & args]
;;   (apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionCanceledEvent args))
