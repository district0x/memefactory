(ns memefactory.server.contract.dank-faucet
  (:require
   [district.server.smart-contracts :refer [contract-call]]))

(defn phone-number-verification-event [& args]
  (apply contract-call [:dank-faucet] :PhoneNumberVerification args))
