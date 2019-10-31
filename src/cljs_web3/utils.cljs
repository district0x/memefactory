(ns cljs-web3.utils
  (:require [cljs-web3.api :as api]))

(defn sha3 [{:keys [:instance :provider]} args]
  (api/-sha3 instance provider args))

(defn solidity-sha3 [{:keys [:instance :provider]} arg & args]
  (api/-solidity-sha3 instance provider [arg args]))

(defn from-ascii [{:keys [:instance :provider]} args]
  (api/-from-ascii instance provider args))

(defn to-ascii [{:keys [:instance :provider]} args]
  (api/-to-ascii instance provider args))

(defn number-to-hex [{:keys [:instance :provider]} number]
  (api/-number-to-hex instance provider number))

(defn from-wei [{:keys [:instance :provider]} number & [unit]]
  (api/-from-wei instance provider number unit))

(defn to-wei [{:keys [:instance :provider]} number & [unit]]
  (api/-to-wei instance provider number unit))

(defn address? [{:keys [:instance :provider]} address]
  (api/-address? instance provider address))
