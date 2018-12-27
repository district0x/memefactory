(ns memefactory.ui.get-dank.events
  (:require [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [memefactory.ui.config :refer [config-map]]
            [memefactory.ui.contract.meme-factory :as meme-factory]
            [print.foo :refer [look] :include-macros true]
            [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [goog.string :as gstring]
            [graphql-query.core :refer [graphql-query]]))

(re-frame/reg-event-fx
 ::send-verification-code
 (fn [{:keys [db]} [_ {:keys [country-code phone-number]}]]
   (let [mutation (gstring/format
                   "mutation {sendVerificationCode(countryCode:\"%s\", phoneNumber:\"%s\") {id, status, msg, success}}"
                   country-code phone-number)]
     (look {:http-xhrio {:method          :post
                         :uri             (get-in config-map [:graphql :url])
                         :params          {:query mutation}
                         :timeout         8000
                         :response-format (ajax/json-response-format {:keywords? true})
                         :format          (ajax/json-request-format)
                         :on-success      [::verification-code-success]
                         :on-failure      [::verification-code-error]}}))))

(re-frame/reg-event-db
 ::verification-code-success
 (fn [db [_ data]]
   ;; TODO add twilio level errors here
   (println "in verification-code-success, data:" data)

   db))

(re-frame/reg-event-fx
 ::encrypt-verification-payload
 (fn [{:keys [db]} [_ {:keys [country-code phone-number verification-code] :as data}]]
   (let [mutation (gstring/format
                   "mutation {encryptVerificationPayload(countryCode:\"%s\", phoneNumber:\"%s\", verificationCode:\"%s\") {payload, success}}"
                   country-code phone-number verification-code)]
     (look
      {:http-xhrio {:method          :post
                    :uri             (get-in config-map [:graphql :url])
                    :params          {:query mutation}
                    :timeout         8000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :format          (ajax/json-request-format)
                    :on-success      [::verify-and-acquire-dank data]
                    :on-failure      [::verification-code-error]}}))))

(re-frame/reg-event-fx
 ::verify-and-acquire-dank
 (fn [{:keys [db]} [_ {:keys [country-code phone-number verification-code]
                       :as data} http-resp]]
   (println "in verify-and-acquire-dank data:" data)
   (println "in verify-and-acquire-dank http-resp:" http-resp)
   (let [active-account (account-queries/active-account db)
         encrypted-payload (get-in http-resp [:data :encryptVerificationPayload :payload])
         oraclize-string (str "[computation] "
                              "['QmdKK319Veha83h6AYgQqhx9YRsJ9MJE7y33oCXyZ4MqHE', "
                              "'GET', "
                              "'https://api.authy.com/protected/json/phones/verification/check', "
                              "'${[decrypt] "
                              encrypted-payload
                              "}']")]
     (println "in verify-and-acquire-dank")
     (println "country-code:" country-code "phone-number" phone-number "verification-code" verification-code)
     (println "encrypted-payload:" encrypted-payload)
     (println "oraclize-string:" oraclize-string)
     (when encrypted-payload
       {:dispatch
        [::tx-events/send-tx {:instance (contract-queries/instance db :dank-faucet)
                              :fn :verify-and-acquire-dank
                              :args [(-> phone-number web3/sha3)
                                     oraclize-string]
                              :tx-opts {:from active-account
                                        :gas 500000}
                              ;;:tx-id {:get-dank/verify-and-acquire-dank id}
                              :on-tx-success-n [[::logging/success
                                                 [::acquire-dank-success]]
                                                [::notification-events/show
                                                 "DANK acquired"]]
                              :on-tx-hash-error [::logging/error
                                                 [::verify-and-acquire-dank]]
                              :on-tx-error [::logging/error
                                            [::verify-and-acquire-dank]]}]}))))

(re-frame/reg-event-db
 ::verification-code-error
 (fn [db [_ data]]
   ;; TODO add http level errors here
   (println "in verification-code-error, data:" data)
   db))

(re-frame/reg-event-db
 ::acquire-dank-success
 (fn [db [_ data]]
   ;; TODO add twilio level errors here
   (println "in acquire-dank-success, data:" data)
   db))

(re-frame/reg-event-db
 ::acquire-dank-error
 (fn [db [_ data]]
   ;; TODO add http level errors here
   (println "in acquire-dank-error, data:" data)
   db))
