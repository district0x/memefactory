(ns memefactory.server.contract.meme-factory
  (:require
   [district.server.web3 :refer [web3]]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.utils :as web3-utils]
   [district.server.smart-contracts :as smart-contracts]
   [memefactory.server.contract.dank-token :as dank-token]))

#_(defn create-meme [{:keys [:creator :meta-hash :total-supply]} & [opts]]
  (smart-contracts/contract-call :meme-factory :create-meme [creator meta-hash total-supply] (merge {:gas 3000000} opts)))

(defn create-meme-data [{:keys [:creator :meta-hash :total-supply] :as args}]
  (web3-eth/encode-abi @web3 (smart-contracts/instance :meme-factory) :create-meme [creator (web3-utils/from-ascii @web3 meta-hash) total-supply]))

(defn approve-and-create-meme [{:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender (smart-contracts/contract-address :meme-factory)
                                :amount amount
                                :extra-data (create-meme-data (merge {:creator (:from opts)} args))}
                               (merge {:gas 1200000} opts)))
