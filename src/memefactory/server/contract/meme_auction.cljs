(ns memefactory.server.contract.meme-auction
  (:require [cljs-web3.eth :as web3-eth]
            [cljs-web3.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]))

(defn buy [contract-addr & [{:keys [:from :value :gas] :as opts}]]
  (smart-contracts/contract-send [:meme-auction contract-addr] :buy [] (merge {:gas 500000} opts)))

(defn start-auction-data [{:keys [:start-price :end-price :duration :description] :as args}]
  (web3-eth/encode-abi (smart-contracts/instance :meme-auction) :start-auction [start-price end-price duration description]))
