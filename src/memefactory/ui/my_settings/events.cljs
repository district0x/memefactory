(ns memefactory.ui.my-settings.events
  (:require
   [ajax.core :as ajax]
   [cljs-web3.core :as web3]
   [clojure.string :as string]
   [day8.re-frame.http-fx]
   [district.ui.logging.events :as logging]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [district0x.re-frame.web3-fx]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   ))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
 ::load-email-settings
 (fn [{:keys [db]} _]
   (let [active-account (account-queries/active-account db)
         instance (contract-queries/instance db :district0x-emails)]
     (when (and active-account instance)
       {:web3/call {:web3 (:web3 db)
                    :fns [{:instance instance
                           :fn :get-email
                           :args [active-account]
                           :on-success [::encrypted-email-found active-account]
                           :on-error [::logging/error "Error loading user encrypted email"
                                      {:user {:id active-account}}
                                      ::load-email-settings]}]}}))))

(re-frame/reg-event-db
 ::encrypted-email-found
 interceptors
 (fn [db [address encrypted-email]]
   (if (or (not encrypted-email)
           (string/blank? encrypted-email))
     (do
       (log/info "No encrypted email found for user" {:user {:id address}
                                                      :encrypted-email encrypted-email})
       db)
     (do (log/info "Loaded user encrypted email" {:user {:id address}
                                                  :encrypted-email encrypted-email})
         (assoc-in db [:settings address :encrypted-email] encrypted-email)))))
