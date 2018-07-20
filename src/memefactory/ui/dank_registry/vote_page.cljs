(ns memefactory.ui.dank-registry.vote-page
  (:require
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [reagent.core :as r]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.form.input :refer [select-input with-label text-input pending-button]]
   [react-infinite]
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
   [district.graphql-utils :as graphql-utils]
   [reagent.ratom :refer [reaction]]))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 2)

(defn header []
  [:div.header
   [:h2 "Dank registry - Vote"]
   [:h3 "Lorem ipsum dolor sit ..."]
   [:div [:div "Get Dank"]]])

(defn collect-reward-action [{:keys [:reg-entry/address :challenge/all-rewards]}]
  (let [tx-id address
        active-account (subscribe [::accounts-subs/active-account])
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme/collect-all-rewards tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {:meme/collect-all-rewards tx-id}])]
    [:div.collect-reward
     [:img]
     [:ol.vote-info
      [:li [with-label "Voted dank:" (gstring/format "%d%% - %d" 62 55234)]]
      [:li [with-label "Voted stank:" (gstring/format "%d%% - %d" 38 34567)]]
      [:li [with-label "Total voted:" (gstring/format "%d" 87234)]]
      [:li [with-label "Your reward:" (gstring/format "%f MFM" (if  (pos? all-rewards)
                                                                 all-rewards
                                                                 0))]]]
     [pending-button {:pending? @tx-pending?
                      :disabled (or (not (pos? all-rewards))
                                    @tx-pending? @tx-success?) 
                      :pending-text "Collecting ..."
                      :on-click (fn []
                                  (dispatch [::dr-events/collect-all-rewards {:send-tx/id tx-id
                                                                              :active-account @active-account
                                                                              :reg-entry/address address}]))}
      "Collect Reward"]]))

(defn vote-action [{:keys [:reg-entry/address :challenge/vote] :as meme}]
  (let [tx-id address
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme/commit-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {:meme/commit-vote tx-id}])
        form-data (r/atom {})
        errors (reaction {:local (let [{:keys [amount-vote-for amount-vote-against]} @form-data]
                                   (cond-> {}
                                     (not (try (< 0 (js/parseInt amount-vote-for)) (catch js/Error e nil)))
                                     (assoc :amount-vote-for "Amount to vote for should be a positive number")
                                     
                                     (not (try (< 0 (js/parseInt amount-vote-against)) (catch js/Error e nil)))
                                     (assoc :amount-vote-against "Amount to vote against should be a positive number")))})]
    (fn [{:keys [:reg-entry/address :challenge/vote] :as meme}]
      (let [voted? (or (not= (graphql-utils/gql-name->kw (:vote/option vote))
                             :vote-option/no-vote)
                       @tx-pending?
                       @tx-success?)]
       [:div.vote
        [with-label "Amount "
         [:div [text-input {:form-data form-data
                            :id :amount-vote-for
                            :errors errors}]
          [:span "DANK"]]]
        [pending-button {:pending? @tx-pending?
                         :pending-text "Voting ..."
                         :disabled (or voted? (-> @errors :local :amount-vote-for)) 
                         :on-click (fn []
                                     (dispatch [::dr-events/commit-vote {:send-tx/id tx-id
                                                                         :reg-entry/address address
                                                                         :vote/option :vote.option/vote-for
                                                                         :vote/amount (-> @form-data :amount-vote-for js/parseInt)}]))}
         "Vote Dank"]
        [with-label "Amount "
         [:div [text-input {:form-data form-data
                            :id :amount-vote-against
                            :errors errors}]
          [:span "DANK"]]]
        [pending-button {:pending? @tx-pending?                         
                         :pending-text "Voting ..."
                         :disabled (or voted? (-> @errors :local :amount-vote-against)) 
                         :on-click (fn []
                                     (dispatch [::dr-events/commit-vote {:send-tx/id tx-id
                                                                         :reg-entry/address address
                                                                         :vote/option :vote.option/vote-against
                                                                         :vote/amount (-> @form-data :amount-vote-against js/parseInt)}]))}
         "Vote Stank"]
        [:p.max-vote-tokens "You can vote with up to 1123455 DANK tokens."]
        [:p.token-return  "Tokens will be returned to you after revealing your vote."]]))))

(defn reveal-action [{:keys [:challenge/vote :reg-entry/address] :as meme}]
  (let [tx-id (str (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {:meme/reveal-vote tx-id}])]
    (fn [{:keys [] :as meme}]
      [:div.vote
       [:img]
       [pending-button {:pending? @tx-pending?
                        :pending-text "Revealing ..."
                        :disabled (or (= (graphql-utils/gql-name->kw (:vote/option vote))
                                         :vote-option/no-vote)
                                      @tx-pending? @tx-success?)
                        :on-click (fn []
                                    (dispatch [::dr-events/reveal-vote
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
         [:div.dank-registry-submit 
          [header]
          [tabbed-pane
           [{:title "Open Challenges"
             :content [challenge-list {:include-challenger-info? false
                                       :query-params {:statuses [:reg-entry.status/commit-period
                                                                 :reg-entry.status/reveal-period]}
                                       :active-account @account
                                       :action-child reveal-vote-action
                                       :key :vote-page/open}]}
            {:title "Resolved Challenges"
             :content [challenge-list {:include-challenger-info? true
                                       :query-params {:statuses [:reg-entry.status/blacklisted
                                                                 :reg-entry.status/whitelisted]}
                                       :active-account @account
                                       :action-child collect-reward-action
                                       :key :vote-page/resolved}]}]]]]))))
