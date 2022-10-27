(ns memefactory.ui.components.account-balances
  (:require
   [district.ui.component.active-account-balance :refer [active-account-balance]]
   [district.ui.component.tx-log :refer [tx-log]]
   [district.ui.web3-chain.subs :as chain-subs]
   [district.ui.web3-tx-log.events :as tx-log-events]
   [district.ui.web3-tx-log.subs :as tx-log-subs]
   [memefactory.ui.config :refer [config-map]]
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
  (let [tx-log-open? (re/subscribe [::tx-log-subs/open?])
        chain (re/subscribe [::chain-subs/chain])
        chain-root (get-in config-map [:web3-chain :l1 :chain-id])
        chain-child (get-in config-map [:web3-chain :l2 :chain-id])]
    (fn []
      [:div.accounts
       {:class (when app-bar-width? "app-bar-width")}
       [:div.dank-section
        {:on-click (when with-tx-logs? #(re/dispatch [::tx-log-events/set-open (not @tx-log-open?)]))}
        [:div.dank-logo
         [:img {:src "/assets/icons/dank-logo.svg"}]]
        (if (or (= @chain chain-root) (= @chain chain-child))
        [active-account-balance
         {:token-code :DANK
          :contract (if (= @chain chain-root) :DANK-root :DANK)
          :class "dank"
          :locale "en-US"
          :max-fraction-digits 0}]
        [:div.active-account-balance
         [:span.balance "N/A"] " " [:span.token-code :DANK]])]
       [:div.eth-section
        {:on-click (when with-tx-logs? #(re/dispatch [::tx-log-events/set-open (not @tx-log-open?)]))}
        [:div.eth-logo
         [:img {:src "/assets/icons/ethereum.svg"}]]
        [active-account-balance
         {:token-code (if (= @chain chain-root) :ETH :MATIC)
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
