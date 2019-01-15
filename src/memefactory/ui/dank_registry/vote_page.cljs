(ns memefactory.ui.dank-registry.vote-page
  (:require
   [cljs-time.core :as t]
   [cljs-time.extend]
   [cljs-web3.core :as web3]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.time :as time]
   [district.ui.component.form.input :refer [select-input with-label text-input amount-input pending-button]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-account-balances.subs :as balance-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.challenge-list :refer [challenge-list]]
   [memefactory.ui.components.charts :as charts]
   [memefactory.ui.components.panes :refer [tabbed-pane]]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [memefactory.ui.dank-registry.events :as dr-events]
   [district.ui.router.events :as router-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log]))

(def page-size 12)

(defn header []
  [:div.registry-vote-header
   [:div.icon]
   [:h2.title "Dank registry - VOTE"]
   [:h3.title "Lorem ipsum dolor sit ..."]
   [:div.get-dank-button {:on-click #(dispatch [::router-events/navigate :route.get-dank/index nil nil])}
    "Get Dank"]])

(defn collect-reward-action [{:keys [:reg-entry/address :challenge/all-rewards] :as meme}]
  (let [active-account (subscribe [::accounts-subs/active-account])]
    (when @active-account
      (let [response (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                         [:reg-entry/address
                                                          :challenge/votes-for
                                                          :challenge/votes-against
                                                          :challenge/votes-total]]]}])]
        (when-not (:graphql/loading? @response)
          (if-let [meme-voting (:meme @response)]
            (let [ch-reward-tx-id (str address "challenge-reward")
                  vote-reward-tx-id (str address "vote-reward")
                  {:keys [:challenge/reward-amount :vote/reward-amount]} all-rewards
                  {:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total]} meme-voting
                  claim-vote-reward-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-vote-reward vote-reward-tx-id}])
                  claim-vote-reward-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-vote-reward vote-reward-tx-id}])
                  claim-challenge-reward-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-challenge-reward ch-reward-tx-id}])
                  claim-challenge-reward-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-challenge-reward ch-reward-tx-id}])]
              [:div.collect-reward
               (log/debug "meme voting" meme-voting ::collect-reward-action)

               (if (pos? votes-total)
                 [charts/donut-chart meme-voting]
                 [:div "No votes"])
               [:ul.vote-info
                (when (pos? votes-for)
                  [:li
                  (str "Voted Dank: " (format/format-percentage votes-for votes-total) " - " (/ votes-for 1e18))])
                (when (pos? votes-against)
                  [:li
                   (str "Voted Stank: " (format/format-percentage votes-against votes-total) " - " (/ votes-against 1e18))])
                (when (pos? votes-total)
                  [:li "Total voted: " (gstring/format "%d" (/ votes-total 1e18))])
                [:li "Your reward: " (let [reward (+ (:challenge/reward-amount all-rewards)
                                                     (:vote/reward-amount all-rewards))]
                                       (if (zero? reward)
                                         0
                                         (format/format-token (/ reward 1e18) {:token "DANK"})))]]
               [pending-button {:pending? @claim-vote-reward-tx-pending?
                                :disabled (or (not (pos? (:vote/reward-amount all-rewards)))
                                              @claim-vote-reward-tx-pending? @claim-vote-reward-tx-success?)
                                :pending-text "Collecting ..."
                                :on-click (fn []
                                            (dispatch [::registry-entry/claim-vote-reward {:send-tx/id vote-reward-tx-id
                                                                                           :active-account @active-account
                                                                                           :reg-entry/address address
                                                                                           :meme/title (:meme/title meme)}]))}
                "Vote Reward"]
               [pending-button {:pending? @claim-challenge-reward-tx-pending?
                                :disabled (or (not (pos? (:challenge/reward-amount all-rewards)))
                                              @claim-challenge-reward-tx-pending? @claim-challenge-reward-tx-success?)
                                :pending-text "Collecting ..."
                                :on-click (fn []
                                            (dispatch [::registry-entry/claim-challenge-reward {:send-tx/id ch-reward-tx-id
                                                                                                :active-account @active-account
                                                                                                :reg-entry/address address
                                                                                                :meme/title (:meme/title meme)}]))}
                "Challenge Reward"]])))))))

(defn vote-action [{:keys [:reg-entry/address :challenge/vote :meme/title] :as meme}]
  (let [tx-id (str address "vote")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-commit-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-commit-vote tx-id}])
        form-data (r/atom {:amount-vote-for nil, :amount-vote-against nil})
        errors (reaction {:local (let [{:keys [amount-vote-for amount-vote-against]} @form-data]
                                   (cond-> {}
                                     (not (try (< 0 (js/parseInt amount-vote-for)) (catch js/Error e nil)))
                                     (assoc :amount-vote-for "Amount to vote for should be a positive number")

                                     (not (try (< 0 (js/parseInt amount-vote-against)) (catch js/Error e nil)))
                                     (assoc :amount-vote-against "Amount to vote against should be a positive number")))})]
    (fn [{:keys [:reg-entry/address :challenge/vote] :as meme}]
      (let [voted? (or (pos? (:vote/amount vote))
                       @tx-pending?
                       @tx-success?)
            account-balance @(subscribe [::balance-subs/active-account-balance :DANK])]
        [:div.vote
         [:div.vote-dank
          [:div.vote-input
           [with-label "Amount "
            [text-input {:form-data form-data
                           :id :amount-vote-for
                           :dom-id (str address :amount-vote-for)
                           :errors errors}]
            {:form-data form-data
             :for (str address :amount-vote-for)
             :id :amount-vote-for}]
           [:span "DANK"]]
          [pending-button {:pending? @tx-pending?
                           :pending-text "Voting ..."
                           :disabled (or voted? (-> @errors :local :amount-vote-for))
                           :on-click (fn []
                                       (dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                                            :reg-entry/address address
                                                                                            :vote/option :vote.option/vote-for
                                                                                            :vote/amount (-> @form-data
                                                                                                             :amount-vote-for
                                                                                                             js/parseInt
                                                                                                             (web3/to-wei :ether))
                                                                                            :meme/title title}]))}
           [:i.vote-dank]
           "Vote Dank"]]
         [:div.vote-stank
          [:div.vote-input
           [with-label "Amount "
            [text-input {:form-data form-data
                           :id :amount-vote-against
                           :dom-id (str address :amount-vote-against)
                           :errors errors}]
            {:form-data form-data
             :for (str address :amount-vote-against)
             :id :amount-vote-against}]
           [:span "DANK"]]
          [pending-button {:pending? @tx-pending?
                           :pending-text "Voting ..."
                           :disabled (or voted? (-> @errors :local :amount-vote-against))
                           :on-click (fn []
                                       (dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                                            :reg-entry/address address
                                                                                            :vote/option :vote.option/vote-against
                                                                                            :meme/title title
                                                                                            :vote/amount (-> @form-data
                                                                                                             :amount-vote-against
                                                                                                             js/parseInt
                                                                                                             (web3/to-wei :ether))}]))}
           "Vote Stank"]]
         [:p.max-vote-tokens (gstring/format "You can vote with up to %s tokens."
                                             (format/format-token (/ account-balance 1e18) {:token "DANK"}))]
         [:p.token-return  "Tokens will be returned to you after revealing your vote."]]))))

(defn reveal-action [{:keys [:challenge/vote :reg-entry/address :meme/title] :as meme}]
  (let [tx-id (str "reveal" address)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])]
    (fn [{:keys [] :as meme}]
      (let [disabled (or @tx-pending? @tx-success?)]
        [:div.reveal
         [:img {:src "/assets/icons/mememouth.png"}]
         [pending-button {:pending? @tx-pending?
                          :pending-text "Revealing ..."
                          :disabled disabled
                          :on-click (fn []
                                      (dispatch [::registry-entry/reveal-vote
                                                 {:send-tx/id tx-id
                                                  :reg-entry/address address
                                                  :meme/title title}
                                                 vote]))}
          (if disabled
            "Revealed"
            "Reveal My Vote")]]))))

(defn reveal-vote-action [{:keys [:reg-entry/address :reg-entry/status] :as meme}]
  (log/debug "REVEAL VOTE ACTION" meme ::reveal-vote-action)
  (case  (graphql-utils/gql-name->kw status)
    :reg-entry.status/commit-period [vote-action meme]
    :reg-entry.status/reveal-period [reveal-action meme]
    ;; TODO we should't need this extra case, but this component is
    ;; being rendered with old subscription value
    [:div]))

(defmethod page :route.dank-registry/vote []
  (let [account (subscribe [::accounts-subs/active-account])]
    (fn []
      (if-not @account
        [:div "Loading..."]
        [app-layout
         {:meta {:title "MemeFactory"
                 :description "Description"}}
         [:div.dank-registry-vote-page
          [:section.vote-header
           [header]]
          [:section.challenges
           [tabbed-pane
            [{:title "Open Challenges"
              :content [challenge-list {:include-challenger-info? false
                                        :query-params {:statuses [:reg-entry.status/commit-period
                                                                  :reg-entry.status/reveal-period]}
                                        :active-account @account
                                        :action-child reveal-vote-action
                                        :key :vote-page/open
                                        :sort-options [{:key "created-on" :value "Newest"}
                                                       {:key "commit-period-end" :value "Commit period end"}]}]}
             {:title "Resolved Challenges"
              :content [challenge-list {:include-challenger-info? true
                                        :query-params {:statuses [:reg-entry.status/blacklisted
                                                                  :reg-entry.status/whitelisted]
                                                       :challenged true}
                                        :active-account @account
                                        :action-child collect-reward-action
                                        :key :vote-page/resolved
                                        :sort-options [{:key "created-on" :value "Newest"}
                                                       {:key "reveal-period-end" :value "Reveal period end"}]}]}]]]]]))))
