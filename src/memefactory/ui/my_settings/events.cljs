(ns memefactory.ui.my-settings.events
  (:require
   [ajax.core :as ajax]
   [cljs-web3.core :as web3]
   [day8.re-frame.http-fx]
   [district.ui.logging.events :as logging]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [district0x.re-frame.web3-fx]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]))

(re-frame/reg-event-fx
 ::load-email-settings
 (fn [{:keys [db]} [_ {:keys [address] :as data}]]
   {:web3/call {:web3 (:web3 db)
                :fns [{:instance (contract-queries/instance db :district0x-emails)
                       :fn :get-email
                       :args [address]
                       :on-success [::found-encrypted-email data]
                       :on-error [::error-loading-encrypted-email data]}]}}))

(re-frame/reg-event-fx
 ::found-encrypted-email
 (fn [{:keys [db]} [_ {:keys [address] :as data} encrypted-email]]
   {:db
    (assoc-in db [:settings address :encrypted-email] encrypted-email)}))

(re-frame/reg-event-fx
 ::error-loading-encrypted-email
 (fn [{:keys [db]} [_ :data]]
   {:db db
    :dispatch-n [[:district.ui.notification.events/show
                  "Error loading your encrypted email"]
                 [::logging/error "Error loading user encrypted email"
                  data
                  ::error-loading-encrypted-email]]}))
