(ns memefactory.ui.dank-registry.challenge-page
  (:require
   [bignumber.core :as bn]
   [cljs-time.extend]
   [cljs-web3.core :as web3]
   [clojure.string :as str]
   [district.ui.component.form.input :refer [select-input with-label text-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.web3-account-balances.subs :as balance-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [district.ui.window-size.subs :as w-size-subs]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.buttons :refer [chain-check-pending-button]]
   [memefactory.ui.components.challenge-list :refer [challenge-list]]
   [memefactory.ui.components.general :refer [dank-with-logo nav-anchor]]
   [memefactory.ui.components.panes :refer [tabbed-pane]]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [memefactory.ui.events :as memefactory-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]))


(defn header []
  (let [active-account (subscribe [::accounts-subs/active-account])]
    (fn []
      (let [account-active? (boolean @active-account)]
        [:div.challenge-info
         [:div.icon]
         [:h2.title "Dank registry - Challenge"]
         [:h3.title "View and Challenge new entries to the registry"]
         [nav-anchor {:route (when account-active? :route.get-dank/index)}
          [:div.get-dank-button
           {:class (when-not account-active? "disabled")}
           [:span "Get Dank"]
           [:img.dank-logo {:src "/assets/icons/dank-logo.svg"}]
           [:img.arrow-icon {:src "/assets/icons/arrow-white-right.svg"}]]]]))))


(defn open-challenge-action [{:keys [:reg-entry/address :meme/title]}]
  (let [form-data (r/atom {})
        open? (r/atom false)
        tx-id (str address "challenges")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-create-challenge tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-create-challenge tx-id}])
        active-account (subscribe [::accounts-subs/active-account])
        errors (reaction {:local (let [{:keys [comment]} @form-data]
                                   (cond-> {}
                                     (str/blank? comment)
                                     (assoc :comment "Challenge reason can't be empty")))})]
    (fn [{:keys [:reg-entry/address]}]
      (let [dank-deposit (:value (:deposit @(subscribe [:memefactory.ui.config/memefactory-db-params])))
            account-balance (subscribe [::balance-subs/active-account-balance :DANK])]
        [:div.challenge-controls
         [:div.vs
          [:img.vs-left {:src "/assets/icons/mememouth.png"}]
          [:span.vs "vs."]
          [:img.vs-right {:src "/assets/icons/mememouth.png"}]]
         (if @open?
           [:div
            [text-input {:form-data form-data
                         :id :comment
                         :disabled (or @tx-success? (not @active-account))
                         :errors errors
                         :placeholder "Challenge Reason..."
                         :class "challenge-reason"
                         :input-type :textarea
                         :maxLength 2000}]
            [chain-check-pending-button {:pending? @tx-pending?
                             :disabled (or @tx-pending? @tx-success? (not (empty? (:local @errors))) (not @active-account))
                             :pending-text "Challenging ..."
                             :on-click #(dispatch [::memefactory-events/add-challenge
                                                   {:send-tx/id tx-id
                                                    :reg-entry/address address
                                                    :tx-description title
                                                    :comment (:comment @form-data)
                                                    :deposit dank-deposit
                                                    :type :meme}])}
             (if @tx-success?
               "Challenged"
               "Challenge")]

            [dank-with-logo (web3/from-wei dank-deposit :ether)]]
           [:button.open-challenge
            {:on-click (when @active-account #(swap! open? not))
             :class [(when (not @active-account) "disabled")]} "Challenge"])
         (when (or (not @active-account) (bn/< @account-balance dank-deposit))
           [:div.not-enough-dank "You don't have enough DANK tokens to challenge this meme"])]))))


(defmethod page :route.dank-registry/challenge []
  [app-layout
   {:meta {:title "MemeFactory - Challenge"
           :description "View and Challenge new entries to the registry. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.dank-registry-challenge
    [:div.challenge-header
     [header]]
    [challenge-list {:include-challenger-info? false
                     :query-params {:statuses [:reg-entry.status/challenge-period]}
                     :action-child open-challenge-action
                     :element-height (if @(subscribe [::w-size-subs/mobile?]) 985.73 523.19)
                     :key :challenge-page
                     ;; HACK : key should be created-on but our select doesn't support two equal keys
                     :sort-options [{:key "challenge-period-end-desc" :value "Newest" :order-dir :desc}
                                    {:key "challenge-period-end-asc" :value "Oldest" :order-dir :asc}]}]]])
