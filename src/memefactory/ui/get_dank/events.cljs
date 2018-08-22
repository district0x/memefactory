(ns memefactory.ui.get-dank.events
  (:require [cljs-web3.eth :as web3-eth]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [memefactory.ui.contract.meme-factory :as meme-factory]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::get-initial-dank
 (fn [{:keys [db]} [_ {:keys [phone-number] :as data}]]
   (.log js/console "Sending initial DANK to:" phone-number)
   (let [tx-id (str (random-uuid))
         active-account (account-queries/active-account db)]
     (.log js/console "active account:" active-account)
     (.log js/console "instance:" (contract-queries/instance db :dank-faucet))
     {:dispatch [::tx-events/send-tx
                 {:instance (contract-queries/instance db :dank-faucet)
                  :fn :acquire-initial-dank
                  :args [phone-number]
                  :tx-opts {:from active-account
                            :gas 6000000}
                  :tx-id {:tx-id :dank-faucet/get-initial-dank}
                  :on-tx-success-n [[::logging/success [::create-meme]]
                                    [::notification-events/show
                                     "You've got DANK!"]]
                  :on-tx-hash-error [::logging/error [::dank-faucet]]
                  :on-tx-error [::logging/error [::dank-faucet]]}]})))
