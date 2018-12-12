(ns memefactory.server.contract.ds-guard
  (:require
    [district.server.smart-contracts :refer [contract-call instance contract-address]]))

(def ANY "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")

(defn permit [{:keys [:src :dst :sig]} & [opts]]
  #_(contract-call :ds-guard :permit src dst sig (merge opts {:gas 100000})))

(defn can-call? [{:keys [:src :dst :sig]}]
  #_(contract-call :ds-guard :can-call src dst sig))
