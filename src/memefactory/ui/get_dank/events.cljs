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
            [graphql-query.core :refer [graphql-query]]
            [taoensso.timbre :as log]))

(re-frame/reg-event-fx
 ::show-spinner
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db
          ::spinner true)}))

(re-frame/reg-event-fx
 ::hide-spinner
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db
          ::spinner false)}))

(re-frame/reg-event-fx
 ::stage
 (fn [{:keys [db]} [_ {:keys [stage-number]}]]
   {:db (assoc db :memefactory.ui.get-dank.page/stage stage-number)}))

(re-frame/reg-event-fx
 ::send-verification-code
 (fn [{:keys [db]} [_ {:keys [country-code phone-number]}]]
   (let [allocated-dank (web3-eth/contract-get-data
                         (contract-queries/instance db :dank-faucet)
                         :allocated-dank
                         (web3/sha3 phone-number))]
     (log/debug "Faucet contract:" (contract-queries/instance db :dank-faucet))
     (log/debug "DANK already allocated to this phone number:" allocated-dank)
     (if (<= allocated-dank 0)

       (let [mutation (gstring/format
                       "mutation {sendVerificationCode(countryCode:\"%s\", phoneNumber:\"%s\") {id, status, msg, success}}"
                       country-code phone-number)]
         {:http-xhrio {:method          :post
                       :uri             (get-in config-map [:graphql :url])
                       :params          {:query mutation}
                       :timeout         8000
                       :response-format (ajax/json-response-format {:keywords? true})
                       :format          (ajax/json-request-format)
                       :on-success      [::verification-code-success]
                       :on-failure      [::verification-code-error]}})

       {:db db
        :dispatch [::notification-events/show "DANK already acquired bawse"]}))))

(re-frame/reg-event-fx
 ::verification-code-success
 (fn [{:keys [db]} [_ {:keys [data]}]]
   (log/debug "in verification-code-success, data:" data)
   (let [success (get-in data [:sendVerificationCode :success])
         msg     (get-in data [:sendVerificationCode :msg])]
     (if success
       {:db db
        :dispatch [::stage 2]}

       ;; Handle Twilio level errors here
       {:db db
        :dispatch [:district.ui.notification.events/show
                   "Internal error verifying the phone number"]}))))

(re-frame/reg-event-fx
 ::encrypt-verification-payload
 (fn [{:keys [db]} [_ {:keys [country-code phone-number verification-code] :as data}]]
   (let [mutation (gstring/format
                   "mutation {encryptVerificationPayload(countryCode:\"%s\", phoneNumber:\"%s\", verificationCode:\"%s\") {payload, success}}"
                   country-code phone-number verification-code)]
     {:http-xhrio {:method          :post
                   :uri             (get-in config-map [:graphql :url])
                   :params          {:query mutation}
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :format          (ajax/json-request-format)
                   :on-success      [::encrypt-payload-success data]
                   :on-failure      [::verification-code-error]}})))

(re-frame/reg-event-fx
 ::encrypt-payload-success
 (fn [{:keys [db]} [_ {:keys [country-code phone-number verification-code] :as data} http-resp]]
   (log/info "Encryption success:" data)
   {:db (assoc db
          ::spinner true)
    :dispatch [::verify-and-acquire-dank data http-resp]}))

(re-frame/reg-event-fx
 ::verify-and-acquire-dank
 (fn [{:keys [db]} [_ {:keys [country-code phone-number verification-code]
                       :as data} http-resp]]
   (log/debug "in verify-and-acquire-dank data:" data)
   (log/debug "in verify-and-acquire-dank http-resp:" http-resp)
   (let [active-account (account-queries/active-account db)
         encrypted-payload (get-in http-resp [:data :encryptVerificationPayload :payload])
         oraclize-string (str "[computation] "
                              "['QmdKK319Veha83h6AYgQqhx9YRsJ9MJE7y33oCXyZ4MqHE', "
                              "'GET', "
                              "'https://api.authy.com/protected/json/phones/verification/check', "
                              "'${[decrypt] "
                              encrypted-payload
                              "}']")]
     (log/debug "in verify-and-acquire-dank")
     (log/debug "country-code:" country-code "phone-number" phone-number "verification-code" verification-code)
     (log/debug "encrypted-payload:" encrypted-payload)
     (log/debug "oraclize-string:" oraclize-string)
     (when encrypted-payload
       {:dispatch
        [::tx-events/send-tx {:instance (look (contract-queries/instance db :dank-faucet))
                              :fn :verify-and-acquire-dank
                              :args (look [(-> phone-number web3/sha3)
                                           oraclize-string])
                              :tx-log {:name "Request DANK"}
                              :tx-opts {:from active-account}
                              ;;:tx-id {:get-dank/verify-and-acquire-dank id}
                              :on-tx-success-n [[::hide-spinner]
                                                [::notification-events/show
                                                 "Successfully requested DANK. It'll be delivered within few minutes!"]
                                                [::stage 1]]
                              :on-tx-hash-error [::logging/error
                                                 [::verify-and-acquire-dank]]
                              :on-tx-error [::logging/error
                                            [::verify-and-acquire-dank]]}]}))))

(re-frame/reg-event-db
 ::verification-code-error
 (fn [db [_ data]]
   ;; TODO add http level errors here
   (log/debug "in verification-code-error, data:" data)
   db))

(re-frame/reg-event-db
 ::acquire-dank-success
 (fn [db [_ data]]
   ;; TODO add twilio level errors here
   (log/debug "in acquire-dank-success, data:" data)
   db))

(re-frame/reg-event-db
 ::acquire-dank-error
 (fn [db [_ data]]
   ;; TODO add http level errors here
   (log/debug "in acquire-dank-error, data:" data)
   db))
