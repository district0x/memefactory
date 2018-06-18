(ns memefactory.server.contract.meme-auction
  (:require
    [district.server.smart-contracts :refer [contract-call instance]]
    [memefactory.shared.contract.meme-auction :refer [parse-load-meme-auction]]
    [cljs-web3.eth :as web3-eth]))

(defn buy [contract-addr & [opts]]
  (contract-call [:meme-auction contract-addr] :buy (merge {:gas 500000} opts)))

(defn cancel [contract-addr & [opts]]
  (contract-call [:meme-auction contract-addr] :cancel (merge {:gas 500000} opts)))

(defn start-auction-data [{:keys [:start-price :end-price :duration :description]}]
  (web3-eth/contract-get-data (instance :meme-auction) :start-auction start-price end-price duration description))

(defn load-meme-auction [contract-address]
  (parse-load-meme-auction
    contract-address
    (contract-call [:meme-auction contract-address] :load-meme-auction)))

(defn current-price [contract-address]
  (contract-call [:meme-auction contract-address] :current-price))
