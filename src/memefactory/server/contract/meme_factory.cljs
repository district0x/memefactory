(ns memefactory.server.contract.meme-factory
  (:require
    [cljs-web3.eth :as web3-eth]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [memefactory.server.contract.dank-token :as dank-token]))

(defn create-meme [{:keys [:creator :name :meta-hash :total-supply :start-price]} & [opts]]
  (contract-call :meme-factory :create-meme creator name meta-hash total-supply start-price (merge opts {:gas 3000000})))

(defn create-meme-data [{:keys [:creator :name :meta-hash :total-supply :start-price]}]
  (web3-eth/contract-get-data (instance :meme-factory) :create-meme creator name meta-hash total-supply start-price))

(defn approve-and-create-meme [{:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender (contract-address :meme-factory)
                                :amount amount
                                :extra-data (create-meme-data (merge {:creator (:from opts)} args))}
                               (merge opts {:gas 1200000})))