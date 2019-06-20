(ns memefactory.ui.param_change.page
  (:require
    [district.ui.component.page :refer [page]]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [memefactory.ui.components.general :refer [nav-anchor]]
    [reagent.core :as r]
    [district.ui.component.form.input :refer [select-input with-label text-input textarea-input]]
    [reagent.ratom :as ratom]
    [memefactory.ui.components.panes :refer [simple-tabbed-pane]]
    [memefactory.ui.components.charts :as charts]))

(defn header-box []
  [:div.header-box
   [:div.icon]
   [:h2.title "Parameters"]
   [:div.body
    [:p "Lorem ipsum dolor sit ..."]]])

(defn parameter-table []
  (let [open? (r/atom true)
        dummy-entry [:tr
                     [:td
                      [:div
                       [:div.param-title "Meme Challenge Period Duration"]
                       [:div.param-b "Lorem ipsum dolor sit amet,..."]]]
                     [:td
                      [:div
                       [:div.param-h "Current Value"]
                       [:div.param-b "86400 (24 hours)"]]]
                     [:td
                      [:div
                       [:div.param-h "Last Change"]
                       [:div.param-b "14/06/2019"]]]
                     [:td]]]

    (fn []
      [:div.panel.param-table-panel {:class (if @open? "open" "closed")}

       [:table.param-table
        [:thead
         [:tr
          [:th "Parameter"]
          [:th.optional "Current Value"]
          [:th.optional "Last Change"]
          [:th [:div.collapse-icon {:on-click #(swap! open? not) ;; TODO: fix this on mobile, doesn't look good
                                    :class (when @open? "flipped")}]]]]
        [:tbody
         dummy-entry
         dummy-entry
         dummy-entry]]])))

(defn change-submit-form []
  (let [form-data (r/atom {})
        errors (ratom/reaction @form-data)]
    (fn []
      [:div.panel.change-submit-form
      [:div.body
       [:h2.title "Propose a change"]
       [:h3 "Lorem ipsum dolor sit amet..."]
       [select-input {:form-data form-data
                      :id :order-by
                      :group-class :options
                      :options [{:key "meme-challenge-period-duration" :value "Meme Challenge Period Duration"}]}]
       [:div.form
        [:div.input-old
         [:div.current-value "86400"]
         #_[:span.unit "Seconds"] ;; TODO: fix this
         [:div.label-under "Current Value"]]

        [:div.input-outer.input-new
         [text-input {:form-data form-data
                      :errors errors
                      :id :title
                      :dom-id :ftitle
                      :maxLength 60}]
         #_[:span.unit "Seconds"] ;; TODO: fix this
         [:div.label-under "New Value"]]

        [:div.textarea
         [:label "Comment"]
         [textarea-input {:form-data form-data
                          :class "comment"
                          :errors errors
                          :maxLength 100
                          :id :param-change/comment
                          :on-click #(.stopPropagation %)}]
         [:div.dank [:span.dank "10"] [:span "DANK"]]]]]
       [:button.footer "Submit"]])))

(defn challenge-action []
  (let [form-data (r/atom {})
        errors (ratom/reaction @form-data)]
    (fn []
     [:div.challenge-action
      [:h4 "Challenge Explanation"]
      [textarea-input {:form-data form-data
                       :class "comment"
                       :errors errors
                       :maxLength 100
                       :id :param-change/comment
                       :on-click #(.stopPropagation %)}]
      [:div.footer
       [:div.dank [:span.dank "10"] [:span "DANK"]]
       [:button "CHALLENGE"]]])))

(defn reveal-action []
  [:div.reveal-action
   [:div.icon]
   [:div [:button "REVEAL MY VOTE"]]])

(defn vote-action []
  (let [form-data (r/atom {})
        errors (ratom/reaction @form-data)]
    (fn []
      [:div.vote-action

       [:div.vote-ctrl.vote-yes
        [:div.vote-input
         [with-label "Amount "
          [text-input {:form-data form-data
                       :id :amount-vote-against
                       :disabled false
                       :dom-id (str #_address :amount-vote-against)
                       :errors errors
                       :type :number}]
          {:form-data form-data
           :for (str #_address :amount-vote-against)
           :id :amount-vote-against}]
         [:span "DANK"]]
        [:button [:i.vote-dank] "VOTE YES"]]

       [:div.vote-ctrl.vote-no
        [:div.vote-input
         [with-label "Amount "
          [text-input {:form-data form-data
                       :id :amount-vote-against
                       :disabled false
                       :dom-id (str #_address :amount-vote-against)
                       :errors errors
                       :type :number}]
          {:form-data form-data
           :for (str #_address :amount-vote-against)
           :id :amount-vote-against}]
         [:span "DANK"]]
        [:button [:i.vote-dank] "VOTE NO"]]

       [:div.info
        [:div "You can vote with up to 1,123,455 DANK tokens."]
        [:div "Tokens will be returned to you after revealing your vote."]]])))

(defn apply-change-action []
  [:div.apply-change-action
   [:div.info "This parameter change wasn't challenged"]
   [:button "APPLY CHANGE"]])

(defn claim-action [voting]
  [:div.claim-action
   [charts/donut-chart voting]
   [:ul
    [:li [:label "Voted Yes:"] [:span "0% (0)"]]
    [:li [:label "Voted No:"] [:span "100% (100)"]]
    [:li [:label "Total Voted:"] [:span "100"]]
    [:li [:label "You Voted:"] [:span "20 for No (5%)"]]]
   [:button "CLAIM REWARD"]])

(defn proposed-change [{:keys [action-child applied-mark]}]
  [:div.panel.proposed-change-panel
   [:div.header
    (cond
      (true? applied-mark) [:div.icon.applied]
      (false? applied-mark) [:div.icon.not-applied])]
   [:div.proposed-change
    [:div.info
     [:h2.title "Proposed Change"]
     [:div.info-body
      [:div.section1
       [:h4 "Meme Submit Deposit"]
       [:ul.submit-info
        [:li.attr [:label "Created:"] [:span "2 weeks ago"]]
        [:li.attr [:label "Status:"] [:span "Change was applied 5 days ago"]]
        [:li.attr [:label "Previous Value"] [:span "100 DANK"]]
        [:li.attr [:label "New Value"] [:span "200 DANK"]]]]
      [:div.section2
       [:h4 "Proposer (0x....)"]
       [:div.comment "Current deposit is too cheap, which results in large number of submissions of low quality"]
       [:h4 "Challenger (0x....)"]
       [:div.comment "I don't think this is a good idea. Number of submissions may decrease below desired level"]]]]
    [:div.action action-child]]])

(defn open-proposals-list []
  [:ul.proposal-list
   [:li [proposed-change {:action-child [challenge-action]}]]
   [:li [proposed-change {:action-child [reveal-action]}]]
   [:li [proposed-change {:action-child [vote-action]}]]
   [:li [proposed-change {:action-child [apply-change-action]}]]])

(defn resolved-proposals-list []
  [:ul.proposal-list
   [:li [proposed-change {:action-child [claim-action {:reg-entry/address "0x1"
                                                       :challenge/votes-for 10
                                                       :challenge/votes-against 13
                                                       :challenge/votes-total 23}]
                          :applied-mark true}]]
   [:li [proposed-change {:action-child [claim-action {:reg-entry/address "0x2"
                                                       :challenge/votes-for 10
                                                       :challenge/votes-against 13
                                                       :challenge/votes-total 23}]
                          :applied-mark false}]]])

(defn change-proposals []
  [simple-tabbed-pane
   [{:title "Open Proposals"
     :content [open-proposals-list]}
    {:title "Resolved Proposals"
     :content [resolved-proposals-list]}]])

(defmethod page :route.param-change/index []
  [app-layout
   {:meta {:title "MemeFactory - Param Change"
           :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.param-change-page
    [header-box]
    [parameter-table]
    [change-submit-form]
    [change-proposals]
    ]])
