(ns memefactory.ui.meme-detail.page
  (:require [bignumber.core :as bn]
            [cljs-time.core :as t]
            [cljs-time.format :as time-format]
            [cljs-web3.core :as web3]
            [cljs.core.match :refer-macros [match]]
            [district.cljs-utils :as cljs-utils]
            [district.format :as format]
            [district.graphql-utils :as gql-utils]
            [district.parsers :as parsers]
            [district.time :as time]
            [district.ui.component.form.input :as inputs]
            [district.ui.component.page :refer [page]]
            [district.ui.component.tx-button :as tx-button]
            [district.ui.graphql.subs :as gql]
            [district.ui.ipfs.subs :as ipfs-subs]
            [district.ui.now.subs :as now-subs]
            [district.ui.router.subs :as router-subs]
            [district.ui.web3-account-balances.subs :as account-balances-subs]
            [district.ui.web3-accounts.subs :as accounts-subs]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [goog.string :as gstring]
            [memefactory.ui.components.app-layout :refer [app-layout]]
            [memefactory.ui.components.general :refer [dank-with-logo nav-anchor]]
            [memefactory.ui.components.panels :refer [panel]]
            [memefactory.ui.components.search :as search]
            [memefactory.ui.components.spinner :as spinner]
            [memefactory.ui.components.tiles :as tiles]
            [memefactory.ui.contract.registry-entry :as registry-entry]
            [memefactory.ui.dank-registry.vote-page :as vote-page]
            [memefactory.ui.events :as memefactory-events]
            [memefactory.ui.spec :as spec]
            [memefactory.ui.utils :as ui-utils :refer [format-price format-dank]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [reagent.ratom :as ratom]
            [taoensso.timbre :as log :refer [spy]]))

(def time-formatter (time-format/formatter "EEEE, ddo MMMM, yyyy 'at' HH:mm Z"))

(defn build-meme-query [address active-account]
  {:queries [[:meme {:reg-entry/address address}
              (cond-> [:reg-entry/address
                       :reg-entry/status
                       :reg-entry/deposit
                       :reg-entry/created-on
                       :reg-entry/challenge-period-end
                       :meme/image-hash
                       :meme/meta-hash
                       :meme/number
                       :meme/title
                       :meme/comment
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
        creator-total-earned (web3/from-wei (reduce (fn [total-earned {:keys [:meme-auction/end-price]}]
                                                      (+ total-earned end-price))
                                                    0
                                                    (-> @query :search-meme-auctions :items))
                                            :ether)]
    [:div.creator
     [:b "Creator"]
     [:div.rank (str "Rank: #" creator-rank " (" (ui-utils/format-dank creator-total-earned) ")")]
     [:div.success (str "Success rate: " total-created-memes-whitelisted "/" total-created-memes " ("
                        (format/format-percentage total-created-memes-whitelisted total-created-memes) ")")]
     [:div {}
      [:span "Address: "]
      [nav-anchor {:route :route.memefolio/index
                   :params {:address address}
                   :query {:tab :created}
                   :class (str "address " (when (= address @(subscribe [::accounts-subs/active-account]))
                                            "active-address"))}
       address]]]))


(defn related-memes-container [address tags]
  (let [form-data (r/atom {:option-filters :only-lowest-number})
        build-query (fn [{:keys [:options] :as args}]
                      [[:search-meme-auctions (cond-> {:tags-or tags
                                                       :first 18
                                                       :non-for-meme address
                                                       :statuses [:meme-auction.status/active]}
                                                (#{:only-lowest-number :only-cheapest} options)
                                                (assoc :group-by (get {:only-lowest-number :meme-auctions.group-by/lowest-card-number
                                                                       :only-cheapest :meme-auctions.group-by/cheapest}
                                                                      options)))
                        [[:items [:meme-auction/address
                                  :meme-auction/status
                                  :meme-auction/start-price
                                  :meme-auction/end-price
                                  :meme-auction/bought-for
                                  :meme-auction/duration
                                  :meme-auction/started-on
                                  :meme-auction/description
                                  [:meme-auction/seller [:user/address]]
                                  [:meme-auction/meme-token
                                   [:meme-token/number
                                    [:meme-token/meme
                                     [:reg-entry/address
                                      :meme/title
                                      :meme/number
                                      :meme/image-hash
                                      :meme/meta-hash
                                      :meme/total-minted]]]]]]]]])]
    (fn [address tags]
      (let [query-id [address @form-data]
            response (subscribe [::gql/query {:queries (build-query {:options (:option-filters @form-data)})}])

            state (-> @response :search-meme-auctions :items)
            loading? (:graphql/loading? @response)]
        [:div.selling-panel
         [inputs/radio-group {:id :option-filters
                              :form-data form-data
                              :options search/auctions-option-filters}]
         (if (and (empty? state)
                  (not loading?))
           [:div.no-items-found "No items found"]
           [:div.scroll-area
            [:div.tiles
             [:div
              (if loading?
                [spinner/spin]
                (doall
                 (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
                        ^{:key address}
                        [tiles/auction-tile {:show-cards-left? (contains? #{:only-cheapest :only-lowest-number} (:option-filters @form-data))}
                         meme-auction])
                      state)))]]])]))))


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
                                                          [:meme-token/token-id
                                                           :meme-token/number]]]]]]]}])
            all-auctions (-> @query :meme :meme/meme-auctions)]
        [:div
         [:h1.title "Marketplace history"]
         (if (:graphql/loading? @query)
           [spinner/spin]
           (if (empty? all-auctions)
             [:div.not-traded-yet "This meme hasn't been traded yet"]
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
              [:tbody
               (doall
                (for [{:keys [:meme-auction/address :meme-auction/bought-for :meme-auction/bought-on
                              :meme-auction/meme-token :meme-auction/seller :meme-auction/buyer] :as auction} all-auctions]
                  (when address
                    ^{:key address}
                    [:tr
                     [:td.meme-token (:meme-token/number meme-token)]
                     [:td.seller-address
                      [nav-anchor {:route :route.memefolio/index
                                   :params {:address (:user/address seller)}
                                   :query {:tab :sold}
                                   :class (str "address seller-address " (when (= (:user/address seller) @(subscribe [::accounts-subs/active-account]))
                                                                           "active-address"))}
                       (:user/address seller)]]
                     [:td.buyer-address
                      [nav-anchor {:route :route.memefolio/index
                                   :params {:address (:user/address buyer)}
                                   :query {:tab :collected}
                                   :class (str "address buyer-address " (when (= (:user/address buyer) @(subscribe [::accounts-subs/active-account]))
                                                                          "active-address"))}
                       (:user/address buyer)]]
                     [:td.end-price (format-price bought-for)]
                     [:td.time  (let [now-date (t/date-time @now)
                                      bought-date (gql-utils/gql-date->date bought-on)]
                                  (when-not (or (empty? (str bought-on))
                                                (> (.getTime bought-date) (.getTime now-date)))
                                    (format/time-ago bought-date now-date)))]])))]]))]))))


(defn challenge-header [created-on]
  (when created-on
    [:div.header
     [:h2.title
      (str "This meme was challenged on " (time-format/unparse time-formatter (t/local-date-time (gql-utils/gql-date->date created-on))))]]))


(defn challenger-component [{:keys [:challenge/comment :challenge/challenger] :as meme}]
  (let [{:keys [:user/challenger-rank :user/challenger-total-earned
                :user/total-created-challenges :user/total-created-challenges-success]} challenger]
    [:div.challenger
     [:b "Challenger"]
     [:div.rank (str "Rank: #" challenger-rank " ("
                     (format-dank challenger-total-earned) ")")]
     [:div.success (str "Success rate: " total-created-challenges-success "/" total-created-challenges " ("
                        (format/format-percentage total-created-challenges-success total-created-challenges) ")")]
     [:div.address
      [:span "Address: "]
      [nav-anchor {:route :route.memefolio/index
                   :params {:address (:user/address challenger)}
                   :query {:tab :curated}
                   :class (str "address " (when (= (:user/address challenger) @(subscribe [::accounts-subs/active-account]))
                                            "active-address"))}
       (:user/address challenger)]]
     [:span.challenge-comment (str "\"" comment "\"")]]))


(defn status-component [{:keys [:reg-entry/status :reg-entry/challenge-period-end :challenge/commit-period-end :challenge/reveal-period-end] :as meme} text]
  (let [status (gql-utils/gql-name->kw status)
        [period-label period-end] (case status
                                    :reg-entry.status/challenge-period ["Challenge" challenge-period-end]
                                    :reg-entry.status/commit-period    ["Voting" commit-period-end]
                                    :reg-entry.status/reveal-period    ["Reveal" reveal-period-end]
                                    nil)
        {:keys [days hours minutes seconds] :as time-remaining} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                                     (gql-utils/gql-date->date period-end))]
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


(defn challenge-meme-component [{:keys [:reg-entry/deposit :meme/title] :as meme} dank-deposit]
  (let [form-data (r/atom {:challenge/comment nil})
        active-account (subscribe [::accounts-subs/active-account])
        account-balance (subscribe [::account-balances-subs/active-account-balance :DANK])
        errors (ratom/reaction {:local (when-not (spec/check ::spec/challenge-comment (:challenge/comment @form-data))
                                         {:challenge/comment "Comment shouldn't be empty."})})
        tx-id (str (:reg-entry/address meme) "challenges")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-create-challenge tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-create-challenge tx-id}])]
    (fn []
      [:div
       [:b "Challenge Explanation"]
       [inputs/textarea-input {:form-data form-data
                               :disabled (not @active-account)
                               :id :challenge/comment
                               :maxLength 2000
                               :errors errors}]
       [:div.controls
        [dank-with-logo (web3/from-wei dank-deposit :ether)]
        [tx-button/tx-button {:primary true
                              :disabled (or @tx-success? (not (empty? (:local @errors))) (not @active-account))
                              :pending? @tx-pending?
                              :pending-text "Challenging..."
                              :on-click #(dispatch [::memefactory-events/add-challenge {:send-tx/id tx-id
                                                                                        :reg-entry/address (:reg-entry/address meme)
                                                                                        :comment (:challenge/comment @form-data)
                                                                                        :deposit dank-deposit
                                                                                        :tx-description title
                                                                                        :type :meme}])}
         (if @tx-success?
           "Challenged"
           "Challenge")]]
       (when (or (not @active-account) (bn/< @account-balance dank-deposit))
         [:div.not-enough-dank "You don't have enough DANK tokens to challenge this meme"])])))


(defn remaining-time-component [to-time]
  (let [time-remaining (subscribe [::now-subs/time-remaining to-time])
        {:keys [:days :hours :minutes :seconds]} @time-remaining]
    [:div.reveal-time-remaining (str (format/pluralize days "day") " "
                                     (format/pluralize hours "hour") " "
                                     minutes " min. "
                                     seconds " sec.")]))


(defn reveal-vote-component [{:keys [:challenge/reveal-period-end :challenge/vote :reg-entry/address :meme/title] :as meme}]
  (let [tx-id (str "reveal" address)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/reveal-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/reveal-vote tx-id}])
        active-account @(subscribe [::accounts-subs/active-account])
        vote (get @(subscribe [:memefactory.ui.subs/votes active-account]) address)
        vote-option (-> meme :challenge/vote :vote/option gql-utils/gql-name->kw)]
    (fn []
      [:div.reveal
       [:div "This meme has been challenged and voting has concluded. You can reveal any votes you made with the button below, before time runs out on the reveal period."]
       [remaining-time-component (gql-utils/gql-date->date reveal-period-end)]
       [tx-button/tx-button {:primary true
                             :disabled (or @tx-success? (not vote))
                             :pending? @tx-pending?
                             :pending-text "Revealing..."
                             :on-click #(dispatch [::registry-entry/reveal-vote
                                                   {:send-tx/id tx-id
                                                    :reg-entry/address (:reg-entry/address meme)
                                                    :tx-description title
                                                    :option-desc {:vote.option/vote-against "stank"
                                                                  :vote.option/vote-for     "dank"}}
                                                   vote])}
        (if @tx-success?
          "Revealed"
          "Reveal My Vote")]
       (when (and (= vote-option :vote-option/not-revealed)
                  (not vote))
         [:div.no-reveal-info "Secret to reveal vote was not found in your browser"])])))


(defn vote-component [{:keys [:challenge/commit-period-end :meme/title] :as meme}]
  (let [balance-dank (subscribe [::account-balances-subs/active-account-balance :DANK])
        active-account (subscribe [::accounts-subs/active-account])
        form-data (r/atom {:vote/amount-for nil
                           :vote/amount-against nil})

        errors (ratom/reaction {:local (let [amount-for (-> @form-data :vote/amount-for parsers/parse-float)
                                             amount-against (-> @form-data :vote/amount-against parsers/parse-float)
                                             balance (if (pos? @balance-dank) (web3/from-wei @balance-dank :ether) 0)]
                                         (cond-> {}
                                           (or (not (pos? amount-for))
                                               (< balance amount-for))
                                           (assoc :vote/amount-for (str "Amount should be between 0 and " balance))

                                           (or (not (pos? amount-against))
                                               (< balance amount-against))
                                           (assoc :vote/amount-against (str "Amount should be between 0 and " balance))))})

        tx-id (str (:reg-entry/address meme) "vote")
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::registry-entry/approve-and-commit-vote tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::registry-entry/approve-and-commit-vote tx-id}])]
    (fn []
      (let [disabled? (or (not @active-account) (bn/<= @balance-dank 0))]
        [:div.vote
         [:div "Below you can choose to vote \"Dank\" to include the Meme in the registry or \"Stank\" to reject it. You can enter the amount of DANK tokens to commit to this vote."]
         [:div.form
          [:div.vote-dank
           [:div.outer
            [inputs/with-label "Amount "
             [inputs/text-input {:form-data form-data
                                 :disabled disabled?
                                 :id :vote/amount-for
                                 :dom-id :vote/amount-for
                                 :errors errors
                                 :type :number
                                 :min 0}]
             {:form-data form-data
              :for :vote/amount-for
              :id :vote/amount-for}]
            [:span.unit "DANK"]]

           [inputs/pending-button
            {:pending? @tx-pending?
             :disabled (or (-> @errors :local :vote/amount-for empty? not)
                           @tx-pending?
                           @tx-success?)
             :pending-text "Voting..."
             :on-click #(dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                             :reg-entry/address (:reg-entry/address meme)
                                                                             :vote/option :vote.option/vote-for
                                                                             :vote/amount (-> @form-data
                                                                                              :vote/amount-for
                                                                                              parsers/parse-float
                                                                                              (web3/to-wei :ether))
                                                                             :type :meme
                                                                             :option-desc {:vote.option/vote-against "stank"
                                                                                           :vote.option/vote-for     "dank"}
                                                                             :tx-description title}])}
            (if @tx-success?
              "Voted"
              "Vote Dank")]]

          [:div.vote-stank
           [:div.outer
            [inputs/with-label "Amount "
             [inputs/text-input {:form-data form-data
                                 :disabled disabled?
                                 :id :vote/amount-against
                                 :dom-id :vote/amount-against
                                 :errors errors
                                 :tpe :number
                                 :min 0}]
             {:form-data form-data
              :for :vote/amount-against
              :id :vote/amount-against}]
            [:span.unit "DANK"]]
           [inputs/pending-button
            {:pending? @tx-pending?
             :disabled (or (-> @errors :local :vote/amount-against empty? not)
                           @tx-pending?
                           @tx-success?)
             :pending-text "Voting..."
             :on-click #(dispatch [::registry-entry/approve-and-commit-vote {:send-tx/id tx-id
                                                                             :reg-entry/address (:reg-entry/address meme)
                                                                             :vote/option :vote.option/vote-against
                                                                             :vote/amount (-> @form-data
                                                                                            :vote/amount-against
                                                                                            parsers/parse-float
                                                                                            (web3/to-wei :ether))
                                                                             :tx-description title
                                                                             :option-desc {:vote.option/vote-against "stank"
                                                                                           :vote.option/vote-for     "dank"}
                                                                             :type :meme}])}
            (if @tx-success?
              "Voted"
              "Vote Stank")]]]
         (if (bn/> @balance-dank 0)
           [:<>
            [:div "You can vote with up to " (ui-utils/format-dank @balance-dank)]
            [:div "Token will be returned to you after revealing your vote."]]
           [:div.not-enough-dank "You don't have any DANK tokens to vote on this meme challenge"])]))))


(defmulti challenge-component (fn [meme] (match [(-> meme :reg-entry/status gql-utils/gql-name->kw)]
                                                [(:or :reg-entry.status/whitelisted :reg-entry.status/blacklisted)] [:reg-entry.status/whitelisted :reg-entry.status/blacklisted]
                                                [:reg-entry.status/challenge-period] :reg-entry.status/challenge-period
                                                [:reg-entry.status/commit-period] :reg-entry.status/commit-period
                                                [:reg-entry.status/reveal-period] :reg-entry.status/reveal-period)))


(defmethod challenge-component :reg-entry.status/reveal-period
  [{:keys [:challenge/created-on :reg-entry/status :challenge/reveal-period-end] :as meme}]
  (let [{:keys [days hours minutes seconds]} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                  (gql-utils/gql-date->date reveal-period-end))]
    [:div
     [:h1.title "Challenge"]
     (cond-> [:div.challenge-component-inner
              [challenge-header created-on]]
       created-on (into [[status-component meme "This meme has had its place in the registry challenged, and voting has concluded. Voters must now reveal their votes to determine if the meme is accepted or rejected."]
                         [challenger-component meme]
                         (if-not (= 0 days hours minutes seconds)
                           [reveal-vote-component meme]
                           [:div "Reveal period ended."])]))]))


(defmethod challenge-component :reg-entry.status/commit-period
  [{:keys [:challenge/created-on :reg-entry/status :challenge/commit-period-end] :as meme}]

  (let [{:keys [days hours minutes seconds]} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                  (gql-utils/gql-date->date commit-period-end))]
    [:div
     [:h1.title "Challenge"]
     (cond-> [:div.challenge-component-inner
              [challenge-header created-on]]
       created-on (into [[status-component meme "This meme has had its place in the registry challenged. It is currently open for voting. When the voting period ends, it will enter the vote reveal phase."]
                         [challenger-component meme]
                         (if-not (= 0 days hours minutes seconds)
                           [vote-component meme]
                           [:div "Commit period ended."])]))]))


(defmethod challenge-component :reg-entry.status/challenge-period
  [{:keys [:challenge/created-on :reg-entry/status :reg-entry/challenge-period-end] :as meme}]

  (let [{:keys [days hours minutes seconds] :as tr} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                         (gql-utils/gql-date->date challenge-period-end))]
    (when-let [params @(subscribe [:memefactory.ui.config/memefactory-db-params])]
      [:div
       [:h1.title "Challenge"]
       [:div.challenge-component-inner-cp
        [challenge-header created-on]
        [status-component meme "This meme has been submitted to the the registry, but has not passed the challenge window for automatic acceptance. It can be challenged at any time before this window ends."]
        (if-not (= 0 days hours minutes seconds)
          [challenge-meme-component meme (-> params :deposit :value)]
          [:div "Challenge period ended."])]])))


(defmethod challenge-component [:reg-entry.status/whitelisted :reg-entry.status/blacklisted]
  [{:keys [:challenge/created-on :reg-entry/status] :as meme}]
  [:div
   [:h1.title "Challenge"]
   (if-not created-on
     [:div.no-challenge "This meme hasn't been challenged"]
     (cond-> [:div.challenge-component-inner
              [challenge-header created-on]]
       created-on (into [[status-component meme "This meme was challenged, and voting has concluded. Accepted memes are minted to the marketplace, while rejected ones can be resubmitted by the creator."]
                         [challenger-component meme]
                         [vote-page/collect-reward-action meme]])))])

(defn details []
  (let [active-account @(subscribe [::accounts-subs/active-account])
        address (-> @(re-frame/subscribe [::router-subs/active-page]) :params :address)
        meme-sub (subscribe [::gql/query (build-meme-query address active-account)
                             {:refetch-on [:memefactory.ui.contract.registry-entry/challenge-success]}])
        meme (when meme-sub (-> @meme-sub :meme))
        {:keys [:reg-entry/status :meme/image-hash :meme/title :meme/number :reg-entry/status :meme/total-supply :reg-entry/created-on
                :meme/tags :meme/owned-meme-tokens :reg-entry/creator :challenge/challenger :reg-entry/challenge-period-end :challenge/reveal-period-end]} meme
        token-count (->> owned-meme-tokens
                         (map :meme-token/token-id)
                         (filter cljs-utils/not-nil?)
                         count)
        tags (mapv :tag/name tags)
        ;; A bit of a hack checking the status here, should just rely upon
        ;; :graphql/loading?. Unfortunately this subscription sometimes returns
        ;; nil when it's really still just loading.
        not-loaded? (or (not status)
                        (and meme-sub (:graphql/loading? @meme-sub)))
        exists? (and (not not-loaded?) (:reg-entry/address meme))
        url (:gateway @(subscribe [::ipfs-subs/ipfs]))]
    [app-layout
     {:meta {:title (str "MemeFactory - " title)
             :description (str "Details of meme " title ". " "MemeFactory is a decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets.")}
      :tags [{:id "image"
              :name "og:image"
              :content (str (format/ensure-trailing-slash url) image-hash)}]}
     [:div.meme-detail-page
      [:section.meme-detail
       (if-not exists?
         [:div.not-in-registry "This meme is not in the registry"]
         [:div.meme-info {:class (when not-loaded? "loading")}
          (if not-loaded?
            [spinner/spin]
            (list
             (when number
               [:div.meme-number {:key :meme-number} (str "#" number)])
             ^{:key :container}
             [tiles/meme-image image-hash {:rejected? (-> (gql-utils/gql-name->kw status) (= :reg-entry.status/blacklisted))}]
             (if exists?
               [:div.registry {:key :registry}
                [:h1 title]
                [:div.status (case (gql-utils/gql-name->kw status)
                               :reg-entry.status/whitelisted [:label.in-registry "In Registry"]
                               :reg-entry.status/blacklisted [:label.rejected "Rejected from Registry"]
                               :reg-entry.status/challenge-period [:label.rejected "Open for Challenge"]
                               [:label.challenged "Challenged"])]
                [:div.description (case (gql-utils/gql-name->kw status)
                                    :reg-entry.status/whitelisted      "This meme has passed through the challenge phase and has been placed into the Dank Registry."
                                    :reg-entry.status/blacklisted      "This meme was challenged and lost. It's ineligible for the Dank Registry unless resubmitted."
                                    :reg-entry.status/challenge-period (gstring/format "This meme has been submitted to the Dank Registry, but is still open to be challenged. The challenge window closes in %s"
                                                                                       (format/format-time-units
                                                                                        (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                                                             (gql-utils/gql-date->date challenge-period-end))))
                                    (gstring/format "This meme has had its status in the registry challenged. It will be either accepted or rejected when the vote and reveal phases conclude in %s"
                                                    (format/format-time-units
                                                     (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                          (gql-utils/gql-date->date reveal-period-end)))))]
                [:div.text (str "Created " (let [days (:days (time/time-remaining (gql-utils/gql-date->date created-on)
                                                                                  @(subscribe [:district.ui.now.subs/now])))]
                                             (if (pos? days)
                                               (str (format/pluralize days "day") " ago")
                                               "today")))]
                [:div.text (gstring/format "You own %s out of %d"
                                           (format/pluralize token-count "card")
                                           total-supply)]
                [meme-creator-component creator]
                (when (seq (:meme/comment meme))
                  [:p.meme-comment (str "\"" (:meme/comment meme) "\"")])
                [:div.tags
                 (for [tag-name tags]
                   [nav-anchor {:route :route.marketplace/index
                                :params nil
                                :query {:search-tags [tag-name]}
                                :class "tag"
                                :key tag-name}
                    tag-name])]
                [:div.buttons
                 [nav-anchor {:route :route.marketplace/index
                              :params nil
                              :query {:term title :option-filter "all-cards"}
                              :class "search marketplace"}
                  "Search On Marketplace"]
                 (when active-account
                   [nav-anchor {:route :route.memefolio/index
                                :params nil
                                :query {:term title}
                                :class "search memefolio"}
                    "Search On Memefolio"])]]
               )))])]
      [:section.history
       [:div.history-component
        (if not-loaded?
          [spinner/spin]
          [history-component address])]]
      [:section.challenge
       [:div.challenge-component
        (if not-loaded?
          [spinner/spin]
          [challenge-component meme])]]
      [:section.related
       [:div.relateds-panel
        [:div.icon]
        [:h2.title "RELATED MEMES ON MARKETPLACE"]
        [:h3.title "Similar memes for sale now"]
        (if not-loaded?
          [spinner/spin]
          [related-memes-container address tags])]]]]))


(defmethod page :route.meme-detail/index []
  (fn []
    [details]))
