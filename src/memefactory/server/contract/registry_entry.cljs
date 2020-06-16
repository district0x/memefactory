(ns memefactory.server.contract.registry-entry
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [clojure.string :as string]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [district.shared.async-helpers :refer [promise->]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.shared.contract.registry-entry
             :refer
             [parse-status vote-option->num]]))

(defn status [contract-addr]
  (-> (smart-contracts/contract-call [:meme contract-addr] :status)
      (.then #(parse-status %))))

(defn create-challenge-data [{:keys [:challenger :meta-hash]}]
  (web3-eth/encode-abi (smart-contracts/instance :meme) :create-challenge [challenger (web3-utils/from-ascii @web3 meta-hash)]))

(defn approve-and-create-challenge [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount (str amount)
                                :extra-data (create-challenge-data (merge {:challenger (:from opts)} args))}
                               (merge {:gas 6000000} opts)))

(defn commit-vote-data [{:keys [:voter :amount :vote-option :salt]}]
  (web3-eth/encode-abi (smart-contracts/instance :meme) :commit-vote [voter amount (web3-utils/solidity-sha3 @web3 (vote-option->num vote-option) salt)]))

(defn approve-and-commit-vote [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount (str amount)
                                :extra-data (commit-vote-data (merge {:voter (:from opts)} args))}
                               (merge {:gas 1200000} opts)))

(defn reveal-vote [contract-addr {:keys [:vote-option :salt]} & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :meme contract-addr) :reveal-vote [(vote-option->num vote-option) salt] (merge {:gas 500000} opts)))

(defn claim-rewards [contract-addr & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :meme contract-addr) :claim-rewards [(:from opts)] (merge {:gas 500000} opts)))

(defn load-registry-entry [contract-addr]
  (promise-> (smart-contracts/contract-call (smart-contracts/instance :meme contract-addr) :load [])
             (fn [registry-entry]
               (let [meta-hash (aget registry-entry "7")]
                 {:reg-entry/address (string/lower-case contract-addr)
                  :reg-entry/deposit (bn/number (aget registry-entry "0"))
                  :reg-entry/creator (string/lower-case (aget registry-entry "1"))
                  :reg-entry/version (bn/number (aget registry-entry "2"))
                  :challenge/challenger (string/lower-case (aget registry-entry "3"))
                  :challenge/commit-period-end (bn/number (aget registry-entry "4"))
                  :challenge/reveal-period-end (bn/number (aget registry-entry "5"))
                  :challenge/reward-pool (bn/number (aget registry-entry "6"))
                  :challenge/meta-hash (when meta-hash (web3-utils/to-ascii @web3 meta-hash))
                  :challenge/claimed-reward-on (bn/number (aget registry-entry "8"))}))))

(defn load-registry-entry-vote [contract-addr voter-addr]
  (promise-> (smart-contracts/contract-call [:meme contract-addr] :load-vote [voter-addr])
             (fn [vote]
               {:vote/secret-hash (aget vote "0")
                :vote/option (js/parseInt (aget vote "1"))
                :vote/amount (bn/number (aget vote "2"))
                :vote/revealed-on (bn/number (aget vote "3"))
                :vote/claimed-reward-on (bn/number (aget vote "4"))
                :vote/reclaimed-deposit-on (bn/number (aget vote "5"))})))

(defn reclaim-vote-amount [contract-addr & [opts]]
  (smart-contracts/contract-send [:meme contract-addr] :reclaim-vote-amount [(:from opts)] (merge {:gas 500000} opts)))
