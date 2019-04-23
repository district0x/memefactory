(ns memefactory.ui.contract.registry-entry
  (:require
   [akiroz.re-frame.storage :as storage]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]
   [cljs-web3.eth :as web3-eth]
   [cljs.spec.alpha :as s]
   [district.cljs-utils :as cljs-utils]
   [district.ui.logging.events :as logging]
   [district.ui.notification.events :as notification-events]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [goog.string :as gstring]
   [memefactory.shared.contract.registry-entry :as reg-entry]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]))

(def interceptors [re-frame/trim-v])

(storage/reg-co-fx! :my-app         ;; local storage key
                    {:fx :store     ;; re-frame fx ID
                     :cofx :store}) ;; re-frame cofx ID


(re-frame/reg-event-fx
 ::approve-and-create-challenge
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :meme/title :send-tx/id :deposit] :as args} {:keys [Hash] :as challenge-meta}]]
   (log/info "Challenge meta created with hash" {:hash Hash} ::approve-and-create-challenge)
   (let [tx-name (gstring/format "Challenge %s" title)
         active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme address)
                                                :create-challenge
                                                active-account
                                                Hash)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args [address
                                             deposit
                                             extra-data]
                                      :tx-opts {:from active-account}
                                      :tx-id {::approve-and-create-challenge id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.meme-detail/index
                                                              :params {:address address}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::approve-and-create-challenge]
                                                        [::notification-events/show (gstring/format "Challenge created for %s" title)]
                                                        [::challenge-success]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args
                                                     :challenge-meta challenge-meta}
                                                    ::approve-and-create-challenge]}]})))


(re-frame/reg-event-db
 ::challenge-success
 (fn [db _] db))


(re-frame/reg-event-fx
 ::approve-and-commit-vote
 [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]} [_ {:keys [:reg-entry/address :send-tx/id :vote/amount :vote/option :meme/title] :as args}
                          {:keys [Hash] :as challenge-meta}]]
   (let [tx-name (gstring/format "Vote %s for %s"
                                 (if (= option :vote.option/vote-against)
                                   "stank"
                                   "dank")
                                 title)
         active-account (account-queries/active-account db)
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
                                      :tx-opts {:from active-account}
                                      :tx-id {::approve-and-commit-vote id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.meme-detail/index
                                                              :params {:address address}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::approve-and-commit-vote]
                                                        [::notification-events/show
                                                         (gstring/format "Successfully voted %s for %s"
                                                                         (if (= option :vote.option/vote-against)
                                                                           "stank"
                                                                           "dank")
                                                                         title)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args
                                                     :challenge-meta challenge-meta}
                                                    ::approve-and-commit-vote]}]
      :store (assoc-in store [:votes active-account address] {:option option :salt salt})
      :db (assoc-in db [:votes active-account address] {:option option :salt salt})})))


(re-frame/reg-event-fx
 ::reveal-vote
 [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]} [_ {:keys [:reg-entry/address :send-tx/id :meme/title] :as args} {:keys [Hash] :as challenge-meta}]]
   (let [tx-name (gstring/format "Reveal vote for %s" title)
         active-account (account-queries/active-account db)
         {:keys [option salt]} (get-in store [:votes active-account address])]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :reveal-vote
                                      :args [(reg-entry/vote-option->num option) salt]
                                      :tx-opts {:from active-account}
                                      :tx-id {::reveal-vote id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.meme-detail/index
                                                              :params {:address address}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::reveal-vote]
                                                        [::notification-events/show (gstring/format "Successfully revealed %s for %s"
                                                                                                    (if (= option :vote.option/vote-against)
                                                                                                      "stank"
                                                                                                      "dank")
                                                                                                    title)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args
                                                     :challenge-meta challenge-meta}
                                                    ::reveal-vote]}]})))


(re-frame/reg-event-fx
 ::claim-rewards
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :reg-entry/address :from :meme/title] :as args}]]
   (let [tx-name (gstring/format "Claim rewards %s" title)
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :claim-rewards
                                      :args [active-account]
                                      :tx-opts {:from active-account}
                                      :tx-id {::claim-rewards id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.meme-detail/index
                                                              :params {:address address}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::claim-rewards]
                                                        [::notification-events/show (gstring/format "Successfully claimed rewards %s" title)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::claim-rewards]}]})))


(re-frame/reg-event-fx
 ::claim-vote-amount
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :reg-entry/address :meme/title] :as args}]]
   (let [tx-name (gstring/format "Reclaim votes from %s" title)
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :reclaim-vote-amount
                                      :args [active-account]
                                      :tx-opts {:from active-account}
                                      :tx-id {::claim-vote-amount id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.meme-detail/index
                                                              :params {:address address}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::reclaim-vote-amount]
                                                        [::notification-events/show (gstring/format "Succesfully reclaimed votes from %s" title)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:is active-account}
                                                     :args args}
                                                    ::reclaim-vote-amount]}]})))
