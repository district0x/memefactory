(ns memefactory.ui.contract.meme
  (:require
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
 ::mint
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :meme/title :reg-entry/address :meme/amount] :as args}]]
   (let [tx-name (gstring/format "Mint %d cards of %s" amount title)
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :mint
                                      :args [amount]
                                      :tx-opts {:from active-account}
                                      :tx-id {:meme/mint id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.memefolio/index,
                                                              :query {:tab "collected" :term title}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::mint]
                                                        [::notification-events/show (gstring/format " %s successfully minted" title)]
                                                        [::gql-events/query {:query {:queries [[:meme {:reg-entry/address address}
                                                                                                [:meme/total-minted]]]}}]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::mint]}]})))
