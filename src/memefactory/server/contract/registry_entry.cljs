(ns memefactory.server.contract.registry-entry
  (:require

   [district.server.smart-contracts :as smart-contracts]
   [memefactory.server.contract.dank-token :as dank-token]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.utils :as web3-utils]
   [district.server.web3 :refer [web3]]
   [district.shared.async-helpers :refer [safe-go <?]]

   [bignumber.core :as bn]
   ;; [camel-snake-kebab.core :as cs :include-macros true]
   ;; [cljs-solidity-sha3.core :refer [solidity-sha3]]
   ;; [district.shared.async-helpers :refer [promise->]]
   ;; [memefactory.server.contract.minime-token :as minime-token]
   [memefactory.shared.contract.registry-entry :refer [parse-status ;;parse-load-registry-entry parse-load-vote
                                                       vote-option->num]]


   ))

;; (defn registry [contract-addr]
;;   (contract-call [:meme contract-addr] :registry))



(defn status [contract-addr]
  (-> (smart-contracts/contract-call [:meme contract-addr] :status)
      (.then #(parse-status %))))

;; (defn create-challenge [contract-addr {:keys [:challenger :meta-hash]} & [opts]]
;;   (contract-call (instance :meme contract-addr) :create-challenge [challenger meta-hash] (merge {:gas 1200000} opts)))

(defn create-challenge-data [{:keys [:challenger :meta-hash]}]
  (web3-eth/encode-abi @web3 (smart-contracts/instance :meme) :create-challenge [challenger (web3-utils/from-ascii @web3 meta-hash)]))

(defn approve-and-create-challenge [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount amount
                                :extra-data (create-challenge-data (merge {:challenger (:from opts)} args))}
                               (merge {:gas 6000000} opts)))

;; (defn commit-vote [contract-addr {:keys [:voter :amount :vote-option :salt]} & [opts]]
;;   (contract-call (instance :meme contract-addr)
;;                  :commit-vote
;;                  [voter
;;                   (bn/number amount)
;;                   (solidity-sha3 (vote-option->num vote-option) salt)]
;;                  (merge {:gas 1200000} opts)))


(defn commit-vote-data [{:keys [:voter :amount :vote-option :salt]}]
  (web3-eth/encode-abi @web3 (smart-contracts/instance :meme) :commit-vote [voter amount (web3-utils/solidity-sha3 @web3 (vote-option->num vote-option) salt)]))

(defn approve-and-commit-vote [contract-addr {:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender contract-addr
                                :amount amount
                                :extra-data (commit-vote-data (merge {:voter (:from opts)} args))}
                               (merge opts {:gas 1200000})))

(defn reveal-vote [contract-addr {:keys [:address :vote-option :salt]} & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :meme contract-addr) :reveal-vote [(vote-option->num vote-option) salt] (merge {:gas 500000} opts)))

(defn claim-rewards [contract-addr & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :meme contract-addr) :claim-rewards [(:from opts)] (merge {:gas 500000} opts)))

;; (defn reclaim-vote-amount [contract-addr & [opts]]
;;   (contract-call (instance :meme contract-addr) :reclaim-vote-amount [(:from opts)] (merge {:gas 500000} opts)))

;; (defn load-registry-entry [contract-addr & [opts]]
;;   (contract-call (instance :meme contract-addr) :load [] (merge {:gas 500000} opts)))

;; (defn load-registry-entry-vote [contract-addr voter-addr & [opts]]
;;   (contract-call (instance :meme contract-addr) :load-vote [voter-addr] (merge {:gas 500000} opts)))
