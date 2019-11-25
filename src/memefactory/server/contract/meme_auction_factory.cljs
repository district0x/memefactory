(ns memefactory.server.contract.meme-auction-factory
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn meme-auction-started-event-in-tx [tx]
  (smart-contracts/contract-event-in-tx [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent tx))

(defn meme-auction-buy-event-in-tx [tx]
  (smart-contracts/contract-event-in-tx [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionBuyEvent tx))
