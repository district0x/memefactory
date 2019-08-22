(ns memefactory.ui.get-dank.events
  (:require
    [ajax.core :as ajax]
    [cljs-web3.core :as web3]
    [day8.re-frame.http-fx]
    [district.ui.graphql.utils :as graphql-ui-utils]
    [district.graphql-utils :as graphql-utils]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [district0x.re-frame.web3-fx]
    [goog.string :as gstring]
    [graphql-query.core :refer [graphql-query]]
    [memefactory.ui.config :refer [config-map]]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame]
    [taoensso.timbre :as log]))

(defn- parse-query [query]
  (:query-str (graphql-ui-utils/parse-query {:queries [query]}
                                            {:kw->gql-name graphql-utils/kw->gql-name})))

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
 (fn [{:keys [db]} [_ stage-number]]
   {:db (assoc db :memefactory.ui.get-dank.page/stage stage-number)}))


(re-frame/reg-event-fx
 ::get-allocated-dank
 (fn [{:keys [db]} [_ {:keys [country-code phone-number] :as data}]]
   {:web3/call {:web3 (:web3 db)
                :fns [{:instance (contract-queries/instance db :dank-faucet)
                       :fn :allocated-dank
                       :args [(web3/sha3 (str country-code phone-number))]
                       :on-success [::send-dank-but-only-once data]
                       :on-error [::logging/error "Error calling DankFaucet" data ::get-allocated-dank]}]}}))

(re-frame/reg-event-fx
 ::send-dank-but-only-once
 (fn [_ [_ {:keys [country-code phone-number] :as args} resp]]
   (let [allocated-dank (aget resp "c")]
     (if (<= allocated-dank 0)
       {:dispatch [::send-verification-code args]}
       {:log/info ["DANK already acquired" {:args args :response resp}]
        :dispatch [::notification-events/show "DANK already acquired"]}))))

(re-frame/reg-event-fx
 ::send-verification-code
 (fn [_ [_ {:keys [country-code phone-number] :as args}]]
   (let [mutation (str "mutation" (parse-query [:send-verification-code
                                                {:country-code country-code
                                                 :phone-number phone-number}
                                                [:id
                                                 :status
                                                 :message
                                                 :success]]))]
     {:http-xhrio {:method          :post
                   :uri             (get-in config-map [:graphql :url])
                   :params          {:query mutation}
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :format          (ajax/json-request-format)
                   :on-success      [::verification-code-success]
                   :on-failure      [::logging/error "Error calling sendVerificationCode" args]}})))


(re-frame/reg-event-fx
 ::verification-code-success
 (fn [_ [_ {:keys [data]}]]
   (let [success (get-in data [:sendVerificationCode :success])
         msg     (get-in data [:sendVerificationCode :msg])]
     (if success
       {:log/info ["Successfully verified phone number" data ::verification-code-success]
        :dispatch [::stage 2]}
       {:dispatch [:district.ui.notification.events/show
                   "Internal error verifying the phone number"]}))))

(re-frame/reg-event-fx
 ::encrypt-verification-payload
 (fn [_ [_ {:keys [country-code phone-number verification-code] :as args}]]
   (let [mutation (str "mutation" (parse-query [:encrypt-verification-payload
                                                {:country-code country-code
                                                 :phone-number phone-number
                                                 :verification-code verification-code}
                                                [:payload
                                                 :success]]))]
     {:http-xhrio {:method          :post
                   :uri             (get-in config-map [:graphql :url])
                   :params          {:query mutation}
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :format          (ajax/json-request-format)
                   :on-success      [::encrypt-payload-success args]
                   :on-failure      [::logging/error "Error calling encryptVerificationPayload endpoint" args]}})))


(re-frame/reg-event-fx
 ::encrypt-payload-success
 (fn [{:keys [db]} [_ {:keys [country-code phone-number verification-code] :as data} http-resp]]
   {:log/info ["Encryption success" data ::encrypt-payload-success]
    :db (assoc db ::spinner true)
    :dispatch [::verify-and-acquire-dank data http-resp]}))


(re-frame/reg-event-fx
 ::verify-and-acquire-dank
 (fn [{:keys [db]} [_ {:keys [country-code phone-number verification-code]
                       :as data} http-resp]]
   (let [active-account (account-queries/active-account db)
         encrypted-payload (get-in http-resp [:data :encryptVerificationPayload :payload])
         args [(web3/sha3 (str country-code phone-number)) encrypted-payload]]
     (log/debug "Succesfully encrypted payload" {:payload encrypted-payload})
     (when encrypted-payload
       {:dispatch
        [::tx-events/send-tx {:instance (contract-queries/instance db :dank-faucet)
                              :fn :verify-and-acquire-dank
                              :args args
                              :tx-log {:name "Request DANK"}
                              :tx-opts {:from active-account}
                              :on-tx-success-n [[::hide-spinner]
                                                [::notification-events/show
                                                 "Successfully requested DANK. It'll be delivered within few minutes!"]
                                                [::stage 1]]
                              :on-tx-error [::logging/error "Error calling DankFaucet" {:user {:id active-account}
                                                                                        :args args}
                                            ::verify-and-acquire-dank]}]}))))
