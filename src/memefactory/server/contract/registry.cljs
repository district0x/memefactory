(ns memefactory.server.contract.registry
  (:require [district.server.smart-contracts :refer [contract-call contract-event-in-tx]]))

(defn db [contract-key]
  #_(contract-call contract-key :db))

(defn construct [contract-key {:keys [:db]} & [opts]]
  #_(contract-call contract-key :construct db (merge {:gas 100000} opts)))

(defn set-factory [contract-key {:keys [:factory :factory?]} & [opts]]
  #_(contract-call contract-key :set-factory factory factory? (merge opts {:gas 100000})))

(defn meme-constructed-event [contract-key & args]
  #_(apply contract-call contract-key :MemeConstructedEvent args))

(defn meme-minted-event [contract-key & args]
  #_(apply contract-call contract-key :MemeMintedEvent args))

(defn challenge-created-event [contract-key & args]
  #_(apply contract-call contract-key :ChallengeCreatedEvent args))

(defn vote-committed-event [contract-key & args]
  #_(apply contract-call contract-key :VoteCommittedEvent args))

(defn vote-revealed-event [contract-key & args]
  #_(apply contract-call contract-key :VoteRevealedEvent args))

(defn vote-amount-claimed-event [contract-key & args]
  #_(apply contract-call contract-key :VoteAmountClaimedEvent args))

(defn vote-reward-claimed-event [contract-key & args]
  #_(apply contract-call contract-key :VoteRewardClaimedEvent args))

(defn challenge-reward-claimed-event [contract-key & args]
  #_(apply contract-call contract-key :ChallengeRewardClaimedEvent args))

(defn meme-constructed-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :MemeConstructedEvent args))

(defn meme-minted-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :MemeMintedEvent args))

(defn challenge-created-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :ChallengeCreatedEvent args))

(defn vote-committed-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :VoteCommittedEvent args))

(defn vote-revealed-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :VoteRevealedEvent args))

(defn vote-amount-claimed-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :VoteAmountClaimedEvent args))

(defn vote-reward-claimed-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :VoteRewardClaimedEvent args))

(defn challenge-reward-claimed-event-in-tx [contract-key tx-hash & args]
  #_(apply contract-event-in-tx tx-hash contract-key :ChallengeRewardClaimedEvent args))

(defn factory? [contract-key factory]
  #_(contract-call contract-key :is-factory factory))
