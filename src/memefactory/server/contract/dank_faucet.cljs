(ns memefactory.server.contract.dank-faucet
  (:require
    [cljs-web3.core :as web3]
    [district.server.smart-contracts :refer [create-event-filter contract-call]]))

(defn dank-reset-event [opts on-event]
  (create-event-filter [:dank-faucet] :DankReset {} opts on-event))

(defn reset-allocated-dank-for-phone-number [country-code phone-number & opts]
  (contract-call :dank-faucet :reset-allocated-dank-for-phone-number [(web3/sha3 (str country-code phone-number))] opts))
