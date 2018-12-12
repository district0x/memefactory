(ns memefactory.server.contract.dank-faucet
  (:require
   [district.server.smart-contracts :refer [contract-call]]))

(defn phone-number-verification-event [& args]
  #_(apply contract-call [:dank-faucet] :PhoneNumberVerification args))
