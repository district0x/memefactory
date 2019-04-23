(ns memefactory.ui.components.account-balances
  (:require
   [district.ui.component.active-account-balance :refer [active-account-balance]]
   [district.ui.component.tx-log :refer [tx-log]]
   [district.ui.web3-tx-log.events :as tx-log-events]
   [district.ui.web3-tx-log.subs :as tx-log-subs]
   [memefactory.ui.utils :as ui-utils]
   [re-frame.core :as re]))


(defn account-balances [{:keys [with-tx-logs?]}]
  (let [tx-log-open? (re/subscribe [::tx-log-subs/open?])]
    (fn []
      [:div.accounts
       [:div.dank-section
        {:on-click (when with-tx-logs? #(re/dispatch [::tx-log-events/set-open (not @tx-log-open?)]))}
        [:div.dank-logo
         [:img {:src "/assets/icons/dank-logo.svg"}]]
        [active-account-balance
         {:token-code :DANK
          :contract :DANK
          :class "dank"
          :locale "en-US"
          :max-fraction-digits 0}]]
       [:div.eth-section
        {:on-click (when with-tx-logs? #(re/dispatch [::tx-log-events/set-open (not @tx-log-open?)]))}
        [:div.eth-logo
         [:img {:src "/assets/icons/ethereum.svg"}]]
        [active-account-balance
         {:token-code :ETH
          :locale "en-US"
          :class "eth"}]]
       (when with-tx-logs?
         [tx-log
          {:header-props {:text "Transaction Log"}
           :transactions-props
           {:transaction-props
            {:tx-value-el
             (fn [{:keys [tx]}]
               [:span.tx-value (ui-utils/format-price (:value tx))])}}}])])))
