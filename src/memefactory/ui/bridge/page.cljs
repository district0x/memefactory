(ns memefactory.ui.bridge.page
  (:require
    [cljs-web3.core :as web3]
    [clojure.string :as str]
    [district.parsers :as parsers]
    [district.ui.component.form.input :refer [index-by-type file-drag-input with-label chip-input text-input int-input pending-button]]
    [district.ui.component.page :refer [page]]
    [district.ui.web3-account-balances.subs :as balance-subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [memefactory.ui.components.buttons :refer [chain-check-pending-button]]
    [memefactory.ui.components.general :refer [nav-anchor dank-with-logo]]
    [memefactory.ui.components.tiles :refer [meme-image]]
    [memefactory.ui.config :refer [config-map]]
    [memefactory.ui.contract.bridge :as bridge-contracts]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.ratom :refer [reaction]]
    [taoensso.timbre :as log :refer [spy]]
    [district.format :as format]))


(defn header []
    (fn []
      [:section.bridge-header
       [:div.icon]
       [:h2.title (str "Bridge tokens")]
       [:h3.title "Bring your DANK tokens from Ethereum to Polygon network"]]
       ))

(defn bridge-panels []
  (let [account-balance (subscribe [::balance-subs/active-account-balance :DANK-root])
        form-data (r/atom {:amount 1})
        errors (reaction {:local (let [{:keys [amount]} @form-data
                                       max-amount (or (/ @account-balance 1e18) 1)]
                                   (cond-> {:amount {:hint (str "Max " (format/format-number max-amount {:max-fraction-digits 0}))}}
                                           (not (try
                                                  (let [amount (parsers/parse-int amount)]
                                                    (and (< 0 amount) (<= amount max-amount)))
                                                  (catch js/Error e nil)))
                                           (assoc-in [:amount :error] (str "Amount should be a number between 1 and " (format/format-number max-amount {:max-fraction-digits 0})))))})
        critical-errors (reaction (index-by-type @errors :error))
        active-account (subscribe [::accounts-subs/active-account])
        tx-id (str @active-account "bridge-dank-to-l2")
        bridge-tx-pending? (subscribe [::tx-id-subs/tx-pending? {:bridge-contracts/approve-and-bridge-dank-to-l2 tx-id}])
        bridge-tx-success? (subscribe [::tx-id-subs/tx-success? {:bridge-contracts/approve-and-bridge-dank-to-l2 tx-id}])
        ]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory - Bridge Tokens"
               :description "Bridge tokens from/to ethereum mainnet. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
       [:div.bridge-page
        [:div.bridge-box
         [header]
         [:p "You can bring the DANK tokens you have on Ethereum Mainnet to Polygon. Select the amount of tokens you want to bridge. Once the transaction succeed, switch your wallet to Polygon Mainnet where you'll receive your DANK in a few minutes."]
         [:div.form-panel
          [with-label "Amount"
           [text-input {:form-data form-data
                        :errors errors
                        :id :amount
                        :dom-id :amount
                        :type :number
                        :min 1}]
           {:form-data form-data
            :id :amount
            :for :amount}]
          [:div.submit
           [chain-check-pending-button {:for-chain (get-in config-map [:web3-chain :l1 :chain-id])
                            :pending? @bridge-tx-pending?
                            :pending-text "Bridging tokens"
                            :disabled (or (not (empty? @critical-errors)) @bridge-tx-pending? @bridge-tx-success? (not @active-account))
                            :class (when-not @bridge-tx-success? "bridge-dank-to-l2")
                            :on-click (fn [e]
                                        (.stopPropagation e)
                                        (dispatch [::bridge-contracts/approve-and-bridge-dank-to-l2 {:send-tx/id tx-id
                                                                                          :to @active-account
                                                                                          :amount (min @account-balance ; deal with rounding
                                                                                                       (* (:amount @form-data) 1e18))}]))}
            (if @bridge-tx-success? "Tokens Bridged" "Bridge Tokens")]
           ]
          (when (< @account-balance 1)
            [:div.not-enough-dank "You don't have DANK tokens to bridge"])

          ]
         [:div.footer "Make sure your wallet is connected to Ethereum Mainnet network"]
         ]]])))


(defmethod page :route.bridge/index []
  [bridge-panels])
