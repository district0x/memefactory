(ns memefactory.ui.dank-registry.vote-page
  (:require
   [cljs-time.core :as t]
   [cljs-time.extend]
   [cljs-web3.core :as web3]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [memefactory.ui.utils :as ui-utils]
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
   [memefactory.ui.components.spinner :as spinner]
   [district.ui.router.events :as router-events]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log :refer [spy]]
   [memefactory.ui.components.buttons :as buttons]))

(def page-size 12)

(defn header []
  [:div.registry-vote-header
   [:div.icon]
   [:h2.title "Dank registry - VOTE"]
   [:h3.title "View challenges and vote to earn more DANK"]
   [:div.get-dank-button {:on-click #(dispatch [::router-events/navigate :route.get-dank/index nil nil])}
    [:span "Get Dank"]
    [:img.dank-logo {:src "/assets/icons/dank-logo.svg"}]
    [:img.arrow-icon {:src "/assets/icons/arrow-white-right.svg"}]]])

(defn collect-reward-action [{:keys [:reg-entry/address :challenge/all-rewards] :as meme}]
  (let [active-account (subscribe [::accounts-subs/active-account])]
    (when @active-account
      (let [response (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                         [:reg-entry/address
                                                          :meme/title
                                                          :challenge/votes-for
                                                          :challenge/votes-against
                                                          [:challenge/all-rewards {:user/address @active-account}
                                                           [:challenge/reward-amount
                                                            :vote/reward-amount]]
                                                          [:challenge/challenger [:user/address]]
                                                          [:challenge/vote-winning-vote-option {:vote/voter @active-account}]
                                                          [:challenge/vote {:vote/voter @active-account}
                                                           [:vote/option
                                                            :vote/amount]]
                                                          :challenge/votes-total]]]}])]

        (log/debug "@response" @response)

        (if (:graphql/loading? @response)
          [spinner/spin]
          (if-let [meme-voting (:meme @response)]
            (let [ch-reward-tx-id (str address "challenge-reward")
                  vote-reward-tx-id (str address "vote-reward")
                  {:keys [:challenge/reward-amount :vote/reward-amount]} all-rewards
                  {:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total :challenge/vote]} meme-voting
                  {:keys [:vote/option :vote/amount]
                   :or {option "voteOption_noVote"}} vote
                  option (graphql-utils/gql-name->kw (spy option))]

              (log/debug "vote" {:v vote :o option} ::collect-reward-action)


              (if (and (zero? votes-for)
                       (zero? votes-against))
                [:div "This challenge didn't receive any votes"]
                [:div.collect-reward
                 [charts/donut-chart meme-voting]
                 (into
                  [:ul.vote-info
                   [:li
                    (gstring/format "Voted Dank: %s (%d) "
                                    (format/format-percentage (or votes-for 0) votes-total)
                                    (quot (or votes-for 0) 1e18))]
                   [:li
                    (gstring/format "Voted Stank: %s (%d) "
                                    (format/format-percentage (or votes-against 0) votes-total)
                                    (quot (or votes-against 0) 1e18))]
                   [:li (gstring/format "Total voted: %d" (quot (or votes-total 0) 1e18))]
                   (when-not (or (= option :vote-option/not-revealed)
                                 (= option :vote-option/no-vote))
                     [:li (str "You voted: " (gstring/format "%d for %s "
                                                             (if (pos? amount)
                                                               (quot amount 1e18)
                                                               0)
                                                             (case option
                                                               :vote-option/vote-for "DANK"
                                                               :vote-option/vote-against "STANK")))])]
                  (cond
                    (and (pos? (:challenge/reward-amount all-rewards))
                         (pos? (:vote/reward-amount all-rewards)))
                    [[:li "Your vote reward: " (ui-utils/format-dank (:vote/reward-amount all-rewards))]
                     [:li "Your challenge reward: " (ui-utils/format-dank (:challenge/reward-amount all-rewards))]
                     [:li "Your total reward: " (ui-utils/format-dank (+ (:challenge/reward-amount all-rewards)
                                                                         (:vote/reward-amount all-rewards)))]]
                    (pos? (:challenge/reward-amount all-rewards))
                    [[:li "Your challenge reward: " (ui-utils/format-dank (:challenge/reward-amount all-rewards))]]

                    (pos? (:vote/reward-amount all-rewards))
                    [[:li "Your reward: " (ui-utils/format-dank (:vote/reward-amount all-rewards))]]))

                 (buttons/reclaim-buttons @active-account meme-voting)]))))))))

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
           (if voted? "Voted ""Vote Dank")]]
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
           (if voted? "Voted" "Vote Stank")]]
         [:p.max-vote-tokens (gstring/format "You can vote with up to %s tokens."
                                             (format/format-token (/ account-balance 1e18) {:token "DANK"}))]
         [:p.token-return  "Tokens will be returned to you after revealing your vote."]]))))

(defn reveal-action [{:keys [:challenge/vote :reg-entry/address :meme/title] :as meme}]
  (let [tx-id (str "reveal" address)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])
        active-account @(subscribe [::accounts-subs/active-account])
        vote (get @(subscribe [:memefactory.ui.subs/votes active-account]) address)
        vote-option (when-let [opt (-> meme :challenge/vote :vote/option)]
                      (graphql-utils/gql-name->kw opt))]
    (fn [{:keys [] :as meme}]
      (let [disabled (or @tx-pending? @tx-success? (not vote))]
        [:div.reveal
         [:img {:src "/assets/icons/mememouth.png"}]
         [:div.button-wrapper
          [pending-button {:pending? @tx-pending?
                           :pending-text "Revealing ..."
                           :disabled disabled
                           :on-click (fn []
                                       (dispatch [::registry-entry/reveal-vote
                                                  {:send-tx/id tx-id
                                                   :reg-entry/address address
                                                   :meme/title title}
                                                  vote]))}
           (if @tx-success?
             "Revealed"
             "Reveal My Vote")]]
         (when (and (= vote-option :vote-option/not-revealed)
                    (not vote))
           [:div.no-reveal-info "Secret to reveal vote was not found in your browser"])
         (when (not vote)
           [:div.no-reveal-info "You haven't voted"])]))))

(defn reveal-vote-action [{:keys [:reg-entry/address :reg-entry/status] :as meme}]
  (log/debug "REVEAL VOTE ACTION" meme ::reveal-vote-action)
  (case  (graphql-utils/gql-name->kw status)
    :reg-entry.status/commit-period [vote-action meme]
    :reg-entry.status/reveal-period [reveal-action meme]
    ;; TODO we should't need this extra case, but this component is
    ;; being rendered with old subscription value
    [:div]))

(defmethod page :route.dank-registry/vote []
  (let [account @(subscribe [::accounts-subs/active-account])]
    ;;fn []

    (log/debug ":route.dank-registry/vote" account)

    [app-layout
     {:meta {:title "MemeFactory"
             :description "Description"}}
     [:div.dank-registry-vote-page
      [:section.vote-header
       [header]]
      [:section.challenges
       [tabbed-pane
        [{:title "Open Challenges"
          :content [challenge-list {:include-challenger-info? true
                                    :query-params {:statuses [:reg-entry.status/commit-period
                                                              :reg-entry.status/reveal-period]}
                                    :active-account account
                                    :action-child reveal-vote-action
                                    :key :vote-page/open
                                    :sort-options [{:key "created-on"        :value "Newest"            :dir :desc}
                                                   {:key "commit-period-end" :value "Commit period end" :dir :asc}]}]}
         {:title "Resolved Challenges"
          :content [challenge-list {:include-challenger-info? true
                                    :query-params {:statuses [:reg-entry.status/blacklisted
                                                              :reg-entry.status/whitelisted]
                                                   :challenged true}
                                    :active-account account
                                    :action-child collect-reward-action
                                    :key :vote-page/resolved
                                    :sort-options [{:key "created-on"        :value "Newest"            :dir :desc}
                                                   {:key "reveal-period-end" :value "Reveal period end" :dir :asc}]}]}]]]]]))
