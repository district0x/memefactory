(ns memefactory.server.contract.meme-auction-factory
  (:require [district.server.smart-contracts :refer [contract-call contract-event-in-tx]]))

(defn construct [{:keys [:meme-token]} & [opts]]
  (contract-call [:meme-auction-factory :meme-auction-factory-fwd] :construct meme-token (merge {:gas 200000} opts)))

(defn meme-auction-event-in-tx [tx-hash & args]
  (apply contract-event-in-tx tx-hash [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionEvent args))

(defn meme-auction-event [& args]
  (apply contract-call [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionEvent args))
