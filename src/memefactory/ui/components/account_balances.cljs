(ns memefactory.ui.components.account-balances
  (:require
   [district.ui.component.active-account-balance :refer [active-account-balance]]
   [district.ui.component.tx-log :refer [tx-log]]
   [district.ui.web3-tx-log.events :as tx-log-events]
   [district.ui.web3-tx-log.subs :as tx-log-subs]
   [memefactory.ui.utils :as ui-utils]
   [re-frame.core :as re]))


(defn account-balances
  "Account Balances (with transactions) Component

  # Optional Arguments

  with-tx-logs? -- Determines whether to include the transactions log
  drop-down [default: false]

  app-bar-width? -- Determines whether to make the component slightly wider
  for situations when it's in the app-bar [default: false]

  "
  [{:keys [with-tx-logs? app-bar-width?]}]
  (let [tx-log-open? (re/subscribe [::tx-log-subs/open?])]
    (fn []
      [:div.accounts
       {:class (when app-bar-width? "app-bar-width")}
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
