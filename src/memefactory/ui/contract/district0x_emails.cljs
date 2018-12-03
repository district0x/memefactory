(ns memefactory.ui.contract.district0x-emails
  (:require
   [district.encryption :as encryption]
   [district.ui.logging.events :as logging]
   [district.ui.notification.events :as notification-events]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [goog.string :as gstring]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   ))

(re-frame/reg-event-fx
 ::save-settings
 (fn [{:keys [db]} [_ public-key {:keys [email] :as args}]]
   (log/debug  (str "About to encrypt " email " with " public-key) ::save-settings)
   (let [tx-id (str (random-uuid))
         tx-name "Set email"
         active-account (account-queries/active-account db)
         encrypted-email (encryption/encrypt-encode public-key email)]
     (log/debug (str "Encrypted " email " into " encrypted-email) ::save-settings)
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :district0x-emails)
                                      :fn :set-email
                                      :args [encrypted-email]
                                      :tx-opts {:from active-account
                                                :gas 600000}
                                      :tx-id {:district0x-emails/set-email tx-id}
                                      :tx-log {:name tx-name}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::save-settings]
                                                        [::notification-events/show (gstring/format "Settings saved")]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args
                                                     :public-key public-key
                                                     :encrypted-email encrypted-email}
                                                    ::save-settings]}]})))
