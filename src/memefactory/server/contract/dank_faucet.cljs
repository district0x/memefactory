(ns memefactory.server.contract.dank-faucet
  (:require
    [cljs-web3.core :as web3]
    [district.server.smart-contracts :refer [create-event-filter contract-call]]))

(defn phone-number-verification-event [opts on-event]
  (create-event-filter [:dank-faucet] :PhoneNumberVerification {} opts on-event))

(defn reset-allotment [phone-number & opts]
  (contract-call :dank-faucet :reset-allotment [(web3/sha3 phone-number)] opts))
