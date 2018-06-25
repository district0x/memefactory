(ns memefactory.ui.events.meme-auction
  (:require
   [cljs-web3.core :as web3]
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
   [re-frame.core :as re-frame :refer [reg-event-fx]]))

(def interceptors [re-frame/trim-v])

(reg-event-fx
 :meme-auction/buy
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :meme-auction/address :value] :as args}]]
   (let [active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance address
                                      :fn :buy
                                      :args (look [active-account
                                                   address
                                                   value])
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme-auction/buy id}
                                      :on-tx-success-n [[::logging/success [:meme-auction/buy]]
                                                        [::notification-events/show (gstring/format "Auction %s is now yours" address)]]
                                      :on-tx-hash-error [::logging/error [:meme-auction/buy]]
                                      :on-tx-error [::logging/error [:meme-auction/buy]]}]})))
