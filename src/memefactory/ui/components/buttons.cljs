(ns memefactory.ui.components.buttons
  (:require [re-frame.core :refer [subscribe dispatch]]
            [memefactory.ui.contract.registry-entry :as registry-entry]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [district.ui.component.form.input :refer [pending-button]]
            [district.graphql-utils :as graphql-utils]
            [district.ui.graphql.subs :as gql]
            [print.foo :refer [look] :include-macros true]))


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
         [pending-button {:pending? @claim-rewards-tx-pending?
                          :disabled (or @claim-rewards-tx-pending? @claim-rewards-tx-success?)
                          :pending-text [:div.label
                                         [:span "Claiming"]
                                         [:img {:src "/assets/icons/dank-logo.svg"}]
                                         [:span "..."]]
                          :class "collect-amount"
                          :on-click (fn []
                                      (dispatch [::registry-entry/claim-rewards {:send-tx/id rewards-tx-id
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
         [pending-button {:pending? @claim-vote-amount-tx-pending?
                          :disabled (or @claim-vote-amount-tx-pending? @claim-vote-amount-tx-success?)
                          :pending-text [:div.label
                                         [:span "Reclaiming"]
                                         [:img {:src "/assets/icons/dank-logo.svg"}]
                                         [:span "..."]]
                          :class "collect-amount"
                          :on-click (fn []
                                      (dispatch [::registry-entry/claim-vote-amount (look {:send-tx/id vote-amount-tx-id
                                                                                           :active-account active-account
                                                                                           :reg-entry/address address
                                                                                           :meme/title (:meme/title meme)})]))}

          (if @claim-vote-amount-tx-success?
            [:div.label
             [:span "Reclaimed"]
             [:img {:src "/assets/icons/dank-logo.svg"}]]

            [:div.label
             [:span "Reclaim"]
             [:img {:src "/assets/icons/dank-logo.svg"}]
             [:span "Votes"]])])])))
