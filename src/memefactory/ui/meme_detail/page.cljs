(ns memefactory.ui.meme-detail.page
  (:require
   [cljs-time.core :as t]
   [cljs-time.format :as time-format]
   [cljs-web3.core :as web3]
   [cljs.core.match :refer-macros [match]]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.time :as time]
   [district.ui.component.form.input :as inputs]
   [district.ui.component.page :refer [page]]
   [district.ui.component.tx-button :as tx-button]
   [bignumber.core :as bn]
   [district.ui.graphql.events :as gql-events]
   [district.ui.graphql.subs :as gql]
   [district.ui.now.subs :as now-subs]
   [district.ui.router.events :as router-events]
   [district.ui.router.subs :as router-subs]
   [district.ui.web3-account-balances.subs :as account-balances-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [memefactory.shared.utils :as shared-utils :refer [not-nil?]]
   [memefactory.ui.components.app-layout :as app-layout]
   [memefactory.ui.components.charts :as charts]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.panels :refer [panel]]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.contract.meme-factory :as meme-factory]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [memefactory.ui.events :as memefactory-events]
   [memefactory.ui.spec :as spec]
   [memefactory.ui.utils :as ui-utils :refer [format-price format-dank]]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :as ratom]
   [taoensso.timbre :as log]))

(def description "Lorem ipsum dolor sit amet, consectetur adipiscing elit")

(def scroll-interval 5)

(def time-formatter (time-format/formatter "EEEE, ddo MMMM, yyyy 'at' HH:mm:ss Z"))

(defn build-meme-query [address active-account]
  {:queries [[:meme {:reg-entry/address address}
              [:reg-entry/address
               :reg-entry/status
               :reg-entry/deposit
               :meme/image-hash
               :meme/meta-hash
               :meme/number
               :meme/title
               :meme/total-supply

               [:meme/tags
                [:tag/name]]

               [:meme/owned-meme-tokens {:owner active-account}
                [:meme-token/token-id]]

               [:reg-entry/creator
                [:user/address
                 :user/total-created-memes
                 :user/total-created-memes-whitelisted
                 :user/creator-rank]]

               :challenge/created-on
               :challenge/commit-period-end
               :challenge/reveal-period-end
               :challenge/comment
               :challenge/votes-for
               :challenge/votes-against
               :challenge/votes-total

               [:challenge/challenger
                [:user/address
                 :user/challenger-rank
                 :user/challenger-total-earned
                 :user/total-created-challenges
                 :user/total-created-challenges-success]]

               [:challenge/vote {:vote/voter active-account}
                [:vote/option
                 :vote/reward
                 :vote/claimed-reward-on
                 :vote/reclaimed-amount-on]]]]]})

(defn meme-creator-component [{:keys [:user/address :user/creator-rank :user/total-created-memes
                                      :user/total-created-memes-whitelisted] :as creator}]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller address :statuses [:meme-auction.status/done]}
                                      [[:items [:meme-auction/start-price
                                                :meme-auction/end-price
                                                :meme-auction/bought-for]]]]]}])
        creator-total-earned (reduce (fn [total-earned {:keys [:meme-auction/end-price] :as meme-auction}]
                                       (+ total-earned end-price))
                                     0
                                     (-> @query :search-meme-auctions :items))]
    [:div.creator
     [:b "Creator"]
     [:div.rank (str "Rank: #" creator-rank " ("
                     (format/format-token creator-total-earned {:token "DANK"}) ")")]
     [:div.success (str "Success rate: " total-created-memes-whitelisted "/" total-created-memes " ("
                        (format/format-percentage total-created-memes-whitelisted total-created-memes) ")")]
     [:div.address (str "Address: " address)]]))

(defn related-memes-component [state]
  (panel :selling state))

(defn related-memes-container [address tags]
  (let [safe-mapcat (fn [f queries] (loop [queries queries
                                           result []]
                                      (if (empty? queries)
                                        result
                                        (recur (rest queries)
                                               (let [not-contains? (complement contains?)
                                                     new (filter #(not-contains? (set result) %)
                                                                 (f (first queries)))]
                                                 (into result new))))))
        build-query (fn [{:keys [:first :after]}]
                      [[:search-meme-auctions {:tags-or tags
                                               :statuses [:meme-auction.status/done]
                                               :after (str after)
                                               :first first}
                        [:total-count
                         :end-cursor
                         :has-next-page
                         [:items [:meme-auction/address
                                  :meme-auction/status
                                  :meme-auction/start-price
                                  :meme-auction/end-price
                                  :meme-auction/bought-for
                                  [:meme-auction/meme-token
                                   [:meme-token/number
                                    [:meme-token/meme
                                     [:reg-entry/address
                                      :meme/title
                                      :meme/image-hash
                                      :meme/meta-hash
                                      :meme/total-minted]]]]]]]]])
        response (subscribe [::gql/query {:queries (build-query {:after 0
                                                                 :first scroll-interval})}
                             {:id address}])
        state (safe-mapcat (fn [q] (get-in q [:search-meme-auctions :items])) @response)]

    (log/debug "Related memes" {:address address :tags tags :resp @response})

    [:div.scroll-area
     [related-memes-component state]
     [infinite-scroll {:load-fn (fn []
                                  (when-not (:graphql/loading? @response)
                                    (let [{:keys [:has-next-page :end-cursor]} (:search-meme-auctions (last @response))]
                                      (when (or has-next-page (empty? state))
                                        (dispatch [::gql-events/query
                                                   {:query {:queries (build-query {:first scroll-interval
                                                                                   :after (or end-cursor 0)})}
                                                    :id address}])))))}]]))

(defn history-component [address]
  (let [now (subscribe [::now-subs/now])
        order-by (r/atom :meme-auctions.order-by/token-id)
        flip-ordering #(reset! order-by %)]
    (fn []
      (let [query (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                      [[:meme/meme-auctions {:order-by @order-by}
                                                        [:meme-auction/address
                                                         :meme-auction/end-price
                                                         :meme-auction/bought-on
                                                         [:meme-auction/seller
                                                          [:user/address]]
                                                         [:meme-auction/buyer
                                                          [:user/address]]
                                                         [:meme-auction/meme-token
                                                          [:meme-token/token-id]]]]]]]}])]
        [:div.history-component
         [:h1.title "Marketplace history"]
         [:table
          [:thead [:tr
                   [:th {:class (if (:meme-auctions.order-by/token-id @order-by) :up :down)
                         :on-click #(flip-ordering :meme-auctions.order-by/token-id)} "Card Number"]
                   [:th {:class (if (:meme-auctions.order-by/seller @order-by) :up :down)
                         :on-click #(flip-ordering :meme-auctions.order-by/seller)} "Seller"]
                   [:th {:class (if (:meme-auctions.order-by/buyer @order-by) :up :down)
                         :on-click #(flip-ordering :meme-auctions.order-by/buyer)} "Buyer"]
                   [:th {:class (if (:meme-auctions.order-by/price @order-by) :up :down)
                         :on-click #(flip-ordering :meme-auctions.order-by/price)} "Price"]
                   [:th {:class (if (:meme-auctions.order-by/bought-on @order-by) :up :down)
                         :on-click #(flip-ordering :meme-auctions.order-by/bought-on)} "Time Ago"]]]
          (if-not (:graphql/loading? @query)
            [:tbody
             (doall
              (for [{:keys [:meme-auction/address :meme-auction/end-price :meme-auction/bought-on
                            :meme-auction/meme-token :meme-auction/seller :meme-auction/buyer] :as auction} (-> @query :meme :meme/meme-auctions)]
                (when address
                  ^{:key address}
                  [:tr
                   [:td.meme-token (:meme-token/token-id meme-token)]
                   [:td.seller-address (:user/address seller)]
                   [:td.buyer-address (:user/address buyer)]
                   [:td.end-price (format-price end-price)]
                   [:td.time  (format/time-ago (ui-utils/gql-date->date bought-on) (t/date-time @now))]])))])]]))))

(defn challenge-header [created-on]
  [:div.header
   (if created-on
     [:h2.title
      (str "This meme was challenged on " (time-format/unparse time-formatter (t/local-date-time (ui-utils/gql-date->date created-on))))]
     [:div
      [:h4.title "This meme hasn't been challenged."]])])

(defn challenger-component [{:keys [:challenge/comment :challenge/challenger] :as meme}]
  (let [{:keys [:user/challenger-rank :user/challenger-total-earned
                :user/total-created-challenges :user/total-created-challenges-success]} challenger]
    [:div.challenger
     [:b "Challenger"]
     [:div.rank (str "Rank: #" challenger-rank " ("
                     (format/format-token challenger-total-earned {:token "DANK"}) ")")]
     [:div.success (str "Success rate: " total-created-challenges-success "/" total-created-challenges " ("
                        (format/format-percentage total-created-challenges-success total-created-challenges) ")")]
     [:div.address (str "Address: " (:user/address challenger))]
     [:i (str "\"" comment "\"")]]))

(defn status-component [status]
  [:div.status
   [:b "Challenge status"]
   [:div.description (case (graphql-utils/gql-name->kw status)
                       :reg-entry.status/whitelisted "Resolved, accepted"
                       :reg-entry.status/blacklisted "Resolved, rejected"
                       :reg-entry.status/challenge-period "Challenge period running"
                       :reg-entry.status/commit-period "Commit vote period running"
                       :reg-entry.status/reveal-period "Reveal vote period running"
                       status)]])

(defn votes-component [{:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total
                               :challenge/challenger :reg-entry/creator :challenge/vote] :as meme}]
  (let [{:keys [:vote/option :vote/reward :vote/claimed-reward-on :vote/reclaimed-amount-on]} vote
        active-account (subscribe [::accounts-subs/active-account])
        option (graphql-utils/gql-name->kw option)
        reward (if (nil? reward) 0 reward)
        tx-id (:reg-entry/address meme)
        claim-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-vote-reward tx-id}])
        claim-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-vote-reward tx-id}])
        reclaim-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reclaim-vote-amount tx-id}])
        reclaim-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reclaim-vote-amount tx-id}])]
    [:div.votes
     (when (< 0 votes-total)
       [charts/donut-chart meme])
     [:div.votes-inner
      [:div.text (str "Voted Dank: " (format/format-percentage votes-for votes-total) " - " (if votes-for (/ votes-for 1e18) 0))]
      [:div.text (str "Voted Stank: " (format/format-percentage votes-against votes-total) " - " (if votes-against (/ votes-against 1e18) 0))]
      [:div.text (str "Total voted: " (if votes-total (/ votes-total 1e18) 0))]
      (cond
        (= :vote-option/not-revealed option)
        [tx-button/tx-button {:primary true
                              :disabled  (or (not-nil? reclaimed-amount-on)
                                             @reclaim-tx-success?)
                              :pending? @reclaim-tx-pending?
                              :pending-text "Collect reward..."
                              :on-click #(dispatch [::registry-entry/reclaim-vote-amount {:send-tx/id tx-id
                                                                                          :reg-entry/address (:reg-entry/address meme)}])}
         "Collect reward"]

        (contains? #{:vote-option/vote-for :vote-option/vote-against} option)
        [:div
         [:div.text (str "You voted: " (case option
                                         :vote-option/vote-for "DANK"
                                         :vote-option/vote-against "STANK"))]
         [:div.text (str "Your reward: " (format/format-token reward {:token "DANK"}))]
         (when-not (= 0 reward)
           [tx-button/tx-button {:primary true
                                 :disabled (or (= 0 reward)
                                               (not-nil? claimed-reward-on)
                                               @claim-tx-success?)
                                 :pending? @claim-tx-pending?
                                 :pending-text "Collecting reward..."
                                 :on-click #(dispatch [::registry-entry/claim-vote-reward {:send-tx/id tx-id
                                                                                           :reg-entry/address (:reg-entry/address meme)
                                                                                           :from (case option
                                                                                                   :vote-option/vote-for (:user/address challenger)
                                                                                                   :vote-option/vote-against (:user/address creator))}])}
            "Collect Reward"])])]]))

(defn challenge-meme-component [{:keys [:reg-entry/deposit] :as meme} dank-deposit]
  (let [form-data (r/atom {:challenge/comment nil})
        errors (ratom/reaction {:local (when-not (spec/check ::spec/challenge-comment (:challenge/comment @form-data))
                                         {:challenge/comment "Comment shouldn't be empty."})})
        tx-id (:reg-entry/address meme)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-create-challenge tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-create-challenge tx-id}])]
    (fn []
      [:div
       [:b "Challenge explanation"]
       [inputs/textarea-input {:form-data form-data
                               :id :challenge/comment
                               :errors errors}]
       [:div (format/format-token deposit {:token "DANK"})]
       [tx-button/tx-button {:primary true
                             :disabled (or @tx-success? (not (empty? (:local @errors))))
                             :pending? @tx-pending?
                             :pending-text "Challenging..."
                             :on-click #(dispatch [::memefactory-events/add-challenge {:send-tx/id tx-id
                                                                                       :reg-entry/address (:reg-entry/address meme)
                                                                                       :comment (:challenge/comment @form-data)
                                                                                       :deposit dank-deposit}])}
        "Challenge"]])))

(defn remaining-time-component [to-time]
  (let [time-remaining (subscribe [::now-subs/time-remaining to-time])
        {:keys [:days :hours :minutes :seconds]} @time-remaining]
    [:b (str (format/pluralize days "Day") ", "
             hours " Hr. "
             minutes " Min. "
             seconds " Sec.")]))

(defn reveal-vote-component [{:keys [:challenge/reveal-period-end :challenge/vote] :as meme}]
  (let [tx-id (:reg-entry/address meme)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])]
    (fn []
      [:div.reveal
       [:div description]
       [remaining-time-component (ui-utils/gql-date->date reveal-period-end)]
       [tx-button/tx-button {:primary true
                             :disabled @tx-success?
                             :pending? @tx-pending?
                             :pending-text "Revealing..."
                             :on-click #(dispatch [::registry-entry/reveal-vote
                                                   {:send-tx/id tx-id
                                                    :reg-entry/address (:reg-entry/address meme)}
                                                   vote])}
        "Reveal My Vote"]])))

(defn vote-component [{:keys [:challenge/commit-period-end] :as meme}]
  (let [balance-dank (subscribe [::account-balances-subs/active-account-balance :DANK])
        form-data (r/atom {:vote/amount-for nil
                           :vote/amount-against nil})
        errors (ratom/reaction {:local (let [amount-for (-> @form-data :vote/amount-for js/parseInt)
                                             amount-against (-> @form-data :vote/amount-against js/parseInt)]
                                         (cond-> {}
                                           (or (not (spec/check ::spec/pos-int amount-for))
                                               (< @balance-dank amount-for))
                                           (assoc :vote/amount-for (str "Amount should be between 0 and " @balance-dank))

                                           (or (not (spec/check ::spec/pos-int amount-against))
                                               (< @balance-dank amount-against))
                                           (assoc :vote/amount-against (str "Amount should be between 0 and " @balance-dank))))})
        tx-id (:reg-entry/address meme)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-commit-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-commit-vote tx-id}])]
    (fn []
      [:div.vote
       [:div description]
       [remaining-time-component (ui-utils/gql-date->date commit-period-end)]
       [inputs/with-label "Amount "
        [:div.vote-dank
         [inputs/amount-input {:form-data form-data
                               :id :vote/amount-for
                               :errors errors}]
         [inputs/pending-button
          {:pending? @tx-pending?
           :disabled (or (-> @errors :local :vote/amount-for empty? not)
                         @tx-success?)
           :pending-text "Voting..."
           :on-click #(dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                           :reg-entry/address (:reg-entry/address meme)
                                                                           :vote/option :vote.option/vote-for
                                                                           :vote/amount (-> @form-data :vote/amount-for js/parseInt)}])}
          "Vote Dank"]]
        {:form-data form-data
         :id :vote/amount-for}]
       [inputs/with-label "Amount "
        [:div.vote-stank
         [inputs/amount-input {:form-data form-data
                               :id :vote/amount-against
                               :errors errors}]
         [inputs/pending-button
          {:pending? @tx-pending?
           :disabled (or (-> @errors :local :vote/amount-against empty? not)
                         @tx-success?)
           :pending-text "Voting..."
           :on-click #(dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                           :reg-entry/address (:reg-entry/address meme)
                                                                           :vote/option :vote.option/vote-against
                                                                           :vote/amount (-> @form-data :vote/amount-against js/parseInt)}])}
          "Vote Stank"]]
        {:form-data form-data
         :id :vote/amount-against}]
       [:div "You can vote with up to " (format/format-token @balance-dank {:token "DANK"})]
       [:div "Token will be returned to you after revealing your vote."]])))

(defmulti challenge-component (fn [meme] (match [(-> meme :reg-entry/status graphql-utils/gql-name->kw)]
                                                [(:or :reg-entry.status/whitelisted :reg-entry.status/blacklisted)] [:reg-entry.status/whitelisted :reg-entry.status/blacklisted]
                                                [:reg-entry.status/challenge-period] :reg-entry.status/challenge-period
                                                [:reg-entry.status/commit-period] :reg-entry.status/commit-period
                                                [:reg-entry.status/reveal-period] :reg-entry.status/reveal-period)))

(defmethod challenge-component :reg-entry.status/reveal-period
  [{:keys [:challenge/created-on :reg-entry/status] :as meme}]

  (cond-> [:div.challenge-component
           [challenge-header created-on]]
    created-on (into [[status-component status]
                      [challenger-component meme]
                      [reveal-vote-component meme]])))

(defmethod challenge-component :reg-entry.status/commit-period
  [{:keys [:challenge/created-on :reg-entry/status] :as meme}]
  (cond-> [:div.challenge-component
           [challenge-header created-on]]
    created-on (into [[status-component status]
                      [challenger-component meme]
                      [vote-component meme]])))

(defmethod challenge-component :reg-entry.status/challenge-period
  [{:keys [:challenge/created-on :reg-entry/status] :as meme}]

  (when-let [params @(subscribe [:memefactory.ui.config/memefactory-db-params])]
    (cond-> [:div.challenge-component
             [challenge-header created-on]]
      created-on (into [[status-component status]
                        [challenge-meme-component meme (:deposit params)]]))))

(defmethod challenge-component [:reg-entry.status/whitelisted :reg-entry.status/blacklisted]
  [{:keys [:challenge/created-on :reg-entry/status] :as meme}]
  [:div.challenge-component
   [:h1.title "Challenge"]
   (cond-> [:div.challenge-component-inner
            [challenge-header created-on]]
     created-on (into [[status-component status]
                       [challenger-component meme]
                       [votes-component meme]]))])

(defmethod page :route.meme-detail/index []
  (let [active-account @(subscribe [::accounts-subs/active-account])
        address (-> @(re-frame/subscribe [::router-subs/active-page]) :query :address)
        response (if active-account
                   (subscribe [::gql/query (build-meme-query address active-account)])
                   (ratom/reaction {:graphql/loading? true}))
        meme (-> @response :meme)
        {:keys [:reg-entry/status :meme/image-hash :meme/title :meme/number :reg-entry/status :meme/total-supply
                :meme/tags :meme/owned-meme-tokens :reg-entry/creator :challenge/challenger]} meme
        token-count (->> owned-meme-tokens
                         (map :meme-token/token-id)
                         (filter shared-utils/not-nil?)
                         count)
        tags (mapv :tag/name tags)]

    (log/debug "Response" @response)

    [app-layout/app-layout
     {:meta {:title "MemeFactory"
             :description "Description"}}
     [:div.meme-detail-page
      [:section.meme-detail
       [:div.meme-info
        [:div.meme-number (if number
                            (str "#" number)
                            [:div.spinner.spinner--number])]
        [:div.container
         [tiles/meme-image image-hash]]
        (if-not status
          [:div.spinner.spinner--info]
          [:div.registry
           [:h1 title]
           [:div.status (case (graphql-utils/gql-name->kw status)
                          :reg-entry.status/whitelisted [:label.in-registry "In Registry"]
                          :reg-entry.status/blacklisted [:label.rejected "Rejected"]
                          [:label.challenged "Challenged"])]
           [:div.description description]
           [:div.text (format/pluralize total-supply "card")]
           [:div.text (str "You own " token-count)]
           [meme-creator-component creator]
           [:div.tags
            (for [tag-name tags]
              ^{:key tag-name} [:button {:on-click #(dispatch [::router-events/navigate
                                                               :route.marketplace/index
                                                               nil
                                                               {:search-tags [tag-name]}])} tag-name])]
           [:div.buttons
            [:button.search.marketplace {:on-click #(dispatch [::router-events/navigate
                                                               :route.marketplace/index
                                                               nil
                                                               {:term title}])} "Search On Marketplace"]
            [:button.search.memefolio {:on-click #(dispatch [::router-events/navigate
                                                             :route.memefolio/index
                                                             nil
                                                             {:term title}])} "Search On Memefolio"]]])]]
      [:section.history
       [history-component address]]
      [:section.challenge
       (if status
         [challenge-component meme]
         [:div.spinner.spinner--challenge])]
      [:section.related
       [:div.relateds-panel
        [:div.icon]
        [:h2.title "REPLATED MEMES ON MARKETPLACE"]
        [:h3.title "lorem ipsum"]
        [related-memes-container address tags]]]]]))
