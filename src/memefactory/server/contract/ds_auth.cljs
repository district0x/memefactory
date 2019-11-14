(ns memefactory.server.contract.ds-auth
  (:require [district.server.smart-contracts :as smart-contracts]))

;; (defn owner [contract-key]
;;   (contract-call contract-key :owner))

;; (defn set-owner [contract-key new-owner & [opts]]
;;   (contract-call contract-key :set-owner [new-owner] (merge {:gas 100000} opts)))

;; (defn set-authority [contract-key new-authority & [opts]]
;;   (contract-call contract-key :set-authority [new-authority] (merge {:gas 100000} opts)))

(defn authority [contract-key]
  (smart-contracts/contract-call contract-key :authority))
