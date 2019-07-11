(ns memefactory.server.contract.registry
  (:require [district.server.smart-contracts :refer [contract-call create-event-filter contract-event-in-tx]]))

(defn db [contract-key]
  (contract-call contract-key :db))

(defn construct [contract-key {:keys [:db]} & [opts]]
  (contract-call contract-key :construct [db] (merge {:gas 100000} opts)))

(defn set-factory [contract-key {:keys [:factory :factory?]} & [opts]]
  (contract-call contract-key :set-factory [factory factory?] (merge opts {:gas 100000})))

(defn meme-constructed-event [contract-key opts on-event]
  (create-event-filter contract-key :MemeConstructedEvent {} opts on-event))

(defn meme-minted-event [contract-key opts on-event]
  (create-event-filter contract-key :MemeMintedEvent {} opts on-event))

(defn challenge-created-event [contract-key opts on-event]
  (create-event-filter contract-key :ChallengeCreatedEvent {} opts on-event))

(defn vote-committed-event [contract-key opts on-event]
  (create-event-filter contract-key :VoteCommittedEvent {} opts on-event))

(defn vote-revealed-event [contract-key opts on-event]
  (create-event-filter contract-key :VoteRevealedEvent {} opts on-event))

(defn vote-amount-claimed-event [contract-key opts on-event]
  (create-event-filter contract-key :VoteAmountClaimedEvent {} opts on-event))

(defn vote-reward-claimed-event [contract-key opts on-event]
  (create-event-filter contract-key :VoteRewardClaimedEvent {} opts on-event))

(defn challenge-reward-claimed-event [contract-key opts on-event]
  (create-event-filter contract-key :ChallengeRewardClaimedEvent {} opts on-event))

(defn meme-constructed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :MemeConstructedEvent args))

(defn meme-minted-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :MemeMintedEvent args))

(defn challenge-created-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :ChallengeCreatedEvent args))

(defn vote-committed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VoteCommittedEvent args))

(defn vote-revealed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VoteRevealedEvent args))

(defn vote-amount-claimed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VoteAmountClaimedEvent args))

(defn vote-reward-claimed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :VoteRewardClaimedEvent args))

(defn challenge-reward-claimed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :ChallengeRewardClaimedEvent args))

(defn param-change-constructed-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :ParamChangeConstructedEvent args))

(defn param-change-applied-event-in-tx [contract-key tx-hash & args]
  (apply contract-event-in-tx tx-hash contract-key :ParamChangeAppliedEvent args))

(defn is-factory? [contract-key factory]
  (contract-call contract-key :is-factory [factory]))
