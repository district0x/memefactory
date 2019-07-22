(ns memefactory.server.contract.param-change-factory
  (:require [camel-snake-kebab.core :as cs :include-macros true]
            [cljs-solidity-sha3.core :refer [solidity-sha3]]
            [cljs-web3.eth :as web3-eth]
            [district.server.smart-contracts :refer [contract-call instance contract-address]]
            [memefactory.server.contract.dank-token :as dank-token]
            [print.foo :refer [look] :include-macros true]))

(defn create-param-change [{:keys [:creator :db :key :value]} & [opts]]
  (contract-call :param-change-factory :create-param-change [creator db (cs/->camelCaseString key) value] (merge {:gas 700000} opts)))

(defn create-param-change-data [{:keys [:creator :db :key :value :meta-hash] :as args}]
  (web3-eth/contract-get-data (instance :param-change-factory)
                              :create-param-change
                              creator
                              db
                              (-> key (cs/->camelCaseString))
                              value
                              meta-hash))

(defn approve-and-create-param-change [{:keys [:amount] :as args} & [opts]]
  (dank-token/approve-and-call {:spender (contract-address :param-change-factory)
                                :amount amount
                                :extra-data (create-param-change-data (merge {:creator (:from opts)} args))}
                               (merge {:gas 700000} opts)))
