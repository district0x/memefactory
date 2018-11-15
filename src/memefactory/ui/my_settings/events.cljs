(ns memefactory.ui.my-settings.events
  (:require [re-frame.core :as re-frame]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [goog.string :as gstring]
            [district.encryption :as encryption]))


(re-frame/reg-event-fx
 ::save-settings
 (fn [{:keys [db]} [_ public-key {:keys [email]}]]
   (.log js/console "About to encrypt " email " with " public-key)
   (let [active-account (account-queries/active-account db)
         encrypted-email (encryption/encrypt-encode public-key email)]
     (.log js/console "Encripted " email " into " encrypted-email)
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :district0x-emails)
                                      :fn :set-email
                                      :args [encrypted-email]
                                      :tx-opts {:from active-account
                                                :gas 600000}
                                      :on-tx-success-n [[::logging/info "Settings saved" ::save-settings]
                                                        [::notification-events/show (gstring/format "Settings saved")]]
                                      :on-tx-error [::logging/error "Error saving settings" {}
                                                    ::save-settings]}]})))
