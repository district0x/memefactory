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
   [memefactory.ui.components.challenge-list :refer [challenge-list]]))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 2)

(defn header []
  [:div.header
   [:h2 "Dank registry - Vote"]
   [:h3 "Lorem ipsum dolor sit ..."]
   [:div [:div "Get Dank"]]])

(defn collect-reward-action [{:keys [:reg-entry/address]}]
  (let [tx-id (str (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/buy tx-id}])]
    [:div.collect-reward
     [:img]
     [:ol.vote-info
      [:li [with-label "Voted dank:" (gstring/format "%d%% - %d" 62 55234)]]
      [:li [with-label "Voted stank:" (gstring/format "%d%% - %d" 38 34567)]]
      [:li [with-label "Total voted:" (gstring/format "%d" 87234)]]
      [:li [with-label "Your reward:" (gstring/format "%f MFM" 125.12)]]]
     [pending-button {:pending? @tx-pending?
                      :pending-text "Collecting ..."
                      :on-click (fn []
                                  (dispatch [:dank-registry/collect-reward {:send-tx/id tx-id
                                                                            :reg-entry/address address}]))}
      "Collect Reward"]]))

(defmethod page :route.dank-registry/vote []
  [app-layout
   {:meta {:title "MemeFactory"
           :description "Description"}}
   [:div.dank-registry-submit
    [header]
    [tabbed-pane
     [{:title "Open Challenges"
       :content [challenge-list {:include-challenger-info? false
                                 :query-params {:statuses [:reg-entry.status/challenge-period]}
                                 :action-child collect-reward-action}]}
      {:title "Resolved Challenges"
       :content [challenge-list {:include-challenger-info? true
                                 :query-params {:statuses [:reg-entry.status/challenge-period]}
                                 :action-child collect-reward-action}]}]]]])

