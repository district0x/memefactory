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
   [memefactory.ui.components.spinner :as spinner]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :as ratom]
   [taoensso.timbre :as log]
   [goog.string :as gstring]
   [memefactory.ui.components.buttons :as buttons]
   [memefactory.ui.dank-registry.vote-page :as vote-page]))

(def description "Lorem ipsum dolor sit amet, consectetur adipiscing elit")

(def scroll-interval 5)

(def time-formatter (time-format/formatter "EEEE, ddo MMMM, yyyy 'at' HH:mm:ss Z"))

(defn build-meme-query [address active-account]
  {:queries [[:meme {:reg-entry/address address}
              (cond-> [:reg-entry/address
                       :reg-entry/status
                       :reg-entry/deposit
                       :reg-entry/challenge-period-end
                       :meme/image-hash
                       :meme/meta-hash
                       :meme/number
                       :meme/title
                       :meme/total-supply
                       [:meme/tags
                        [:tag/name]]
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
                         :user/total-created-challenges-success]]]
                active-account (into [[:meme/owned-meme-tokens {:owner active-account}
                                       [:meme-token/token-id]]
                                      [:challenge/all-rewards {:user/address active-account}
                                       [:challenge/reward-amount
                                        :vote/reward-amount]][:challenge/vote-winning-vote-option {:vote/voter active-account}]
                                      [:challenge/vote {:vote/voter active-account}
                                       [:vote/option
                                        :vote/reward
                                        :vote/amount
                                        :vote/claimed-reward-on
                                        :vote/reclaimed-amount-on]]]))]]})

(defn meme-creator-component [{:keys [:user/address :user/creator-rank :user/total-created-memes
                                      :user/total-created-memes-whitelisted] :as creator}]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller address :statuses [:meme-auction.status/done]}
                                      [[:items [:meme-auction/start-price
                                                :meme-auction/end-price
                                                :meme-auction/bought-for]]]]]}])
        creator-total-earned (quot (reduce (fn [total-earned {:keys [:meme-auction/end-price] :as meme-auction}]
                                        (+ total-earned end-price))
                                      0
                                      (-> @query :search-meme-auctions :items))
                                   1e18)]
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
                                      :meme/number
                                      :meme/image-hash
                                      :meme/meta-hash
                                      :meme/total-minted]]]]]]]]])
        response (subscribe [::gql/query {:queries (build-query {:after 0
                                                                 :first scroll-interval})}
                             {:id address}])
        state (->> @response
                   (mapcat (fn [q] (get-in q [:search-meme-auctions :items])))
                   (remove #(= (-> % :meme-auction/meme-token :meme-token/meme :reg-entry/address) address)))]

    (log/debug "Related memes" {:address address :tags tags :resp @response})

    [:div.scroll-area
     [related-memes-component state]
     [infinite-scroll {:load-fn (fn []
                                  (when-not (:graphql/loading? @response)
                                    (let [{:keys [:has-next-page :end-cursor]} (:search-meme-auctions (last @response))]
                                      (when has-next-page
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
                                                      [[:meme/meme-auctions {:order-by @order-by
                                                                             :completed :true}
                                                        [:meme-auction/address
                                                         :meme-auction/bought-for
                                                         :meme-auction/bought-on
                                                         [:meme-auction/seller
                                                          [:user/address]]
                                                         [:meme-auction/buyer
                                                          [:user/address]]
                                                         [:meme-auction/meme-token
                                                          [:meme-token/token-id]]]]]]]}])
            all-auctions (-> @query :meme :meme/meme-auctions)]

        [:div
         [:h1.title "Marketplace history"]
         (if (:graphql/loading? @query)
           [spinner/spin]
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
            (if (empty? all-auctions)
              [:tbody [:tr [:td {:colSpan 5} "This meme hasn't been traded yet."]]]
              [:tbody
               (doall
                (for [{:keys [:meme-auction/address :meme-auction/bought-for :meme-auction/bought-on
                              :meme-auction/meme-token :meme-auction/seller :meme-auction/buyer] :as auction} all-auctions]
                  (when address
                    ^{:key address}
                    [:tr
                     [:td.meme-token (:meme-token/token-id meme-token)]
                     [:td.seller-address {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                                {:address (:user/address seller)}
                                                                {:tab :sold}])}
                      (:user/address seller)]
                     [:td.buyer-address {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                               {:address (:user/address buyer)}
                                                               {:tab :collected}])}
                      (:user/address buyer)]
                     [:td.end-price (format-price bought-for)]
                     [:td.time  (let [now-date (t/date-time @now)
                                      bought-date (ui-utils/gql-date->date bought-on)]
                                  (when-not (or (empty? (str bought-on))
                                                (> (.getTime bought-date) (.getTime now-date)))
                                    (format/time-ago bought-date now-date)))]])))])])]))))

(defn challenge-header [created-on]
  (when created-on
    [:div.header
     [:h2.title
      (str "This meme was challenged on " (time-format/unparse time-formatter (t/local-date-time (ui-utils/gql-date->date created-on))))]]))

(defn challenger-component [{:keys [:challenge/comment :challenge/challenger] :as meme}]
  (let [{:keys [:user/challenger-rank :user/challenger-total-earned
                :user/total-created-challenges :user/total-created-challenges-success]} challenger]
    [:div.challenger
     [:b "Challenger"]
     [:div.rank (str "Rank: #" challenger-rank " ("
                     (format/format-token challenger-total-earned {:token "DANK"}) ")")]
     [:div.success (str "Success rate: " total-created-challenges-success "/" total-created-challenges " ("
                        (format/format-percentage total-created-challenges-success total-created-challenges) ")")]
     [:div.address {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                          {:address (:user/address challenger)}
                                          {:tab :curated}])}
      (str "Address: " (:user/address challenger))]
     [:i (str "\"" comment "\"")]]))

(defn status-component [{:keys [:reg-entry/status :reg-entry/challenge-period-end :challenge/commit-period-end :challenge/reveal-period-end] :as meme} text]
  (let [status (graphql-utils/gql-name->kw status)
        [period-label period-end] (case status
                                    :reg-entry.status/challenge-period ["Challenge" challenge-period-end]
                                    :reg-entry.status/commit-period    ["Voting" commit-period-end]
                                    :reg-entry.status/reveal-period    ["Reveal" reveal-period-end]
                                    nil)
        {:keys [days hours minutes seconds] :as time-remaining} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                                     (ui-utils/gql-date->date period-end))]
    [:ul.status
     [:li "Challenge status"
      [:div (case status
              :reg-entry.status/whitelisted "Resolved, accepted"
              :reg-entry.status/blacklisted "Resolved, rejected"
              :reg-entry.status/challenge-period "Challenge period running"
              :reg-entry.status/commit-period "Commit vote period running"
              :reg-entry.status/reveal-period "Reveal vote period running"
              status)]]

     (if period-end
       (if (= 0 days hours minutes seconds)
         [:li (str period-label " period ends in ")
          [:div (str period-label " period ended.")]]

         [:li (str period-label " period ends in ")
          [:div (-> time-remaining
                     format/format-time-units)]])
       [:li ""])

     [:div.lorem text]]))

(defn votes-component [{:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total
                               :challenge/challenger :reg-entry/creator :challenge/vote] :as meme}]
  (let [{:keys [:vote/option :vote/reward :vote/claimed-reward-on :vote/reclaimed-amount-on :vote/amount]} vote
        active-account (subscribe [::accounts-subs/active-account])
        option (graphql-utils/gql-name->kw option)
        reward (if (nil? reward) 0 (quot reward 1e18))
        tx-id (:reg-entry/address meme)
        claim-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/claim-vote-reward tx-id}])
        claim-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/claim-vote-reward tx-id}])
        reclaim-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reclaim-vote-amount tx-id}])
        reclaim-tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reclaim-vote-amount tx-id}])]
    [:div.votes
     (when (< 0 votes-total)
       [charts/donut-chart meme])
     [:div.votes-inner
      [:div.text (str "Voted Dank: " (format/format-percentage votes-for votes-total) " - " (format-dank votes-for))]
      [:div.text (str "Voted Stank: " (format/format-percentage votes-against votes-total) " - " (format-dank votes-against ))]
      [:div.text (str "Total voted: " (format-dank votes-total))]
      (when-not (or (= option :vote-option/not-revealed)
                    (= option :vote-option/no-vote))
        [:div.text (str "You voted: " (gstring/format "%d for %s "
                                                (if (pos? amount)
                                                  (quot amount 1e18)
                                                  0)
                                                (case option
                                                  :vote-option/vote-for "DANK"
                                                  :vote-option/vote-against "STANK")))])
      (buttons/reclaim-buttons @active-account meme)]]))

(defn challenge-meme-component [{:keys [:reg-entry/deposit :meme/title] :as meme} dank-deposit]
  (let [form-data (r/atom {:challenge/comment nil})
        errors (ratom/reaction {:local (when-not (spec/check ::spec/challenge-comment (:challenge/comment @form-data))
                                         {:challenge/comment "Comment shouldn't be empty."})})
        tx-id (str (:reg-entry/address meme) "challenges")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-create-challenge tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-create-challenge tx-id}])]
    (fn []
      [:div
       [:b "Challenge Explanation"]
       [inputs/textarea-input {:form-data form-data
                               :id :challenge/comment
                               :maxLength 2000
                               :errors errors}]
       [:div.controls
        [:div.dank (format/format-token (quot deposit 1e18) {:token "DANK"})]
        [tx-button/tx-button {:primary true
                              :disabled (or @tx-success? (not (empty? (:local @errors))))
                              :pending? @tx-pending?
                              :pending-text "Challenging..."
                              :on-click #(dispatch [::memefactory-events/add-challenge {:send-tx/id tx-id
                                                                                        :reg-entry/address (:reg-entry/address meme)
                                                                                        :comment (:challenge/comment @form-data)
                                                                                        :deposit dank-deposit
                                                                                        :meme/title title}])}
         (if @tx-success?
           "Challenged"
           "Challenge")]]])))

(defn remaining-time-component [to-time]
  (let [time-remaining (subscribe [::now-subs/time-remaining to-time])
        {:keys [:days :hours :minutes :seconds]} @time-remaining]
    [:b (str (format/pluralize days "Day") ", "
             hours " Hr. "
             minutes " Min. "
             seconds " Sec.")]))

(defn reveal-vote-component [{:keys [:challenge/reveal-period-end :challenge/vote :reg-entry/address :meme/title] :as meme}]
  (let [tx-id (str "reveal" address)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])
        active-account @(subscribe [::accounts-subs/active-account])
        vote (get @(subscribe [:memefactory.ui.subs/votes active-account]) address)
        vote-option (-> meme :challenge/vote :vote/option graphql-utils/gql-name->kw)]
    (fn []
      [:div.reveal
       [:div description]
       [remaining-time-component (ui-utils/gql-date->date reveal-period-end)]
       [:div
        [tx-button/tx-button {:primary true
                              :disabled (or @tx-success? (not vote))
                              :pending? @tx-pending?
                              :pending-text "Revealing..."
                              :on-click #(dispatch [::registry-entry/reveal-vote
                                                    {:send-tx/id tx-id
                                                     :reg-entry/address (:reg-entry/address meme)
                                                     :meme/title title}
                                                    vote])}
         (if @tx-success?
           "Revealed"
           "Reveal My Vote")]]
       (when (and (= vote-option :vote-option/not-revealed)
                  (not vote))
         [:div.no-reveal-info "Secret to reveal vote was not found in your browser"])])))

(defn vote-component [{:keys [:challenge/commit-period-end :meme/title] :as meme}]
  (let [balance-dank (subscribe [::account-balances-subs/active-account-balance :DANK])
        form-data (r/atom {:vote/amount-for nil
                           :vote/amount-against nil})

        errors (ratom/reaction {:local (let [amount-for (-> @form-data :vote/amount-for js/parseInt)
                                             amount-against (-> @form-data :vote/amount-against js/parseInt)
                                             balance (if (pos? @balance-dank) (quot @balance-dank 1e18) 0)]
                                         (cond-> {}
                                           (or (not (spec/check ::spec/pos-int amount-for))
                                               (< balance amount-for))
                                           (assoc :vote/amount-for (str "Amount should be between 0 and " balance))

                                           (or (not (spec/check ::spec/pos-int amount-against))
                                               (< balance amount-against))
                                           (assoc :vote/amount-against (str "Amount should be between 0 and " balance))))})

        tx-id (str (:reg-entry/address meme) "vote")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-commit-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-commit-vote tx-id}])]
    (fn []
      [:div.vote
       [:div description]
       #_[remaining-time-component (ui-utils/gql-date->date commit-period-end)]
       [:div.form
        [:div.vote-dank
         [:div.outer
          [inputs/with-label "Amount "
           [inputs/text-input {:form-data form-data
                               :id :vote/amount-for
                               :dom-id :vote/amount-for
                               :errors errors
                               }]
           {:form-data form-data
            :for :vote/amount-for
            :id :vote/amount-for}]
          [:span.unit "DANK"]]

         [inputs/pending-button
          {:pending? @tx-pending?
           :disabled (or (-> @errors :local :vote/amount-for empty? not)
                         @tx-success?)
           :pending-text "Voting..."
           :on-click #(dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                           :reg-entry/address (:reg-entry/address meme)
                                                                           :vote/option :vote.option/vote-for
                                                                           :vote/amount (-> @form-data
                                                                                            :vote/amount-for
                                                                                            js/parseInt
                                                                                            (web3/to-wei :ether))
                                                                           :meme/title title}])}
          (if @tx-success?
            "Voted"
            "Vote Dank")]]

        [:div.vote-stank
         [:div.outer
          [inputs/with-label "Amount "
           [inputs/text-input {:form-data form-data
                               :id :vote/amount-against
                               :dom-id :vote/amount-against
                               :errors errors}]
           {:form-data form-data
            :for :vote/amount-against
            :id :vote/amount-against}]
          [:span.unit "DANK"]]
         [inputs/pending-button
          {:pending? @tx-pending?
           :disabled (or (-> @errors :local :vote/amount-against empty? not)
                         @tx-success?)
           :pending-text "Voting..."
           :on-click #(dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                           :reg-entry/address (:reg-entry/address meme)
                                                                           :vote/option :vote.option/vote-against
                                                                           :vote/amount (-> @form-data
                                                                                            :vote/amount-against
                                                                                            js/parseInt
                                                                                            (web3/to-wei :ether))
                                                                           :meme/title title}])}
          (if @tx-success?
            "Voted"
            "Vote Stank")]]]
       [:div "You can vote with up to " (format/format-token (quot @balance-dank 1e18) {:token "DANK"})]
       [:div "Token will be returned to you after revealing your vote."]])))

(defmulti challenge-component (fn [meme] (match [(-> meme :reg-entry/status graphql-utils/gql-name->kw)]
                                                [(:or :reg-entry.status/whitelisted :reg-entry.status/blacklisted)] [:reg-entry.status/whitelisted :reg-entry.status/blacklisted]
                                                [:reg-entry.status/challenge-period] :reg-entry.status/challenge-period
                                                [:reg-entry.status/commit-period] :reg-entry.status/commit-period
                                                [:reg-entry.status/reveal-period] :reg-entry.status/reveal-period)))

(defmethod challenge-component :reg-entry.status/reveal-period
  [{:keys [:challenge/created-on :reg-entry/status :challenge/reveal-period-end] :as meme}]
  (let [{:keys [days hours minutes seconds]} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                  (ui-utils/gql-date->date reveal-period-end))]
    [:div
     [:h1.title "Challenge"]
     (cond-> [:div.challenge-component-inner
              [challenge-header created-on]]
       created-on (into [[status-component meme "This meme has had its place in the registry challenged, and voting has concluded. Voters must now reveal their votes to determine if the meme is accepted or rejected."]
                         [challenger-component meme]
                         (if-not (= 0 days hours minutes seconds)
                           [reveal-vote-component meme]
                           [:div "Reveal period ended. Please refresh the page."])]))]))

(defmethod challenge-component :reg-entry.status/commit-period
  [{:keys [:challenge/created-on :reg-entry/status :challenge/commit-period-end] :as meme}]

  (let [{:keys [days hours minutes seconds]} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                  (ui-utils/gql-date->date commit-period-end))]
    [:div
     [:h1.title "Challenge"]
     (cond-> [:div.challenge-component-inner
              [challenge-header created-on]]
       created-on (into [[status-component meme "This meme has had its place in the registry challenged. It is currently open for voting. When the voting period ends, it will enter the vote reveal phase."]
                         [challenger-component meme]
                         (if-not (= 0 days hours minutes seconds)
                           [vote-component meme]
                           [:div "Commit period ended. Please refresh the page."])]))]))

(defmethod challenge-component :reg-entry.status/challenge-period
  [{:keys [:challenge/created-on :reg-entry/status :reg-entry/challenge-period-end] :as meme}]

  (let [{:keys [days hours minutes seconds] :as tr} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                         (ui-utils/gql-date->date challenge-period-end))]
    (when-let [params @(subscribe [:memefactory.ui.config/memefactory-db-params])]
      [:div
       [:h1.title "Challenge"]
       [:div.challenge-component-inner-cp
        [challenge-header created-on]
        [status-component meme "This meme has been submitted to the the registry, but has not passed the challenge window for automatic acceptance. It can be challenged at any time before this window ends."]
        (if-not (= 0 days hours minutes seconds)
          [challenge-meme-component meme (:deposit params)]
          [:div "Challenge period ended."])]])))

(defmethod challenge-component [:reg-entry.status/whitelisted :reg-entry.status/blacklisted]
  [{:keys [:challenge/created-on :reg-entry/status] :as meme}]
  [:div
   [:h1.title "Challenge"]
   (if-not created-on
     [:div
      [:h4.title "This meme hasn't been challenged."]]
     (cond-> [:div.challenge-component-inner
              [challenge-header created-on]]
       created-on (into [[status-component meme "This meme was challenged, and voting has concluded. Accepted memes are minted to the marketplace, while rejected ones can be resubmitted by the creator."]
                         [challenger-component meme]
                         [vote-page/collect-reward-action meme]])))])

(defn details []

  (let [active-account @(subscribe [::accounts-subs/active-account])
        address (-> @(re-frame/subscribe [::router-subs/active-page]) :params :address)
        meme-sub (subscribe [::gql/query (build-meme-query address active-account)])
        meme (when meme-sub (-> @meme-sub :meme))
        {:keys [:reg-entry/status :meme/image-hash :meme/title :meme/number :reg-entry/status :meme/total-supply
                :meme/tags :meme/owned-meme-tokens :reg-entry/creator :challenge/challenger]} meme
        token-count (->> owned-meme-tokens
                         (map :meme-token/token-id)
                         (filter shared-utils/not-nil?)
                         count)
        tags (mapv :tag/name tags)
        ;; A bit of a hack checking the status here, should just rely upon
        ;; :graphql/loading?. Unfortunately this subscription sometimes returns
        ;; nil when it's really still just loading.
        meme-not-loaded? (or (not status)
                             (and meme-sub (:graphql/loading? @meme-sub)))]

    (log/debug "Query sub:" @(subscribe [::gql/query (build-meme-query address active-account)]))
    [app-layout/app-layout
     {:meta {:title "MemeFactory"
             :description "Description"}}
     [:div.meme-detail-page
      [:section.meme-detail
       [:div.meme-info {:class (when meme-not-loaded? "loading")}
        (if meme-not-loaded?
          [spinner/spin]
          (list
           (when number
             [:div.meme-number {:key :meme-number} (str "#" number)])

           [:div.container {:key :container}
            [tiles/meme-image image-hash]]

           [:div.registry {:key :registry}
            [:h1 title]
            [:div.status (case (graphql-utils/gql-name->kw status)
                           :reg-entry.status/whitelisted [:label.in-registry "In Registry"]
                           :reg-entry.status/blacklisted [:label.rejected "Rejected"]
                           :reg-entry.status/challenge-period [:label.rejected "In Challenge Period"]
                           [:label.challenged "Challenged"])]
            [:div.description description]
            [:div.text (format/pluralize total-supply "card")]
            [:div.text (str "You own " token-count)]
            [meme-creator-component creator]
            [:div.tags
             (for [tag-name tags]
               [:button {:key tag-name
                         :on-click #(dispatch [::router-events/navigate
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
                                                              {:term title}])} "Search On Memefolio"]]]))]]
      [:section.history
       [:div.history-component
        (if meme-not-loaded?
          [spinner/spin]
          [history-component address])]]
      [:section.challenge
       [:div.challenge-component
        (if meme-not-loaded?
          [spinner/spin]
          [challenge-component meme])]]
      [:section.related
       [:div.relateds-panel
        [:div.icon]
        [:h2.title "RELATED MEMES ON MARKETPLACE"]
        [:h3.title "Similar memes for sale now"]
        [related-memes-container address tags]]]]]))

(defmethod page :route.meme-detail/index []
  (r/create-class {:component-did-mount #(js/window.scrollTo 0 0)
                   :reagent-render (fn [] [details])}))
