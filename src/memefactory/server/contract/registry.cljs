(ns memefactory.server.contract.registry
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn set-factory [contract-key {:keys [:factory :factory?]} & [opts]]
  (smart-contracts/contract-send contract-key :set-factory [factory factory?] (merge opts {:gas 100000})))

(defn meme-constructed-event-in-tx [contract-key tx-hash]
  (smart-contracts/contract-event-in-tx tx-hash contract-key :MemeConstructedEvent))

(defn is-factory? [contract-key factory]
  (smart-contracts/contract-call contract-key :is-factory [factory]))
