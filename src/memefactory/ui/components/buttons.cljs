(ns memefactory.ui.components.buttons
  (:require [re-frame.core :refer [subscribe dispatch]]
            [memefactory.ui.contract.registry-entry :as registry-entry]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [district.ui.component.form.input :refer [pending-button]]
            [district.graphql-utils :as graphql-utils]
            [district.ui.graphql.subs :as gql]))


(defn reclaim-buttons [active-account {:keys [:reg-entry/address :challenge/all-rewards] :as meme}]
  (let [ch-reward-tx-id (str address "challenge-reward")
        vote-reward-tx-id (str address "vote-reward")
        voting-sub (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                      [:reg-entry/address
                                                       [:challenge/vote-winning-vote-option {:vote/voter active-account}]
                                                       [:challenge/vote {:vote/voter active-account}
                                                        [:vote/option
                                                         :vote/amount]]]]]}])
        {:keys [:challenge/vote :challenge/vote-winning-vote-option]} (:meme @voting-sub)
        {:keys [:vote/option :vote/amount]} vote]
    (when-not (:graphql/loading? @voting-sub)
      (let [claim-vote-reward-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-vote-reward vote-reward-tx-id}])
            claim-vote-reward-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-vote-reward vote-reward-tx-id}])
            claim-challenge-reward-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-challenge-reward ch-reward-tx-id}])
            claim-challenge-reward-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-challenge-reward ch-reward-tx-id}])
            option (graphql-utils/gql-name->kw option)]

        [:div
         [pending-button {:pending? @claim-vote-reward-tx-pending?
                          :disabled (or @claim-vote-reward-tx-pending? @claim-vote-reward-tx-success?)
                          :pending-text "Collecting ..."
                          :on-click (fn []
                                      (dispatch [::registry-entry/claim-vote-reward {:send-tx/id vote-reward-tx-id
                                                                                     :active-account active-account
                                                                                     :reg-entry/address address
                                                                                     :meme/title (:meme/title meme)}]))}
          (cond
            @claim-vote-reward-tx-success? "Vote Collected"
            vote-winning-vote-option       "Vote Reward"
            vote                           "Collect Vote")]

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
            "Challenge Collected"
            "Challenge Reward")]]))))
