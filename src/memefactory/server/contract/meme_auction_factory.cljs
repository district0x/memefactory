(ns memefactory.server.contract.meme-auction-factory
  (:require [district.server.smart-contracts :refer [create-event-filter contract-call contract-event-in-tx contract-events-in-tx]]))

(defn construct [{:keys [:meme-token]} & [opts]]
  (contract-call [:meme-auction-factory :meme-auction-factory-fwd] :construct [meme-token] (merge {:gas 200000} opts)))

(defn meme-auction-started-event [opts on-event]
  (create-event-filter [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent {} opts on-event))

(defn meme-auction-buy-event [opts on-event]
  (create-event-filter [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionBuyEvent {} opts on-event))

(defn meme-auction-canceled-event [opts on-event]
  (create-event-filter [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionCanceledEvent {} opts on-event))

(defn meme-auction-started-event-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent args))

(defn meme-auction-started-events-in-tx [tx-hash & args]
  (apply contract-events-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent args))

(defn meme-auction-buy-event-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionBuyEvent args))

(defn meme-auction-canceled-event-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionCanceledEvent args))
