(ns memefactory.server.contract.meme-factory
  (:require [cljs-web3.eth :as web3-eth]
            [cljs-web3.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]))

(defn create-meme-data [{:keys [:creator :meta-hash :total-supply] :as args}]
  (web3-eth/encode-abi (smart-contracts/instance :meme-factory) :create-meme [creator (web3-utils/from-ascii @web3 meta-hash) total-supply]))

(defn approve-and-create-meme [{:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender (smart-contracts/contract-address :meme-factory)
                                :amount amount
                                :extra-data (create-meme-data (merge {:creator (:from opts)} args))}
                               (merge {:gas 1200000} opts)))
