(ns memefactory.server.contract.meme-auction-factory
  (:require [district.server.smart-contracts :refer [contract-call contract-event-in-tx]]))

(defn construct [{:keys [:meme-token]} & [opts]]
  #_(contract-call [:meme-auction-factory :meme-auction-factory-fwd] :construct meme-token (merge {:gas 200000} opts)))

(defn meme-auction-started-event [& args]
  #_(apply contract-call [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent args))

(defn meme-auction-buy-event [& args]
  #_(apply contract-call [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionBuyEvent args))

(defn meme-auction-canceled-event [& args]
  #_(apply contract-call [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionCanceledEvent args))

(defn meme-auction-started-event-in-tx [tx-hash & args]
  #_(apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent args))

(defn meme-auction-buy-event-in-tx [tx-hash & args]
  #_(apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionBuyEvent args))

(defn meme-auction-canceled-event-in-tx [tx-hash & args]
  #_(apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionCanceledEvent args))
