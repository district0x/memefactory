(ns memefactory.server.contract.ds-auth
  (:require [district.server.smart-contracts :as smart-contracts]))

(defn authority [contract-key]
  (smart-contracts/contract-call contract-key :authority))
