(ns memefactory.ui.get-dank.events
  (:require [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [memefactory.ui.contract.meme-factory :as meme-factory]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::get-initial-dank
 (fn [_ [_ {:keys [phone-number] :as data}]]
   (.log js/console "Sending initial DANK to:" phone-number)
   ))
