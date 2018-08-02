(ns memefactory.ui.contract.registry-entry
  (:require [akiroz.re-frame.storage :as storage]
            [cljs-solidity-sha3.core :refer [solidity-sha3]]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs.spec.alpha :as s]
            [district.cljs-utils :as cljs-utils]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [district0x.re-frame.spec-interceptors :as spec-interceptors]
            [goog.string :as gstring]
            [memefactory.shared.contract.registry-entry :as reg-entry]
            [print.foo :refer [look] :include-macros true]
            [re-frame.core :as re-frame]))

(def interceptors [re-frame/trim-v])

(storage/reg-co-fx! :my-app         ;; local storage key
                    {:fx :store     ;; re-frame fx ID
                     :cofx :store}) ;; re-frame cofx ID

(re-frame/reg-event-fx
 ::approve-and-create-challenge
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :send-tx/id :deposit]} {:keys [Hash]}]]
   (prn "Challenge meta created with hash " Hash)
   (let [active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme address)
                                                :create-challenge
                                                active-account
                                                Hash)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args [address
                                             deposit
                                             extra-data]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {::approve-and-create-challenge id}
                                      :on-tx-success-n [[::logging/success [::approve-and-create-challenge]]
                                                        [::notification-events/show (gstring/format "Challenge created for %s with metahash %s"
                                                                                                    address Hash)]]
                                      :on-tx-hash-error [::logging/error [::approve-and-create-challenge]]
                                      :on-tx-error [::logging/error [::approve-and-create-challenge]]}]})))

(re-frame/reg-event-fx
 ::approve-and-commit-vote
 [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]} [_ {:keys [:reg-entry/address :send-tx/id :vote/amount :vote/option]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)
         salt (cljs-utils/rand-str 5)
         secret-hash (solidity-sha3 (reg-entry/vote-option->num option) salt)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme address)
                                                :commit-vote
                                                active-account
                                                amount
                                                secret-hash)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args [address
                                             amount
                                             extra-data]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {::approve-and-commit-vote id}
                                      :on-tx-success-n [[::logging/success [::approve-and-commit-vote]]
                                                        [::notification-events/show "Voted"]]
                                      :on-tx-hash-error [::logging/error [::approve-and-commit-vote]]
                                      :on-tx-error [::logging/error [::approve-and-commit-vote]]}]
      :store (assoc-in store [:votes active-account address] {:option option :salt salt})})))

(re-frame/reg-event-fx
 ::reveal-vote
 [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]} [_ {:keys [:reg-entry/address :send-tx/id]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)
         {:keys [option salt]} (get-in store [:votes active-account address])]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :reveal-vote
                                      :args (look [(reg-entry/vote-option->num option) salt])
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {::reveal-vote id}
                                      :on-tx-success-n [[::logging/success [::reveal-vote]]
                                                        [::notification-events/show "Voted"]]
                                      :on-tx-hash-error [::logging/error [::reveal-vote]]
                                      :on-tx-error [::logging/error [::reveal-vote]]}]})))

(re-frame/reg-event-fx
 ::claim-vote-reward
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :reg-entry/address :from] :as args}]]
   (let [active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :claim-vote-reward
                                      :args [from]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {::claim-vote-reward id}
                                      :on-tx-success-n [[::logging/success [::claim-vote-reward]]
                                                        [::notification-events/show (gstring/format "Succesfully claimed reward from %s" from)]]
                                      :on-tx-hash-error [::logging/error [::claim-vote-reward]]
                                      :on-tx-error [::logging/error [::claim-vote-reward]]}]})))
