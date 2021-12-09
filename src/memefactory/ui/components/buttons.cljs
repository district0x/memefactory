(ns memefactory.ui.components.buttons
  (:require
    [district.graphql-utils :as graphql-utils]
    [district.ui.component.form.input :refer [pending-button]]
    [district.ui.graphql.subs :as gql]
    [district.ui.web3-chain.events :as chain-events]
    [district.ui.web3-chain.subs :as chain-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [memefactory.ui.config :refer [config-map]]
    [memefactory.ui.contract.registry-entry :as registry-entry]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :refer [subscribe dispatch]]))


(defn chain-check-pending-button
  [{:keys [:for-chain] :as opts
    :or {for-chain (get-in config-map [:web3-chain (get-in config-map [:web3-chain :deployed-on]) :chain-id])}} & children]
  (let [chain (subscribe [::chain-subs/chain])
        disabled (:disabled opts)]
    [pending-button (merge (dissoc opts :for-chain) {:disabled (or (not= @chain for-chain) disabled)}) children]))

(defn switch-chain-button
  [{:keys [:net]
    :or {net (get-in config-map [:web3-chain :deployed-on])}}]
  (let [chain (subscribe [::chain-subs/chain])
        for-chain (get-in config-map [:web3-chain net :chain-id])]
    (when (and @chain (not= @chain for-chain))
      [:button {:on-click #(dispatch [::chain-events/request-switch-chain
                                      for-chain
                                    {:chain-info (get-in config-map [:web3-chain net])}])
                :class "switch-chain-button"}
       (str "Switch to " (get-in config-map [:web3-chain net :chain-name]))])
    ))

(defn reclaim-buttons [active-account {:keys [:reg-entry/address :challenge/all-rewards :challenge/vote :challenge/vote-winning-vote-option :challenge/challenger :challenge/votes-against :challenge/votes-for] :as meme}]
  (let [rewards-tx-id (str address "rewards")
        vote-amount-tx-id (str address "vote-amount")
        {:keys [:vote/option :vote/amount]} vote]
    (let [claim-vote-amount-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-vote-amount vote-amount-tx-id}])
          claim-vote-amount-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-vote-amount vote-amount-tx-id}])
          claim-rewards-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-rewards rewards-tx-id}])
          claim-rewards-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-rewards rewards-tx-id}])
          option (graphql-utils/gql-name->kw option)
          not-revealed? (= option :vote-option/not-revealed)
          reward-amount (+ (:challenge/reward-amount all-rewards) (:vote/reward-amount all-rewards))]
      [:div
       (when (pos? reward-amount)
         [chain-check-pending-button {:pending? @claim-rewards-tx-pending?
                          :disabled (or @claim-rewards-tx-pending? @claim-rewards-tx-success?)
                          :pending-text [:div.label
                                         [:span "Claiming"]
                                         [:img {:src "/assets/icons/dank-logo.svg"}]
                                         [:span "..."]]
                          :class "collect-amount"
                          :on-click (fn []
                                      (dispatch [::registry-entry/claim-rewards
                                                 {:send-tx/id rewards-tx-id
                                                  :active-account active-account
                                                  :reg-entry/address address
                                                  :meme/title (:meme/title meme)}]))}
          (if @claim-rewards-tx-success?
            [:div.label
             [:span "Claimed"]
             [:img {:src "/assets/icons/dank-logo.svg"}]]

            [:div.label
             [:span "Claim"]
             [:img {:src "/assets/icons/dank-logo.svg"}]
             [:span "Reward"]])])

       (when (and vote not-revealed?)
         [chain-check-pending-button {:pending? @claim-vote-amount-tx-pending?
                          :disabled (or @claim-vote-amount-tx-pending? @claim-vote-amount-tx-success?)
                          :pending-text [:div.label
                                         [:span "Reclaiming"]
                                         [:img {:src "/assets/icons/dank-logo.svg"}]
                                         [:span "..."]]
                          :class "collect-amount"
                          :on-click (fn []
                                      (dispatch [::registry-entry/claim-vote-amount
                                                 {:send-tx/id vote-amount-tx-id
                                                  :active-account active-account
                                                  :reg-entry/address address
                                                  :meme/title (:meme/title meme)}]))}

          (if @claim-vote-amount-tx-success?
            [:div.label
             [:span "Reclaimed"]
             [:img {:src "/assets/icons/dank-logo.svg"}]]

            [:div.label
             [:span "Reclaim"]
             [:img {:src "/assets/icons/dank-logo.svg"}]
             [:span "Votes"]])])])))
