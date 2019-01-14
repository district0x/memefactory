(ns memefactory.ui.contract.meme-factory
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.ui.logging.events :as logging]
   [district.ui.notification.events :as notification-events]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [goog.string :as gstring]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   ))

(re-frame/reg-event-fx
 ::approve-and-create-meme
 (fn [{:keys [db]} [_ data deposit {:keys [Name Hash Size] :as meme-meta}]]
   (log/info "Meme meta uploaded with hash" {:hash Hash} ::approve-and-create-meme)
   (let [tx-id (str (random-uuid))
         tx-name (gstring/format "Meme %s submitted" (:title data))
         active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme-factory)
                                                :create-meme
                                                active-account
                                                Hash
                                                (bn/number (:issuance data)))]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args [(contract-queries/contract-address db :meme-factory)
                                             deposit
                                             extra-data]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/create-meme tx-id}
                                      :tx-log {:name tx-name}
                                      :on-tx-hash-n [[::notification-events/show (gstring/format "Your meme was successfully submitted")]]
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::create-meme]]
                                      :on-tx-error [::logging/error (str tx-name " tx error") {:user {:id active-account}
                                                                                               :deposit deposit
                                                                                               :data data
                                                                                               :meme-meta meme-meta} ::create-meme]}]})))
