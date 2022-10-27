(ns memefactory.ui.contract.meme-auction
  (:require
    [cljs-web3-next.core :as web3]
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
 ::buy
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :meme-auction/address :meme/title :value] :as args}]]
   (let [tx-name (str (gstring/format "Buy %s for %.2f MATIC"
                                      title
                                      (web3/from-wei (str value) :ether)))
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-auction address)
                                      :fn :buy
                                      :args []
                                      :tx-opts {:from active-account
                                                :value value}
                                      :tx-id {:meme-auction/buy id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.memefolio/index
                                                              :query {:tab "collected" :term title}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::buy]
                                                        [::notification-events/show (gstring/format "You successfully bought %s" title)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :value value}
                                                    ::buy]}]})))


(reg-event-fx
 ::cancel
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :meme-auction/address :meme/title] :as args}]]
   (let [tx-name (gstring/format "Cancel offering %s" title)
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-auction address)
                                      :fn :cancel
                                      :args []
                                      :tx-opts {:from active-account}
                                      :tx-id {:meme-auction/cancel id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.memefolio/index
                                                              :query {:tab "collected" :term title}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::cancel]
                                                        [::notification-events/show (gstring/format "%s offering canceled" title)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::cancel]}]})))
