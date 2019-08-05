(ns memefactory.ui.dank-registry.vote-page
  (:require
   [bignumber.core :as bn]
   [cljs-time.extend]
   [cljs-web3.core :as web3]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.parsers :as parsers]
   [district.ui.component.form.input :refer [select-input with-label text-input amount-input pending-button file-drag-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-account-balances.subs :as balance-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [district.ui.window-size.subs :as w-size-subs]
   [goog.string :as gstring]
   [memefactory.ui.events :as events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.buttons :as buttons]
   [memefactory.ui.components.challenge-list :refer [challenge-list]]
   [memefactory.ui.components.charts :as charts]
   [memefactory.ui.components.general :refer [nav-anchor]]
   [memefactory.ui.components.panes :refer [tabbed-pane]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [memefactory.ui.utils :as ui-utils]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log]))


(defn header []
  (let [form-data (r/atom nil)
        active-account (subscribe [::accounts-subs/active-account])]
    (fn []
      (let [account-active? (boolean @active-account)]
        [:div.registry-vote-header
         [:div.icon]
         [:h2.title "Dank registry - VOTE"]
         [:h3.title "View challenges and vote to earn more DANK"]
         [:div.buttons
          [:label.button
           {:on-click (fn [evt]
                        (re-frame/dispatch [::events/backup-vote-secrets]))}
           [:b "BACKUP VOTES"]]
          [:label.button
           [:input {:type :file
                    :on-change (fn [evt]
                                 (let [target (.-currentTarget evt)
                                       file (-> target .-files (aget 0))
                                       reader (js/FileReader.)]
                                   (set! (.-value target) "")
                                   (set! (.-onload reader) (fn [e]
                                                             (re-frame/dispatch [::events/import-vote-secrets (-> e .-target .-result)])))
                                   (.readAsText reader file)))}]
           [:b "IMPORT VOTES"]]]
         [nav-anchor {:route (when account-active? :route.get-dank/index)}
          [:div.get-dank-button
           {:class (when-not account-active? "disabled")}
           [:span "Get Dank"]
           [:img.dank-logo {:src "/assets/icons/dank-logo.svg"}]
           [:img.arrow-icon {:src "/assets/icons/arrow-white-right.svg"}]]]]))))


(defn collect-reward-action [{:keys [:reg-entry/address :challenge/all-rewards] :as meme}]
  (let [active-account (subscribe [::accounts-subs/active-account])
        response (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                     (cond->
                                                         [:reg-entry/address
                                                          :meme/title
                                                          :challenge/votes-for
                                                          :challenge/votes-against
                                                          [:challenge/challenger [:user/address]]
                                                          :challenge/votes-total]
                                                       @active-account (into [[:challenge/all-rewards {:user/address @active-account}
                                                                               [:challenge/reward-amount
                                                                                :vote/reward-amount]]
                                                                              [:challenge/vote-winning-vote-option {:vote/voter @active-account}]
                                                                              [:challenge/vote {:vote/voter @active-account}
                                                                               [:vote/option
                                                                                :vote/amount]]]))]]}])]
    (if (:graphql/loading? @response)
      [spinner/spin]
      (if-let [meme-voting (:meme @response)]
        (let [{:keys [:challenge/reward-amount :vote/reward-amount]} all-rewards
              {:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total :challenge/vote]} meme-voting
              {:keys [:vote/option :vote/amount]
               :or {option "voteOption_noVote"}} vote
              option (graphql-utils/gql-name->kw option)]
          (if (and (zero? votes-for)
                   (zero? votes-against))
            [:div.collect-reward
             [:div "This challenge didn't receive any votes"]
             (when @active-account (buttons/reclaim-buttons @active-account meme-voting))]
            [:div.collect-reward
             [charts/donut-chart meme-voting]
             (into
              [:ul.vote-info
               [:li
                (gstring/format "Voted Dank: %s (%f) "
                                (format/format-percentage (or votes-for 0) votes-total)
                                (format/format-number (bn/number (web3/from-wei (or votes-for 0) :ether))))]
               [:li
                (gstring/format "Voted Stank: %s (%f) "
                                (format/format-percentage (or votes-against 0) votes-total)
                                (format/format-number (bn/number (web3/from-wei (or votes-against 0) :ether))))]
               [:li (gstring/format "Total voted: %f" (format/format-number (bn/number (web3/from-wei (or votes-total 0) :ether))))]
               (when-not (or (= option :vote-option/not-revealed)
                             (= option :vote-option/no-vote))
                 [:li (str "You voted: " (gstring/format "%f for %s (%s)"
                                                         (if (pos? amount)
                                                           (format/format-number (bn/number (web3/from-wei amount :ether)))
                                                           0)
                                                         (case option
                                                           :vote-option/vote-for "DANK"
                                                           :vote-option/vote-against "STANK")
                                                         (format/format-percentage
                                                           amount
                                                           (case option
                                                             :vote-option/vote-for votes-for
                                                             :vote-option/vote-against votes-against))))])]
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

             (when @active-account (buttons/reclaim-buttons @active-account meme-voting))]))))))


(defn vote-action [{:keys [:reg-entry/address :challenge/vote :meme/title] :as meme}]
  (let [tx-id (str address "vote")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-commit-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-commit-vote tx-id}])
        form-data (r/atom {:amount-vote-for nil, :amount-vote-against nil})
        errors (reaction {:local (let [{:keys [amount-vote-for amount-vote-against]} @form-data]
                                   (cond-> {}
                                     (not (pos? (parsers/parse-float amount-vote-for)))
                                     (assoc :amount-vote-for "Amount to vote for should be a positive number")

                                     (not (pos? (parsers/parse-float amount-vote-against)))
                                     (assoc :amount-vote-against "Amount to vote against should be a positive number")))})
        active-account (subscribe [::accounts-subs/active-account])]
    (fn [{:keys [:reg-entry/address :challenge/vote] :as meme}]
      (let [voted? (or (pos? (:vote/amount vote))
                       @tx-pending?
                       @tx-success?)
            account-balance @(subscribe [::balance-subs/active-account-balance :DANK])
            disabled? (or (not @active-account) (bn/<= account-balance 0))]
        [:div.vote
         [:div.vote-dank
          [:div.vote-input
           [with-label "Amount "
            [text-input {:form-data form-data
                         :id :amount-vote-for
                         :disabled disabled?
                         :dom-id (str address :amount-vote-for)
                         :errors errors
                         :type :number}]
            {:form-data form-data
             :for (str address :amount-vote-for)
             :id :amount-vote-for}]
           [:span "DANK"]]
          [pending-button {:pending? @tx-pending?
                           :pending-text "Voting ..."
                           :disabled (or voted? (-> @errors :local :amount-vote-for) disabled?)
                           :on-click (fn []
                                       (dispatch [::registry-entry/approve-and-commit-vote
                                                  {:send-tx/id tx-id
                                                   :reg-entry/address address
                                                   :vote/option :vote.option/vote-for
                                                   :vote/amount (-> @form-data
                                                                  :amount-vote-for
                                                                  parsers/parse-float
                                                                  (web3/to-wei :ether))
                                                   :tx-description title
                                                   :option-desc {:vote.option/vote-against "stank"
                                                                 :vote.option/vote-for     "dank"}
                                                   :type :meme}]))}
           [:i.vote-dank]
           (if voted? "Voted ""Vote Dank")]]
         [:div.vote-stank
          [:div.vote-input
           [with-label "Amount "
            [text-input {:form-data form-data
                         :id :amount-vote-against
                         :disabled disabled?
                         :dom-id (str address :amount-vote-against)
                         :errors errors
                         :type :number}]
            {:form-data form-data
             :for (str address :amount-vote-against)
             :id :amount-vote-against}]
           [:span "DANK"]]
          [pending-button {:pending? @tx-pending?
                           :pending-text "Voting ..."
                           :disabled (or voted? (-> @errors :local :amount-vote-against) disabled?)
                           :on-click (fn []
                                       (dispatch [::registry-entry/approve-and-commit-vote
                                                  {:send-tx/id tx-id
                                                   :reg-entry/address address
                                                   :vote/option :vote.option/vote-against
                                                   :tx-description title
                                                   :type :meme
                                                   :option-desc {:vote.option/vote-against "stank"
                                                                 :vote.option/vote-for     "dank"}
                                                   :vote/amount (-> @form-data
                                                                  :amount-vote-against
                                                                  parsers/parse-float
                                                                  (web3/to-wei :ether))}]))}
           (if voted? "Voted" "Vote Stank")]]
         (if (bn/> account-balance 0)
           [:<>
            [:p.max-vote-tokens (gstring/format "You can vote with up to %s tokens."
                                                (ui-utils/format-dank account-balance))]
            [:p.token-return  "Tokens will be returned to you after revealing your vote."]
            [:p "A vote secret is stored in your browser, which needs to be used for revealing the vote later."]]
           [:div.not-enough-dank "You don't have any DANK tokens to vote on this meme challenge"])]))))


(defn reveal-action [{:keys [:challenge/vote :reg-entry/address :meme/title] :as meme}]
  (let [tx-id (str "reveal" address)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])
        active-account (subscribe [::accounts-subs/active-account])
        vote-option (when-let [opt (-> meme :challenge/vote :vote/option)]
                      (graphql-utils/gql-name->kw opt))]
    (fn [{:keys [] :as meme}]
      (let [vote (get @(subscribe [:memefactory.ui.subs/votes @active-account]) address)
            disabled? (or @tx-pending? @tx-success? (not vote) (not @active-account))]
        [:div.reveal
         [:img {:src "/assets/icons/mememouth.png"}]
         [:div.button-wrapper
          [pending-button {:pending? @tx-pending?
                           :pending-text "Revealing ..."
                           :disabled disabled?
                           :on-click (fn []
                                       (dispatch [::registry-entry/reveal-vote
                                                  {:send-tx/id tx-id
                                                   :reg-entry/address address
                                                   :tx-description title
                                                   :option-desc {:vote.option/vote-against "stank"
                                                                 :vote.option/vote-for     "dank"}}
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
  (case  (graphql-utils/gql-name->kw status)
    :reg-entry.status/commit-period [vote-action meme]
    :reg-entry.status/reveal-period [reveal-action meme]
    ;; TODO we should't need this extra case, but this component is
    ;; being rendered with old subscription value
    [:div]))


(defmethod page :route.dank-registry/vote []
  (let [account @(subscribe [::accounts-subs/active-account])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory - Vote"
               :description "View challenges and vote to earn more DANK. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
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
                                      :element-height (if @(subscribe [::w-size-subs/mobile?]) 1193.55 523.19)
                                      :key :vote-page/open
                                      ;; HACK : key should be created-on but our select doesn't support two equal keys
                                      :sort-options [{:key "commit-period-end-desc" :value "Newest" :order-dir :desc}
                                                     {:key "commit-period-end-asc"  :value "Oldest" :order-dir :asc}]}]
            :route :route.dank-registry/vote}
           {:title "Resolved Challenges"
            :content [challenge-list {:include-challenger-info? true
                                      :query-params {:statuses [:reg-entry.status/blacklisted
                                                                :reg-entry.status/whitelisted]
                                                     :challenged true}
                                      :active-account account
                                      :action-child collect-reward-action
                                      :element-height (if @(subscribe [::w-size-subs/mobile?]) 1136.92 523.19)
                                      :key :vote-page/resolved
                                      ;; HACK : key should be created-on but our select doesn't support two equal keys
                                      :sort-options [{:key "reveal-period-end-desc" :value "Newest" :order-dir :desc}
                                                     {:key "reveal-period-end-asc"  :value "Oldest" :order-dir :asc}]}]
            :route :route.dank-registry/vote}]]]]])))
