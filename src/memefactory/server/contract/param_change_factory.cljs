(ns memefactory.server.contract.param-change-factory
  (:require [camel-snake-kebab.core :as cs]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]))

;; (defn create-param-change [{:keys [:creator :db :key :value]} & [opts]]
;;   (contract-call :param-change-factory :create-param-change [creator db (cs/->camelCaseString key) value] (merge {:gas 700000} opts)))

(defn create-param-change-data [{:keys [:creator :db :key :value :meta-hash] :as args}]
  (web3-eth/encode-abi (smart-contracts/instance :param-change-factory)
                       :create-param-change
                       [creator
                        db
                        (cs/->camelCaseString key)
                        (str value)
                        (web3-utils/from-ascii @web3 meta-hash)]))

(defn approve-and-create-param-change [{:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender (smart-contracts/contract-address :param-change-factory)
                                :amount (str amount)
                                :extra-data (create-param-change-data (merge {:creator (:from opts)} args))}
                               (merge {:gas 800000} opts)))
