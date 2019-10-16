(ns memefactory.server.contract.district0x-emails
  (:require [district.server.smart-contracts :as smart-contracts]
            [district.encryption :as encryption]
            [district.server.config :as config]))

(defn get-email [{:keys [:district0x-emails/address]}]
  (smart-contracts/contract-call :district0x-emails :get-email [address]))

(defn set-email [email]
  (smart-contracts/contract-send :district0x-emails :set-email [(encryption/encrypt-encode (get-in @config/config [:ui :public-key]) email)]))
