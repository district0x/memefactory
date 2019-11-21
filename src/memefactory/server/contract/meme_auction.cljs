(ns memefactory.server.contract.meme-auction
  (:require [cljs-web3.eth :as web3-eth]
            [cljs-web3.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [district.shared.async-helpers :refer [promise->]]))

(defn buy [contract-addr & [{:keys [:from :value :gas] :as opts}]]
  (smart-contracts/contract-send [:meme-auction contract-addr] :buy [] (merge {:gas 500000} opts)))

(defn cancel [contract-addr & [{:keys [:from :value :gas] :as opts}]]
  (smart-contracts/contract-send [:meme-auction contract-addr] :cancel [] (merge {:gas 500000} opts)))

(defn start-auction-data [{:keys [:start-price :end-price :duration :description] :as args}]
  (web3-eth/encode-abi (smart-contracts/instance :meme-auction) :start-auction [start-price end-price duration description]))

(defn load-meme-auction [contract-addr]
  (promise-> (smart-contracts/contract-call [:meme-auction contract-addr] :load)
             (fn [meme-auction]
               {:meme-auction/address contract-addr
                :meme-auction/seller (aget meme-auction "0")
                :meme-auction/token-id (aget meme-auction "1")
                :meme-auction/start-price (aget meme-auction "2")
                :meme-auction/end-price (aget meme-auction "3")
                :meme-auction/duration (aget meme-auction "4")
                :meme-auction/started-on (aget meme-auction "5")
                :meme-auction/description (aget meme-auction "6")})))

(defn current-price [contract-address]
  (smart-contracts/contract-call [:meme-auction contract-address] :current-price))
