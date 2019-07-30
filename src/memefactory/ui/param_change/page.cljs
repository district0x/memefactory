(ns memefactory.ui.param_change.page
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [district.parsers :as parsers]
   [district.ui.component.page :refer [page]]
   [district.ui.web3-account-balances.subs :as balance-subs]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.general :refer [nav-anchor]]
   [reagent.core :as r]
   [district.ui.component.form.input :as inputs :refer [select-input with-label text-input textarea-input pending-button]]
   [memefactory.ui.components.buttons :as buttons]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [reagent.ratom :as ratom]
   [memefactory.ui.components.panes :refer [simple-tabbed-pane]]
   [memefactory.ui.components.charts :as charts]
   [re-frame.core :refer [subscribe dispatch]]
   [district.graphql-utils :as gql-utils]
   [memefactory.shared.utils :as shared-utils]
   [cljs-time.core :as t]
   [memefactory.ui.utils :as ui-utils]
   [district.time :as time]
   [district.format :as format]
   [cljs-time.format :as time-format]
   [memefactory.ui.contract.param-change :as param-change]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.graphql.subs :as gql]
   [goog.string :as gstring]
   [clojure.string :as str]
   [memefactory.ui.events :as memefactory-events]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [print.foo :refer [look] :include-macros true]))

(defn header-box []
  [:div.header-box
   [:div.icon]
   [:h2.title "Parameters"]
   [:div.body
    [:p "Lorem ipsum dolor sit ..."]]])

(def param-info {:meme/challenge-dispensation {:title "Meme Challenge Dispensation" :description "Lorem ipsum dolor sit amet,..." :unit "%"}
                 :meme/vote-quorum {:title "Meme Vote Quorum" :description "Lorem ipsum dolor sit amet,..." :unit "%"}
                 :meme/max-total-supply {:title "Meme Max Total Supply" :description "Lorem ipsum dolor sit amet,..." :unit ""}
                 :meme/challenge-period-duration {:title "Meme Challenge Period Duration" :description "Lorem ipsum dolor sit amet,..." :unit "Seconds"}
                 :meme/max-auction-duration {:title "Meme Max Auction Duration" :description "Lorem ipsum dolor sit amet,..." :unit "Seconds"}
                 :meme/reveal-period-duration {:title "Meme Reveal Period Duration" :description "Lorem ipsum dolor sit amet,..." :unit "Seconds"}
                 :meme/commit-period-duration {:title "Meme Commit Period Duration" :description "Lorem ipsum dolor sit amet,..." :unit "Seconds"}
                 :meme/deposit {:title "Meme Deposit" :description "Lorem ipsum dolor sit amet,..." :unit "DANK"}
                 :param-change/challenge-dispensation {:title "Parameter Challenge Dispensation" :description "Lorem ipsum dolor sit amet,..." :unit "%"}
                 :param-change/vote-quorum {:title "Parameter Vote Quorum" :description "Lorem ipsum dolor sit amet,..." :unit "%"}
                 :param-change/challenge-period-duration {:title "Parameter Challenge Period Duration" :description "Lorem ipsum dolor sit amet,..." :unit "Seconds"}
                 :param-change/reveal-period-duration {:title "Parameter Reveal Period Duration" :description "Lorem ipsum dolor sit amet,..." :unit "Seconds"}
                 :param-change/commit-period-duration {:title "Parameter Commit Period Duration" :description "Lorem ipsum dolor sit amet,..." :unit "Seconds"}
                 :param-change/deposit {:title "Parameter Deposit" :description "Lorem ipsum dolor sit amet,..." :unit "DANK"}})

(defn scale-param-change-value [k v]
  (if (#{:meme/deposit :param-change/deposit} k)
    (web3/from-wei v :ether)
    v))

(defn unscale-param-change-value [k v]
  (if (#{:meme/deposit :param-change/deposit} k)
    (web3/to-wei v :ether)
    v))

(defn param-split-keys [p]
  ((juxt namespace name) (:key p)))

(defn param-str-key->ns-key [str-key]
  (when-not (str/blank? str-key)
    (apply keyword (str/split str-key #","))))

(defn parameter-table []
  (let [open? (r/atom true)
        all-params (subscribe [:memefactory.ui.config/all-params])]
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
         (for [{:keys [key value set-on]} @all-params]
           ^{:key (str key)}
           [:tr
            [:td
             [:div
              [:div.param-title (:title (param-info key))]
              [:div.param-b (:description (param-info key))]]]
            [:td
             [:div
              [:div.param-h "Current Value"]
              [:div.param-b (str (scale-param-change-value key value) " " (:unit (param-info key)))]]]
            [:td
             [:div
              [:div.param-h "Last Change"]
              [:div.param-b (time-format/unparse (time-format/formatter "dd/MM/yyyy")
                                                 (t/local-date-time (gql-utils/gql-date->date set-on)))]]]
            [:td]])]]])))


(defn change-submit-form []
  (let [get-param (fn [k all-params]
                    (some (fn [{:keys [key ptype] :as p}]
                            (when (= key k)
                              p))
                     all-params))
        all-params (subscribe [:memefactory.ui.config/all-params])

        form-data (r/atom {:param-change/key nil})
        errors (ratom/reaction @form-data)
        tx-id (str "param-change-submission" (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::param-change/submit-param-change tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::param-change/submit-param-change tx-id}])
        active-account (subscribe [::accounts-subs/active-account])
        selected-param (ratom/reaction (or (param-str-key->ns-key (:param-change/key @form-data))
                                           (:key (first @all-params))))
        current-value (ratom/reaction (when @all-params
                                        (:value (get-param @selected-param @all-params))))
        current-value-unit (ratom/reaction (:unit (get param-info @selected-param)))
        pc-params (subscribe [:memefactory.ui.config/param-change-db-params])]
    (fn []
      [:div.panel.change-submit-form
       [:div.body
        [:h2.title "Propose a change"]
        [:h3 "Lorem ipsum dolor sit amet..."]
        [select-input {:form-data form-data
                       :id :param-change/key
                       :group-class :options
                       :options (mapv (fn [{:keys [key] :as p}]
                                        {:key (param-split-keys p) :value (:title (get param-info key))})
                                      @all-params)}]
        [:div.form
         [:div.input-old
          [:div.current-value (scale-param-change-value @selected-param @current-value)]
          [:span.param-unit @current-value-unit]
          [:div.label-under "Current Value"]]

         [:div.input-outer.input-new
          [text-input {:form-data form-data
                       :errors errors
                       :id :param-change/value
                       :dom-id :ftitle
                       :maxLength 60}]
          [:span.param-unit @current-value-unit]
          [:div.label-under "New Value"]]

         [:div.textarea
          [:label "Comment"]
          [textarea-input {:form-data form-data
                           :class "comment"
                           :errors errors
                           :maxLength 100
                           :id :param-change/comment
                           :on-click #(.stopPropagation %)}]
          [:div.dank [:span.dank (ui-utils/format-dank (-> (get @pc-params :deposit) :value))]]]]]
       [pending-button {:pending? @tx-pending?
                        :disabled (or @tx-pending? @tx-success? (not (empty? (:local @errors))) (not @active-account))
                        :class "footer"
                        :pending-text "Submitting ..."
                        :on-click #(dispatch [::param-change/upload-and-add-param-change
                                              {:send-tx/id tx-id
                                               :reason (:param-change/comment @form-data)
                                               :param-db ({"meme" :meme-registry-db
                                                           "param-change" :param-change-registry-db}
                                                          (namespace @selected-param))
                                               :key (name @selected-param)
                                               :value (unscale-param-change-value @selected-param (:param-change/value @form-data))
                                               :deposit (-> (get @pc-params :deposit) :value)}])}
        "Submit"]])))

(defn challenge-action [{:keys [:reg-entry/address :param-change/key] :as args}]
  (let [form-data (r/atom {})
        errors (ratom/reaction @form-data)
        tx-id (str address "challenges")
        pc-params (subscribe [:memefactory.ui.config/param-change-db-params])]
    (fn [{:keys [:reg-entry/address :param-change/key] :as args}]
      [:div.challenge-action
       [:h4 "Challenge Explanation"]
      [textarea-input {:form-data form-data
                       :class "comment"
                       :errors errors
                       :maxLength 100
                       :id :param-change/comment
                       :on-click #(.stopPropagation %)}]
      [:div.footer
       [:div.dank [:span.dank (ui-utils/format-dank (-> (get @pc-params :deposit) :value))] ]
       [:button {:on-click #(dispatch [::memefactory-events/add-challenge
                                       {:send-tx/id tx-id
                                        :reg-entry/address address
                                        :tx-description (->> (gql-utils/gql-name->kw key)
                                                             (get param-info)
                                                             :title)
                                        :type :param-change
                                        :comment (:param-change/comment @form-data)
                                        :deposit (-> (get @pc-params :deposit) :value)}])}
        "CHALLENGE"]]])))

(defn reveal-action [param-change]
  (let [tx-id (str "reveal" (:reg-entry/address param-change))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])
        active-account @(subscribe [::accounts-subs/active-account])
        vote (get @(subscribe [:memefactory.ui.subs/votes active-account]) (:reg-entry/address param-change))
        vote-option (-> param-change :challenge/vote :vote/option gql-utils/gql-name->kw)]
    (fn [param-change]
      [:div.reveal-action
       [:div.icon]
       [:div [inputs/pending-button {:disabled (or @tx-success? (not vote))
                                     :pending? @tx-pending?
                                     :pending-text "Revealing..."
                                     :on-click #(dispatch [::registry-entry/reveal-vote
                                                           {:send-tx/id tx-id
                                                            :reg-entry/address (:reg-entry/address param-change)
                                                            :tx-description (->> (gql-utils/gql-name->kw (:param-change/key param-change))
                                                                                 (get param-info)
                                                                                 :title)
                                                            :option-desc {:vote.option/vote-against "no"
                                                                          :vote.option/vote-for     "yes"}}
                                                           vote])}
              (if @tx-success?
                "Revealed"
                "Reveal My Vote")]]
       (when (and (= vote-option :vote-option/not-revealed)
                  (not vote))
         [:div.no-reveal-info "Secret to reveal vote was not found in your browser"])])))

(defn vote-action [param-change]
  (let [form-data (r/atom {})
        errors (ratom/reaction @form-data)
        tx-id (str (:reg-entry/address param-change) "vote")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-commit-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-commit-vote tx-id}])]
    (fn [param-change]
      (let [vote-fn (fn [option amount]
                      (dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                           :reg-entry/address (:reg-entry/address param-change)
                                                                           :vote/option option
                                                                           :vote/amount amount
                                                                           :tx-description (->> (gql-utils/gql-name->kw (:param-change/key param-change))
                                                                                                (get param-info)
                                                                                                :title)
                                                                           :type :param-change
                                                                           :option-desc {:vote.option/vote-against "NO"
                                                                                         :vote.option/vote-for     "YES"}}]))
            address (:reg-entry/address param-change)
            account-balance @(subscribe [::balance-subs/active-account-balance :DANK])]
        [:div.vote-action

        [:div.vote-ctrl.vote-yes
         [:div.vote-input
          [with-label "Amount "
           [text-input {:form-data form-data
                        :id :amount-vote-for
                        :disabled false
                        :dom-id (str address :amount-vote-for)
                        :errors errors
                        :type :number}]
           {:form-data form-data
            :for (str address :amount-vote-for)
            :id :amount-vote-for}]
          [:span "DANK"]]
         [inputs/pending-button
          {:pending? @tx-pending?
           :disabled (or (-> @errors :local :vote/amount-vote-for empty? not)
                         @tx-pending?
                         @tx-success?)
           :pending-text "Voting..."
           :on-click (partial vote-fn
                              :vote.option/vote-for
                              (-> @form-data
                                  :amount-vote-for
                                  parsers/parse-float
                                  (web3/to-wei :ether)))}
          (if @tx-success?
            "VOTED"
            "VOTE YES")]]

        [:div.vote-ctrl.vote-no
         [:div.vote-input
          [with-label "Amount "
           [text-input {:form-data form-data
                        :id :amount-vote-against
                        :disabled false
                        :dom-id (str address :amount-vote-against)
                        :errors errors
                        :type :number}]
           {:form-data form-data
            :for (str address :amount-vote-against)
            :id :amount-vote-against}]
          [:span "DANK"]]
         [inputs/pending-button
          {:pending? @tx-pending?
           :disabled (or (-> @errors :local :amount-vote-against empty? not)
                         @tx-pending?
                         @tx-success?)
           :pending-text "Voting..."
           :on-click (partial vote-fn
                              :vote.option/vote-against
                              (-> @form-data
                                  :amount-vote-against
                                  parsers/parse-float
                                  (web3/to-wei :ether)))}
          (if @tx-success?
            "VOTED"
            "VOTE NO")]]

        [:div.info
         [:div (gstring/format "You can vote with up to %s tokens."
                               (ui-utils/format-dank account-balance))]
         [:div "Tokens will be returned to you after revealing your vote."]]]))))

(defn apply-change-action [{:keys [:reg-entry/address] :as param-change}]
  (let [tx-id (str "apply" (:reg-entry/address param-change))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:param-change/apply-change tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {:param-change/apply-change tx-id}])]
    [:div.apply-change-action
     [inputs/pending-button
      {:pending? @tx-pending?
       :disabled (or @tx-pending?
                     @tx-success?)
       :pending-text "Applying..."
       :on-click #(dispatch [::param-change/apply-param-change
                             {:send-tx/id tx-id
                              :reg-entry/address address}])}
      (if @tx-success?
        "APPLIED"
        "APPLY CHANGE")]]))

(defn claim-action [{:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total :challenge/vote] :as voting}]

  (let [format-votes (fn [n tot]
                       (gstring/format "%s (%f)"
                                       (if (pos? tot) (format/format-percentage (or n 0) tot) 0)
                                       (format/format-number (bn/number (web3/from-wei (or n 0) :ether)))))
        active-account (subscribe [::accounts-subs/active-account])]
    [:div.claim-action
     (when (pos? votes-total)
       [charts/donut-chart voting])
     [:ul
      [:li [:label "Voted Yes:"] [:span (format-votes votes-for votes-total)]]
      [:li [:label "Voted No:"] [:span (format-votes votes-against votes-total)]]
      [:li [:label "Total Voted:"] [:span (format/format-number (bn/number (web3/from-wei (or votes-total 0) :ether)))]]

      (when (#{:vote-option/vote-for :vote-option/vote-against} (gql-utils/gql-name->kw (:vote/option vote)))
        [:li [:label "You Voted:"] [:span (gstring/format "%d for %s (%s)"
                                                          (format/format-number (bn/number (web3/from-wei (or (:vote/amount vote) 0) :ether)))
                                                          ({:vote-option/vote-for "Yes"
                                                            :vote-option/vote-against "No"}
                                                           (gql-utils/gql-name->kw (:vote/option vote)))
                                                          (format/format-percentage (or (:vote/amount vote) 0) votes-total))]])]
     (when @active-account (buttons/reclaim-buttons @active-account voting))]))

(defn format-time [gql-date]
  (let [formated-time (-> (time/time-remaining (t/date-time (gql-utils/gql-date->date gql-date))
                                               (t/now))
                          (dissoc :seconds)
                          (format/format-time-units {:short? true}))]
    (if-not (empty? formated-time)
      (str formated-time " ago")
      "less than a minute ago")))

(defn second-date-keys
  "Convert given map keys from date to seconds since epoch"
  [m ks]
  (reduce (fn [r k]
            (update r k #(let [r (when-let [d (gql-utils/gql-date->date %)]
                                   (quot (.getTime d) 1000))]
                           r)))
   m
   ks))

(defn param-change-status [now pc]
  (->> (second-date-keys pc #{:reg-entry/created-on :reg-entry/challenge-period-end
                              :challenge/commit-period-end :challenge/reveal-period-end})
       (shared-utils/reg-entry-status now)))

(defn proposed-change [{:keys [:reg-entry/creator :challenge/challenger :param-change/reason :param-change/key :param-change/db
                               :reg-entry/created-on :param-change/original-value :param-change/value
                               :challenge/comment :param-change/applied-on] :as pc} {:keys [action-child applied-mark]}]
  (let [now (ui-utils/now-in-seconds)
        param-db-keys-by-db @(subscribe [:memefactory.ui.config/param-db-keys-by-db])
        key (keyword ({:meme-registry-db "meme"
                       :param-change-registry-db "param-change"}
                      (param-db-keys-by-db db))
                     (gql-utils/gql-name->kw key))]
    [:div.panel.proposed-change-panel
     ;; Only for debugging
    #_[:b (str (param-change-status @now pc) " " @now)]
    #_(str (second-date-keys pc #{:reg-entry/created-on :reg-entry/challenge-period-end
                                :challenge/commit-period-end :challenge/reveal-period-end}))
     [:div.header
     (cond
       (true? applied-mark) [:div.icon.applied]
       (false? applied-mark) [:div.icon.not-applied])]
    [:div.proposed-change
     [:div.info
      [:h2.title "Proposed Change"]
      [:div.info-body
       [:div.section1
        [:h4 (:title (get param-info key))]
        [:ul.submit-info
         [:li.attr [:label "Created:"] [:span (format-time created-on)]]
         [:li.attr [:label "Status:"] [:span
                                       (let [entry-status (param-change-status @now pc)]
                                         (cond
                                           applied-on
                                           (gstring/format "Change was applied " (format-time applied-on))

                                           (#{:reg-entry.status/challenge-period} entry-status)
                                           "Change is in challenge period"

                                           (#{:reg-entry.status/commit-period :reg-entry.status/reveal-period} entry-status)
                                           "Change is being challenged"

                                           (#{:reg-entry.status/blacklisted} entry-status)
                                           "Change was rejected"

                                           (#{:reg-entry.status/whitelisted} entry-status)
                                           "Change was accepted"))]]
         [:li.attr [:label "Previous Value:"] [:span (scale-param-change-value key original-value)]]
         [:li.attr [:label "New Value:"] [:span (scale-param-change-value key value)]]]]
       [:div.section2
        [:div.proposer
         [:h4 [:span "Proposer ("] [:span.address (:user/address creator)] [:span ")"]]
         [:div.comment reason]]
        (when challenger
          [:div.challenger
           [:h4 [:span "Challenger ("] [:span.address (:user/address challenger)] [:span ")"]]
           [:div.comment comment]])]]]
     [:div.action action-child]]]))

(defn open-proposals-list []
  (let [active-account (subscribe [::accounts-subs/active-account])]
    (fn []
      (when @active-account
        (let [proposals-subs (subscribe [::gql/query {:queries [[:search-param-changes {:order-dir :desc
                                                                                       :order-by :param-changes.order-by/created-on
                                                                                       :statuses [:reg-entry.status/commit-period
                                                                                                  :reg-entry.status/reveal-period
                                                                                                  :reg-entry.status/challenge-period
                                                                                                  :reg-entry.status/whitelisted]}
                                                                [[:items [:param-change/reason
                                                                          :param-change/key
                                                                          :param-change/db
                                                                          :reg-entry/created-on
                                                                          :param-change/original-value
                                                                          :param-change/value
                                                                          :challenge/comment
                                                                          [:challenge/challenger [:user/address]]
                                                                          [:reg-entry/creator [:user/address]]
                                                                          :reg-entry/address
                                                                          :reg-entry/challenge-period-end
                                                                          :challenge/commit-period-end
                                                                          :challenge/reveal-period-end
                                                                          :challenge/votes-for
                                                                          :challenge/votes-against
                                                                          [:challenge/vote {:vote/voter @active-account}
                                                                           [:vote/option]]]]]]]}
                                         {:refetch-on #{::param-change/create-param-change-success
                                                        ::registry-entry/challenge-success}}])
              now (ui-utils/now-in-seconds)]
         [:ul.proposal-list
          (doall
           (for [pc (-> @proposals-subs :search-param-changes :items)]
             ^{:key (:reg-entry/address pc)}
             [:li [proposed-change
                   pc
                   {:action-child (let [entry-status (param-change-status @now pc)]
                                    (cond
                                      (#{:reg-entry.status/challenge-period} entry-status) [challenge-action pc]
                                      (#{:reg-entry.status/reveal-period} entry-status)    [reveal-action pc]
                                      (#{:reg-entry.status/commit-period} entry-status)    [vote-action pc]
                                      (#{:reg-entry.status/whitelisted} entry-status)      [apply-change-action pc]))}]]))])))))

(defn resolved-proposals-list []
  (let [active-account (subscribe [::accounts-subs/active-account])]
    (fn []
      (when @active-account
        (let [proposals-subs (subscribe [::gql/query {:queries [[:search-param-changes {:order-dir :desc
                                                                                        :order-by :param-changes.order-by/created-on
                                                                                        :statuses [:reg-entry.status/blacklisted
                                                                                                   :reg-entry.status/whitelisted]}
                                                                 [[:items (cond-> [:param-change/reason
                                                                                   :param-change/key
                                                                                   :reg-entry/created-on
                                                                                   :param-change/original-value
                                                                                   :param-change/value
                                                                                   :challenge/comment
                                                                                   [:challenge/challenger [:user/address]]
                                                                                   [:reg-entry/creator [:user/address]]
                                                                                   :reg-entry/address
                                                                                   :reg-entry/challenge-period-end
                                                                                   :challenge/commit-period-end
                                                                                   :challenge/reveal-period-end
                                                                                   :challenge/votes-for
                                                                                   :challenge/votes-against
                                                                                   :challenge/votes-total
                                                                                   [:challenge/vote {:vote/voter @active-account}
                                                                                    [:vote/option]]]
                                                                            @active-account (into [[:challenge/all-rewards {:user/address @active-account}
                                                                                                    [:challenge/reward-amount
                                                                                                     :vote/reward-amount]]
                                                                                                   [:challenge/vote-winning-vote-option {:vote/voter @active-account}]
                                                                                                   [:challenge/vote {:vote/voter @active-account}
                                                                                                    [:vote/option
                                                                                                     :vote/amount]]]))]]]]}])]
          [:ul.proposal-list
           (doall
            (for [pc (-> @proposals-subs :search-param-changes :items)]
              ^{:key (:reg-entry/address pc)}
              [:li [proposed-change pc {:action-child [claim-action pc]}]]))])))))

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
