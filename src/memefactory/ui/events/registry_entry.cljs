(ns memefactory.ui.events.registry-entry
  (:require [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs.spec.alpha :as s]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [district0x.re-frame.spec-interceptors :as spec-interceptors]
            [goog.string :as gstring]
            [print.foo :refer [look] :include-macros true]
            [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]))

(def interceptors [re-frame/trim-v])

(reg-event-db
 ::claim-vote-reward-success
 (fn [db _]      
   db))

(reg-event-fx
 ::claim-vote-reward
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :reg-entry/address :from] :as args}]]
   (let [active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (look (contract-queries/instance db :meme address))
                                      :fn :claim-vote-reward
                                      :args [from]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:registry-entry/claim-vote-reward id}
                                      :on-tx-success-n [[::logging/success [::claim-vote-reward]]
                                                        [::notification-events/show (gstring/format "Succesfully claimed reward from %s" from)]
                                                        [::claim-vote-reward-success]]
                                      :on-tx-hash-error [::logging/error [::claim-vote-reward]]
                                      :on-tx-error-n [[::logging/error [::claim-vote-reward]]
                                                      #_[::claim-vote-reward-success]]}]})))
