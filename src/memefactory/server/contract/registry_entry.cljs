(ns memefactory.server.contract.registry-entry
  (:require
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [cljs-web3.eth :as web3-eth]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [memefactory.server.contract.dank-token :as dank-token]
    [memefactory.server.contract.minime-token :as minime-token]
    [memefactory.shared.contract.registry-entry :refer [parse-status parse-load-registry-entry
                                                        parse-load-registry-entry-challenge
                                                        parse-load-vote vote-option->num]]))

(defn registry [contract-addr]
  (contract-call [:meme contract-addr] :registry))

(defn status
  [contract-addr]
  (parse-status (contract-call [:meme contract-addr] :status)))

(defn load-registry-entry [contract-addr]
  (parse-load-registry-entry
    contract-addr
    (contract-call (instance :meme contract-addr) :load-registry-entry)))

(defn load-registry-entry-challenge [contract-addr]
  (parse-load-registry-entry-challenge
    contract-addr
    (contract-call (instance :meme contract-addr) :load-registry-entry-challenge)))

(defn create-challenge [contract-addr {:keys [:challenger :meta-hash]} & [opts]]
  (contract-call (instance :meme contract-addr) :create-challenge challenger meta-hash (merge {:gas 1200000} opts)))

(defn create-challenge-data [{:keys [:challenger :meta-hash]}]
  (web3-eth/contract-get-data (instance :meme) :create-challenge challenger meta-hash))

(defn approve-and-create-challenge [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount amount
                                :extra-data (create-challenge-data (merge {:challenger (:from opts)} args))}
                               (merge {:gas 6000000} opts)))

(defn commit-vote [contract-addr {:keys [:voter :amount :vote-option :salt]} & [opts]]
  (contract-call (instance :meme contract-addr)
                 :commit-vote
                 voter
                 (bn/number amount)
                 (solidity-sha3 (vote-option->num vote-option) salt)
                 (merge {:gas 1200000} opts)))

(defn commit-vote-data [{:keys [:voter :amount :vote-option :salt]}]
  (web3-eth/contract-get-data (instance :meme) :commit-vote voter (bn/number amount) (solidity-sha3 (vote-option->num vote-option) salt)))

(defn approve-and-commit-vote [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount amount
                                :extra-data (commit-vote-data (merge {:voter (:from opts)} args))}
                               (merge opts {:gas 1200000})))

(defn reveal-vote [contract-addr {:keys [:vote-option :salt]} & [opts]]
  (contract-call (instance :meme contract-addr) :reveal-vote (vote-option->num vote-option) salt (merge {:gas 500000} opts)))

(defn claim-vote-reward [contract-addr & [opts]]
  (contract-call (instance :meme contract-addr) :claim-vote-reward (:from opts) (merge {:gas 500000} opts)))

(defn reclaim-vote-amount [contract-addr & [opts]]
  (contract-call (instance :meme contract-addr) :reclaim-vote-amount (:from opts) (merge {:gas 500000} opts)))

(defn load-vote [contract-addr voter-address]
  (parse-load-vote
    contract-addr
    voter-address
    (contract-call (instance :meme contract-addr) :load-vote voter-address)))

(defn vote-reward [contract-addr voter-address]
  (contract-call (instance :meme contract-addr) :vote-reward voter-address))

(defn claim-challenge-reward [contract-addr & [opts]]
  (contract-call (instance :meme contract-addr) :claim-challenge-reward (merge {:gas 500000} opts)))

