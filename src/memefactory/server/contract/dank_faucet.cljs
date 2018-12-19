(ns memefactory.server.contract.dank-faucet
  (:require
   [district.server.smart-contracts :refer [create-event-filter]]))

(defn phone-number-verification-event [opts on-event]
  (create-event-filter [:dank-faucet] :PhoneNumberVerification {} opts on-event))
