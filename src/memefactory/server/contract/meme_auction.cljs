(ns memefactory.server.contract.meme-auction
  (:require [cljs-web3.eth :as web3-eth]
            [cljs-web3.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [memefactory.shared.contract.meme-auction :refer [parse-load-meme-auction]]))

(defn buy [contract-addr & [{:keys [:from :value :gas] :as opts}]]
  (smart-contracts/contract-send [:meme-auction contract-addr] :buy [] (merge {:gas 500000} opts)))

;; (defn cancel [contract-addr & [opts]]
;;   (contract-call [:meme-auction contract-addr] :cancel [] (merge {:gas 500000} opts)))

(defn start-auction-data [{:keys [:start-price :end-price :duration :description] :as args}]
  (web3-eth/encode-abi @web3 (smart-contracts/instance :meme-auction) :start-auction [start-price end-price duration description])

  #_(web3-eth/contract-get-data (instance :meme-auction) :start-auction start-price end-price duration description))

;; (defn current-price [contract-address]
;;   (contract-call [:meme-auction contract-address] :current-price))

;; (defn load-meme-auction [contract-addr]
;;   (promise-> (contract-call [:meme-auction contract-addr] :load)
;;              #(parse-load-meme-auction contract-addr %)))
