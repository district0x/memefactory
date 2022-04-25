(ns memefactory.ui.contract.meme-factory
  (:require [bignumber.core :as bn]
            [cljs-web3.eth :as web3-eth]
            [district.shared.error-handling :refer [try-catch]]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [memefactory.ui.utils :as utils]
            [goog.string :as gstring]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log :refer [spy]]))


(re-frame/reg-event-fx
 ::approve-and-create-meme
 (fn [{:keys [db]} [_ data ipfs-response]]
   (log/info "Meme meta uploaded" {:ipfs-response ipfs-response} ::approve-and-create-meme)
   (try-catch
    (let [[{:keys [Name Hash Size] :as meme-meta}] (utils/parse-ipfs-response ipfs-response)
          tx-id (:send-tx/id data)
          form-data (:form-data data)
          deposit (:deposit data)
          tx-name (gstring/format "%s submitted" (:title form-data))
          active-account (account-queries/active-account db)
          extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme-factory)
                                                 :create-meme
                                                 active-account
                                                 Hash
                                                 (bn/number (:issuance form-data)))]
      {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                       :fn :approve-and-call
                                       :args [(contract-queries/contract-address db :meme-factory)
                                              deposit
                                              extra-data]
                                       :tx-opts {:from active-account}
                                       :tx-id {::approve-and-create-meme tx-id}
                                       :tx-log {:name tx-name
                                                :related-href {:name :route.memefolio/index
                                                               :query {:tab "created" :term (:title form-data)}}}
                                       :on-tx-success-n [[::logging/info (str tx-name " tx success") meme-meta ::create-meme]
                                                         [::notification-events/show (gstring/format "%s was successfully submitted" (:title form-data))]
                                                         [::creation-success]]
                                       :on-tx-error [::logging/error
                                                     (str tx-name " tx error")
                                                     {:user {:id active-account}
                                                      :data form-data
                                                      :meme-meta meme-meta} ::create-meme]}]}))))


(re-frame/reg-event-db
  ::creation-success
  (fn [db _] db))
