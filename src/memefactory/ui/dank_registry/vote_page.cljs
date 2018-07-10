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
   [district.graphql-utils :as graphql-utils]))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 2)

(defn header []
  [:div.header
   [:h2 "Dank registry - Vote"]
   [:h3 "Lorem ipsum dolor sit ..."]
   [:div [:div "Get Dank"]]])

(defn collect-reward-action [{:keys [:reg-entry/address :challenge/vote-winning-vote-option]}]
  (let [tx-id (str (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/buy tx-id}])]
    [:div.collect-reward
     [:img]
     [:ol.vote-info
      [:li [with-label "Voted dank:" (gstring/format "%d%% - %d" 62 55234)]]
      [:li [with-label "Voted stank:" (gstring/format "%d%% - %d" 38 34567)]]
      [:li [with-label "Total voted:" (gstring/format "%d" 87234)]]
      [:li [with-label "Your reward:" (gstring/format "%f MFM" (if vote-winning-vote-option
                                                                 125.12
                                                                 0))]]]
     [pending-button {:pending? @tx-pending?
                      :disabled (not vote-winning-vote-option)
                      :pending-text "Collecting ..."
                      :on-click (fn []
                                  (dispatch [:dank-registry/collect-reward {:send-tx/id tx-id
                                                                            :reg-entry/address address}]))}
      "Collect Reward"]]))

(defn vote-action [{:keys [:reg-entry/address] :as meme}]
  (let [tx-id (str (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/buy tx-id}])
        form-data (r/atom {})]
    (fn [{:keys [] :as meme}]
      [:div.vote
       [with-label "Amount "
        [:div [text-input {:form-data form-data
                           :id :amount-vote-for}]
         [:span "DANK"]]]
       [pending-button {:pending? @tx-pending?
                        :pending-text "Voting ..."
                        :on-click (fn []
                                    (dispatch [:dank-registry/vote {:send-tx/id tx-id
                                                                    :reg-entry/address address
                                                                    :vote/option :vote-option/vote-for}]))}
        "Vote Dank"]
       [with-label "Amount "
        [:div [text-input {:form-data form-data
                           :id :amount-vote-for}]
         [:span "DANK"]]]
       [pending-button {:pending? @tx-pending?
                        :pending-text "Voting ..."
                        :on-click (fn []
                                    (dispatch [:dank-registry/vote {:send-tx/id tx-id
                                                                    :reg-entry/address address
                                                                    :vote/option :vote-option/vote-against}]))}
        "Vote Stank"]
       [:p.max-vote-tokens "You can vote with up to 1123455 DANK tokens."]
       [:p.token-return  "Tokens will be returned to you after revealing your vote."]])))

(defn reveal-action [{:keys [:challenge/vote :reg-entry/address] :as meme}]
  (let [tx-id (str (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/buy tx-id}])]
    (fn [{:keys [] :as meme}]
      [:div.vote
       [:img]
       [pending-button {:pending? @tx-pending?
                        :pending-text "Revealing ..."
                        :disabled vote
                        :on-click (fn []
                                    (dispatch [:dank-registry/reveal-vote
                                               {:send-tx/id tx-id
                                                :reg-entry/address address}
                                               vote]))}
        "Reveal My Vote"]])))

(defn reveal-vote-action [{:keys [:reg-entry/address :reg-entry/status] :as meme}]
  (case  (graphql-utils/gql-name->kw status)
    :reg-entry.status/commit-period [vote-action meme] 
    :reg-entry.status/reveal-period [reveal-action meme]))

(defmethod page :route.dank-registry/vote []
  (let [account (subscribe [::accounts-subs/active-account])]
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
                                  :voter @account
                                  :action-child reveal-vote-action}]}
       {:title "Resolved Challenges"
        :content [challenge-list {:include-challenger-info? true
                                  :query-params {:statuses [:reg-entry.status/blacklisted
                                                            :reg-entry.status/whitelisted]}
                                  :voter @account
                                  :action-child collect-reward-action}]}]]]]))
