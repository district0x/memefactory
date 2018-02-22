(ns memefactory.server.contract.dank-token
  (:require
    [bignumber.core :as bn]
    [district.server.smart-contracts :refer [contract-call]]))

(defn approve-and-call [{:keys [:spender :amount :extra-data]} & [opts]]
  (contract-call :DANK :approve-and-call spender (bn/number amount) extra-data (merge opts {:gas 4000000})))

(defn approve [{:keys [:spender :amount]} & [opts]]
  (contract-call :DANK :approve spender (bn/number amount) (merge opts {:gas 1000000})))

(defn transfer [{:keys [:to :amount]} & [opts]]
  (contract-call :DANK :transfer to (bn/number amount) (merge opts {:gas 200000})))
