(ns memefactory.ui.contract.meme-token
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.ui.graphql.events :as gql-events]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3-tx.events :as tx-events]
    [goog.string :as gstring]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame :refer [reg-event-fx]]))

(def interceptors [re-frame/trim-v])

(reg-event-fx
 ::transfer-multi-and-start-auction
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :reg-entry/address :meme/title :meme-auction/token-ids :meme-auction/start-price :meme-auction/end-price :meme-auction/duration :meme-auction/description] :as args}]]
   (let [tx-name (str "Offer " title)
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-token)
                                      :fn :safe-transfer-from-multi
                                      :args [active-account
                                             (contract-queries/contract-address db :meme-auction-factory-fwd)
                                             token-ids
                                             (web3-eth/contract-get-data (contract-queries/instance db :meme-auction)
                                                                         :start-auction
                                                                         (web3/to-wei start-price :ether)
                                                                         (web3/to-wei end-price :ether)
                                                                         duration
                                                                         description)]
                                      :tx-opts {:from active-account}
                                      :tx-id {:meme-token/transfer-multi-and-start-auction id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.memefolio/index
                                                              :query {:tab "selling" :term title}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::transfer-multi]
                                                        [::notification-events/show (gstring/format "Offering for %s was successfully created" title)]
                                                        [::gql-events/query
                                                         {:query {:queries [[:meme {:reg-entry/address address}
                                                                             [:meme/total-supply
                                                                              [:meme/owned-meme-tokens {:owner active-account}
                                                                               [:meme-token/token-id]]]]]}}]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::transfer-multi-and-start-auction]}]})))


(reg-event-fx
 ::safe-transfer-from-multi
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :meme/title :meme-auction/token-ids :send/address] :as args}]]
   (let [tx-name (str "Send " title " to " address)
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-token)
                                      :fn :safe-transfer-from-multi
                                      :args (look [active-account
                                                   address
                                                   token-ids
                                                   nil])
                                      :tx-opts {:from active-account}
                                      :tx-id {:meme-token/safe-transfer-from-multi id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.meme-detail/index
                                                              :params {:address (:reg-entry/address args)}}}

                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::transfer-multi]
                                                        [::notification-events/show (gstring/format "%s tokens were successfully sent" title)]
                                                        [::gql-events/query
                                                         {:query {:queries [[:meme {:reg-entry/address (:reg-entry/address args)}
                                                                             [:meme/total-supply
                                                                              [:meme/owned-meme-tokens {:owner active-account}
                                                                               [:meme-token/token-id]]]]]}}]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::safe-transfer-from-multi]}]})))
