(ns memefactory.ui.contract.bridge
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
    [clojure.set :as set]
    [memefactory.shared.contract.registry-entry :refer [vote-options vote-option->num]]
    [re-frame.core :as re-frame]
    [memefactory.ui.utils :as utils]
    [taoensso.timbre :as log]
    [district.format :as format]))

(def interceptors [re-frame/trim-v])

(defn format-dank [amount]
;  (/ amount 1e18))
(format/format-number (/ amount 1e18) {:max-fraction-digits 0}))

(re-frame/reg-event-fx
 ::approve-and-bridge-dank-to-l2
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :to :amount] :as args}]]
   (let [tx-name (gstring/format "Bridging %s DANK to L2 account %s" (format-dank amount) to)
         address (contract-queries/contract-address db :DANK-root-tunnel)
         active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :DANK-root-tunnel address)
                                                :deposit-from
                                                to
                                                to
                                                amount)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK-root)
                                      :fn :approve-and-call
                                      :args [address
                                             amount
                                             extra-data]
                                      :tx-opts {:from active-account}
                                      :tx-id {::approve-and-commit-vote id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.meme-detail/index
                                                              :params {:address address}}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::approve-and-bridge-dank-to-l2]
                                                        [::notification-events/show
                                                         (gstring/format "Successfully bridged %s DANK to L2 account %s" (format-dank amount) to)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::approve-and-bridge-dank-to-l2]}]})))

