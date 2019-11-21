(ns memefactory.server.contract.ds-guard
  (:require [district.server.smart-contracts :as smart-contracts]))

(def ANY "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")

(defn can-call? [{:keys [:src :dst :sig]}]
  (smart-contracts/contract-call :ds-guard :can-call [src dst sig]))
