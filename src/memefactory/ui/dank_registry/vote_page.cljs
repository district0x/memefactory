(ns memefactory.ui.dank-registry.vote-page
  (:require
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [reagent.core :as r]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.form.input :refer [select-input with-label text-input amount-input pending-button]]
   [react-infinite]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [memefactory.ui.dank-registry.events :as dr-events]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [district.ui.graphql.subs :as gql]
   [goog.string :as gstring]
   [district.time :as time]
   [cljs-time.extend]
   [district.format :as format]
   [cljs-time.core :as t]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [memefactory.ui.components.panes :refer [tabbed-pane]]
   [memefactory.ui.components.challenge-list :refer [challenge-list]]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-account-balances.subs :as balance-subs]
   [district.graphql-utils :as graphql-utils]
   [reagent.ratom :refer [reaction]]
   [memefactory.ui.components.charts :as charts]))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 2)

(defn header []
  [:div.registry-vote-header
   [:div.icon]
   [:h2.title "Dank registry - VOTE"]
   [:h3.title "Lorem ipsum dolor sit ..."]
   [:div.get-dank-button "Get Dank"]])

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
            (let [tx-id address
                  {:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total]} meme-voting
                  tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme/collect-all-rewards tx-id}])
                  tx-success? (subscribe [::tx-id-subs/tx-success? {:meme/collect-all-rewards tx-id}])]
              [:div.collect-reward
               (println "meme voting " meme-voting)
               [charts/donut-chart meme-voting]
               [:ul.vote-info
                [:li
                 (str "Voted dank: " (format/format-percentage votes-for votes-total) " - " votes-for)]
                [:li
                 (str "Voted stank: " (format/format-percentage votes-against votes-total) " - " votes-against)]
                [:li "Total voted: " (gstring/format "%d" votes-total)]
                [:li "Your reward: " (format/format-token all-rewards {:token "DANK"})]]
               [pending-button {:pending? @tx-pending?
                                :disabled (or (not (pos? all-rewards))
                                              @tx-pending? @tx-success?)
                                :pending-text "Collecting ..."
                                :on-click (fn []
                                            (dispatch [::dr-events/collect-all-rewards {:send-tx/id tx-id
                                                                                        :active-account @active-account
                                                                                        :reg-entry/address address}]))}
                "Collect Reward"]])))))))

(defn vote-action [{:keys [:reg-entry/address :challenge/vote] :as meme}]
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
            [amount-input {:form-data form-data
                           :id :amount-vote-for
                           :errors errors}]
            {;;:group-class :name
             :form-data form-data
             :id :amount-vote-for}]
           [:span "DANK"]]
          [pending-button {:pending? @tx-pending?
                           :pending-text "Voting ..."
                           :disabled (or voted? (-> @errors :local :amount-vote-for))
                           :on-click (fn []
                                       (dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                                            :reg-entry/address address
                                                                                            :vote/option :vote.option/vote-for
                                                                                            :vote/amount (-> @form-data :amount-vote-for js/parseInt)}]))}
           [:i.vote-dank]
           "Vote Dank"]]
         [:div.vote-stank
          [:div.vote-input
           [with-label "Amount "
            [amount-input {:form-data form-data
                           :id :amount-vote-against
                           :errors errors}]
            {;;:group-class :name
             :form-data form-data
             :id :amount-vote-against}]
           [:span "DANK"]]
          [pending-button {:pending? @tx-pending?
                           :pending-text "Voting ..."
                           :disabled (or voted? (-> @errors :local :amount-vote-against))
                           :on-click (fn []
                                       (dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                                            :reg-entry/address address
                                                                                            :vote/option :vote.option/vote-against
                                                                                            :vote/amount (-> @form-data :amount-vote-against js/parseInt)}]))}
           "Vote Stank"]]
         [:p.max-vote-tokens (gstring/format "You can vote with up to %s tokens."
                                             (format/format-token (/ account-balance 1e18) {:token "DANK"}))]
         [:p.token-return  "Tokens will be returned to you after revealing your vote."]]))))

(defn reveal-action [{:keys [:challenge/vote :reg-entry/address] :as meme}]
  (let [tx-id (str (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])]
    (fn [{:keys [] :as meme}]
      [:div.reveal
       [:img {:src "/assets/icons/mememouth.png"}]
       [pending-button {:pending? @tx-pending?
                        :pending-text "Revealing ..."
                        :disabled (or (not (pos? (:vote/amount vote)))
                                      @tx-pending? @tx-success?)
                        :on-click (fn []
                                    (dispatch [::registry-entry/reveal-vote
                                               {:send-tx/id tx-id
                                                :reg-entry/address address}
                                               vote]))}
        "Reveal My Vote"]])))

(defn reveal-vote-action [{:keys [:reg-entry/address :reg-entry/status] :as meme}]
  (println "REVEAL VOTE ACTION " meme)
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
                                                                  :reg-entry.status/whitelisted]}
                                        :active-account @account
                                        :action-child collect-reward-action
                                        :key :vote-page/resolved
                                        :sort-options [{:key "created-on" :value "Newest"}
                                                       {:key "reveal-period-end" :value "Reveal period end"}]}]}]]]]]))))
