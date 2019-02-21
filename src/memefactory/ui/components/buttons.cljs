(ns memefactory.ui.components.buttons
  (:require [re-frame.core :refer [subscribe dispatch]]
            [memefactory.ui.contract.registry-entry :as registry-entry]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [district.ui.component.form.input :refer [pending-button]]
            [district.graphql-utils :as graphql-utils]
            [district.ui.graphql.subs :as gql]
            [print.foo :refer [look] :include-macros true]))


(defn reclaim-buttons [active-account {:keys [:reg-entry/address :challenge/all-rewards :challenge/vote :challenge/vote-winning-vote-option :challenge/challenger :challenge/votes-against :challenge/votes-for] :as meme}]
  (let [ch-reward-tx-id (str address "challenge-reward")
        vote-reward-tx-id (str address "vote-reward")
        vote-amount-tx-id (str address "vote-amount")
        {:keys [:vote/option :vote/amount]} vote]
    (let [claim-vote-reward-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-vote-reward vote-reward-tx-id}])
          claim-vote-amount-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-vote-amount vote-amount-tx-id}])
          claim-vote-reward-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-vote-reward vote-reward-tx-id}])
          claim-vote-amount-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-vote-amount vote-amount-tx-id}])
          claim-challenge-reward-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-challenge-reward ch-reward-tx-id}])
          claim-challenge-reward-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-challenge-reward ch-reward-tx-id}])
          option (graphql-utils/gql-name->kw option)
          not-revealed? (= option :vote-option/not-revealed)]
      [:div
       (when vote
         (if not-revealed?
           [pending-button {:pending? @claim-vote-amount-tx-pending?
                            :disabled (or @claim-vote-amount-tx-pending? @claim-vote-amount-tx-success?)
                            :pending-text "Collecting ..."
                            :class "collect-amount"
                            :on-click (fn []
                                        (dispatch [::registry-entry/claim-vote-amount (look {:send-tx/id vote-amount-tx-id
                                                                                             :active-account active-account
                                                                                             :reg-entry/address address
                                                                                             :meme/title (:meme/title meme)})]))}

            (if @claim-vote-amount-tx-success?
              "Collected"
              "Reclaim amount")]

           (when vote-winning-vote-option
             [pending-button {:pending? @claim-vote-reward-tx-pending?
                              :disabled (or @claim-vote-reward-tx-pending? @claim-vote-reward-tx-success?)
                              :pending-text "Collecting ..."
                              :class "collect-reward"
                              :on-click (fn []
                                          (dispatch [::registry-entry/claim-vote-reward (look {:send-tx/id vote-reward-tx-id
                                                                                               :active-account active-account
                                                                                               :reg-entry/address address
                                                                                               :meme/title (:meme/title meme)})]))}
              (if @claim-vote-reward-tx-success?
                "Collected"
                "Vote reward")])))

       (when (and (= (:user/address challenger) active-account)
                  (> votes-against votes-for))
         [pending-button {:pending? @claim-challenge-reward-tx-pending?
                          :disabled (or (not (pos? (:challenge/reward-amount all-rewards)))
                                        @claim-challenge-reward-tx-pending? @claim-challenge-reward-tx-success?)
                          :pending-text "Collecting ..."
                          :on-click (fn []
                                      (dispatch [::registry-entry/claim-challenge-reward {:send-tx/id ch-reward-tx-id
                                                                                          :active-account active-account
                                                                                          :reg-entry/address address
                                                                                          :meme/title (:meme/title meme)}]))}
          (if @claim-challenge-reward-tx-success?
            "Collected"
            "Challenge Reward")])])))
