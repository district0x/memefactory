(ns memefactory.server.contract.meme
  (:require [bignumber.core :as bn]
            [cljs-web3.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [district.shared.async-helpers :refer [promise->]]
            [memefactory.shared.utils :as shared-utils]))

(defn construct [contract-addr {:keys [:creator/address :version :meta-hash :total-supply]} & [opts]]
  (smart-contracts/contract-send [:meme contract-addr] :construct [address (or version 1) (web3-utils/from-ascii @web3 meta-hash) total-supply] (merge {:from address :gas 6000000} opts)))

(defn mint [contract-addr & [amount opts]]
  (smart-contracts/contract-send [:meme contract-addr] :mint [(or amount 0)] (merge {:gas 6000000} opts)))

(defn load-meme [contract-addr]
  (promise-> (smart-contracts/contract-call (smart-contracts/instance :meme contract-addr) :load-meme)
             (fn [meme]
               {:reg-entry/address contract-addr
                :meme/meta-hash (web3-utils/to-ascii @web3 (aget meme "0"))
                :meme/total-supply (bn/number (aget meme "1"))
                :meme/total-minted (bn/number (aget meme "2"))
                :meme/token-id-start (bn/number (aget meme "3"))})))

(defn transfer-deposit [contract-addr & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :meme contract-addr) :transfer-deposit [] (merge {:gas 300000} opts)))
