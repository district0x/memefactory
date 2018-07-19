(ns memefactory.ui.dank-registry.challenge-page
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
   [district.ui.server-config.subs :as config-subs]
   [district.format :as format]
   [reagent.ratom :refer [reaction]]))

(defn header []
  [:div.header
   [:h2 "Dank registry - Challenge"]
   [:h3 "Lorem ipsum dolor sit ..."]
   [:div [:div "Get Dank"]]])

(defn open-challenge-action [{:keys [:reg-entry/address]}]
  (let [form-data (r/atom {})
        open? (r/atom false)
        tx-id address
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme/create-challenge tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {:meme/create-challenge tx-id}])
        dank-deposit (subscribe [::config-subs/config :deployer :initial-registry-params :meme-registry :deposit])
        errors (reaction {:local (let [{:keys [comment]} @form-data]
                                   (cond-> {}
                                     (empty? comment)
                                     (assoc :comment "Comment shouldn't be empty.")))})]
    (fn [{:keys [:reg-entry/address]}]
     [:div.challenge-controls
      [:img.vs]
      (if @open?
        [:div
         [text-input {:form-data form-data
                      :id :comment
                      :errors errors}]
         [pending-button {:pending? @tx-pending?
                          :disabled (or @tx-pending? @tx-success? (not (empty? (:local @errors))))
                          :pending-text "Challenging ..."
                          :on-click (fn []
                                      (dispatch [::dr-events/add-challenge {:send-tx/id tx-id
                                                                            :reg-entry/address address
                                                                            :comment (:comment @form-data)
                                                                            :deposit @dank-deposit}]))}
          "Challenge"]
         [:span.dank (format/format-token @dank-deposit  {:token "DANK"})]]
        [:button {:on-click #(swap! open? not)} "Challenge"])])))

(defmethod page :route.dank-registry/challenge []
  [app-layout
   {:meta {:title "MemeFactory"
           :description "Description"}}
   [:div.dank-registry-challenge
    [header]
    [challenge-list {:include-challenger-info? false
                     :query-params {:statuses [:reg-entry.status/challenge-period]}
                     :action-child open-challenge-action
                     :key :challenge-page}]]])

