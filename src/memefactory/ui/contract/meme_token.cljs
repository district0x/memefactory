(ns memefactory.ui.contract.meme-token
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
            [re-frame.core :as re-frame :refer [reg-event-fx]]
            [district.ui.graphql.events :as gql-events]))

(def interceptors [re-frame/trim-v])

(reg-event-fx
 ::transfer-multi-and-start-auction
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :reg-entry/address :meme/title :meme-auction/token-ids :meme-auction/start-price :meme-auction/end-price :meme-auction/duration :meme-auction/description] :as args}]]
   (let [tx-name (str "Offered on market " title)
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
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme-token/transfer-multi-and-start-auction id}
                                      :tx-log {:name tx-name}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::transfer-multi]
                                                        [::notification-events/show (gstring/format "Offering for %s was successfully created" title)]
                                                        [::gql-events/query {:query {:queries [[:meme {:reg-entry/address address}
                                                                                                [:meme/total-supply
                                                                                                 [:meme/owned-meme-tokens {:owner active-account}
                                                                                                  [:meme-token/token-id]]]]]}}]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::transfer-multi-and-start-auction]}]})))
