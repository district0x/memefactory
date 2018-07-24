(ns memefactory.ui.meme-detail.page
  (:require
   ;;   [district.time :as time]
   ;;  [cljs.core.match :refer-macros [match]]
   [cljs-time.core :as t]
   [cljs-time.format :as time-format]
   [cljs-web3.core :as web3]
   [cljsjs.d3]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.ui.component.page :refer [page]]
   [district.ui.component.tx-button :as tx-button]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.events :as router-events]
   [district.ui.router.subs :as router-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :as app-layout]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.meme-detail.events :as meme-detail-events]
   [memefactory.ui.utils :as ui-utils]
   [print.foo :refer [look] :include-macros true]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   ))

(def description "Lorem ipsum dolor sit amet, consectetur adipiscing elit")

(def time-formatter (time-format/formatter "EEEE, ddo MMMM, yyyy 'at' HH:mm:ss Z"))

(defn build-meme-query [address active-account]
  {:id address
   :queries [[:meme {:reg-entry/address address}
              [:reg-entry/address
               :reg-entry/status
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
                 :vote/claimed-reward-on]]
               ]]]})

(defn meme-creator-component [{:keys [:user/address :user/creator-rank :user/total-created-memes
                                      :user/total-created-memes-whitelisted] :as creator}]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller address :statuses [:meme-auction.status/done]}
                                      [[:items [;;:meme-auction/start-price
                                                :meme-auction/end-price
                                                ;;:meme-auction/bought-for
                                                ]]]]]}])
        creator-total-earned (reduce (fn [total-earned {:keys [:meme-auction/end-price] :as meme-auction}]
                                       (+ total-earned end-price))
                                     0
                                     (-> @query :search-meme-auctions :items))]

    (when-not (:graphql/loading? @query)
      [:div.creator
       [:b "Creator"]
       [:div.rank (str "Rank: #" creator-rank " (" (format/format-eth (web3/from-wei creator-total-earned :ether)) ")")]
       [:div.success (str "Success rate: " total-created-memes-whitelisted "/" total-created-memes " ("
                          (format/format-percentage total-created-memes-whitelisted total-created-memes) ")")]
       [:div.address (str "Address: " address)]])))

(defn history-component [address]
  (let [order-by (r/atom :meme-auctions.order-by/token-id)
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
        (when-not (:graphql/loading? @query)

          #_(prn @query)

          [:h1 "Marketplace history"]
          [:table {:style {:table-layout "fixed"
                           :border-collapse "collapse"}}
           [:thead [:tr {:style {:display "block"}}
                    [:th {:class (if (:meme-auctions.order-by/token-id @order-by) :up :down)
                          :on-click #(flip-ordering :meme-auctions.order-by/token-id)} "Card Number"]
                    [:th {:class (if (:meme-auctions.order-by/seller @order-by) :up :down)
                          :on-click #(flip-ordering :meme-auctions.order-by/seller)} "Seller"]
                    [:th {:class (if (:meme-auctions.order-by/buyer @order-by) :up :down)
                          :on-click #(flip-ordering :meme-auctions.order-by/buyer)} "Buyer"]
                    [:th {:class (if (:meme-auctions.order-by/proce @order-by) :up :down)
                          :on-click #(flip-ordering :meme-auctions.order-by/price)} "Price"]
                    [:th {:class (if (:meme-auctions.order-by/bought-on @order-by) :up :down)
                          :on-click #(flip-ordering :meme-auctions.order-by/bought-on)} "Time Ago"]]]
           [:tbody {:style {:display "block"
                            :width "100%"
                            :overflow "auto"
                            :height "100px"}}
            (doall
             (for [{:keys [:meme-auction/address :meme-auction/end-price :meme-auction/bought-on
                           :meme-auction/meme-token :meme-auction/seller :meme-auction/buyer] :as auction} (-> @query :meme :meme/meme-auctions)]
               (when address
                 ^{:key address}
                 [:tr
                  [:td (:meme-token/token-id meme-token)]
                  [:td (:user/address seller)]
                  [:td (:user/address buyer)]
                  [:td end-price]
                  [:td (format/time-ago (ui-utils/gql-date->date bought-on) (t/now))]])))]])))))

(defn donut-chart [{:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total]}]
  (r/create-class
   {:reagent-render (fn [] [:div#donutchart])
    :component-did-mount (fn []
                           (let [width 170
                                 height 170
                                 data [{:challenge/votes :for :value votes-for}
                                       {:challenge/votes :against :value votes-against}]
                                 outer-radius (/ (min width height) 2)
                                 inner-radius (/ outer-radius 2)
                                 pie (-> js/d3
                                         .pie
                                         (.value (fn [d] (aget d "value"))))
                                 color-scale (-> js/d3
                                                 .scaleOrdinal
                                                 (.range (clj->js ["#04ffcc" "#ffeb01"])))]
                             (-> js/d3
                                 (.select "#donutchart")
                                 (.append "svg")
                                 (.attr "width" width)
                                 (.attr "height" height)
                                 (.append "g")
                                 (.attr "transform" (str "translate(" (/ width 2) "," (/ height 2) ")"))
                                 (.selectAll ".arc")
                                 (.data (pie
                                         (clj->js data)))
                                 (.enter)
                                 (.append "g")
                                 (.attr "class" "arc")
                                 (.append "path")
                                 (.attr "d" (-> js/d3
                                                .arc
                                                (.outerRadius inner-radius)
                                                (.innerRadius outer-radius)))
                                 (.style "fill" (fn [d]
                                                  (color-scale
                                                   (aget d "data" "votes")))))))}))

(defn challenge-header [created-on]
  [:div
   [:div.title [:h1 "Challenge"]]
   [:span (str "This meme was challenged on " (time-format/unparse time-formatter (t/local-date-time (ui-utils/gql-date->date created-on))))]])

(defn challenger-component [{:keys [:challenge/comment :challenge/challenger] :as meme}]
  (let [{:keys [:user/challenger-rank :user/challenger-total-earned
                :user/total-created-challenges :user/total-created-challenges-success]} challenger]
    [:div
     [:b "Challenger"]
     [:div.rank (str "Rank: #" challenger-rank " (" (format/format-dnt challenger-total-earned) ")")]
     [:div.success (str "Success rate: " total-created-challenges-success "/" total-created-challenges " ("
                        (format/format-percentage total-created-challenges-success total-created-challenges) ")")]
     [:div.address (str "Address: " (:user/address challenger))]
     [:i comment]]))

(defn status-component [status]
  [:div [:b "Challenge status"]
   [:div.description (case (graphql-utils/gql-name->kw status)
                       :reg-entry.status/whitelisted "Resolved, accepted"
                       :reg-entry.status/blacklisted "Resolved, rejected"
                       status)]])

(defn votes-component [{:keys [:challenge/votes-for :challenge/votes-against :challenge/votes-total
                               :challenge/challenger :reg-entry/creator :challenge/vote] :as meme}]
  (let [{:keys [:vote/option :vote/reward :vote/claimed-reward-on]} vote
        active-account (subscribe [::accounts-subs/active-account])
        option (graphql-utils/gql-name->kw option)
        tx-id (str (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-token/transfer-multi-and-start-auction tx-id}])
        tx-button-disabled? (or (= 0 reward)
                                (shared-utils/not-nil? claimed-reward-on))]
    [:div
     [:div {:style {:float "left"}}
      [donut-chart meme]]

     [:div {:style {:float "right"}}
      [:div.dank (str "Voted Dank: " (format/format-percentage votes-for votes-total) " - " votes-for)]
      [:div.stank (str "Voted Stank: " (format/format-percentage votes-against votes-total) " - " votes-against)]
      [:div.total (str "Total voted: " votes-total)]
      (when (contains? #{:vote-option/vote-for :vote-option/vote-against} option)
        [:div
         [:div.vote (str "You voted: " (case option
                                         :vote-option/vote-for "DANK"
                                         :vote-option/vote-against "STANK"))]
         [:div.reward (str "Your reward: " (format/format-dnt reward))]
         (when-not (= 0 (look reward))
           [tx-button/tx-button {:primary true
                                 :disabled tx-button-disabled?
                                 :pending? @tx-pending?
                                 :pending-text "Collecting reward..."
                                 :on-click (fn [] (dispatch [::meme-detail-events/claim-vote-reward {:tx-id tx-id
                                                                                                     :meme meme

                                                                                                     :meme-query (build-meme-query (:reg-entry/address meme) @active-account)
                                                                                                     }]))}
            "Collect Reward"])])]]))

(defmulti challenge-component (fn [meme] (-> meme :reg-entry/status graphql-utils/gql-name->kw)))

(defmethod challenge-component [:reg-entry.status/whitelisted :reg-entry.status/blacklisted]
  [{:keys [:challenge/created-on :reg-entry/status] :as meme}]
  (let [areas "'header header header header header header'
               'status status challenger challenger votes votes'"]
    [:div.whitelisted {:style {:display "grid"
                               :grid-template-areas areas}}
     
     [:div.header {:style {:grid-area "header"}}
      [challenge-header created-on]]

     [:div.status {:style {:grid-area "status"}}
      [status-component status]]

     [:div.challenger {:style {:grid-area "challenger"}}
      [challenger-component meme]]

     [:div.votes {:style {:grid-area "votes"}}
      [votes-component meme]]

     ]))

;;  TODO:  related
(defmethod page :route.meme-detail/index []
  (let [address (-> @(re-frame/subscribe [::router-subs/active-page]) :query :address)
        active-account (subscribe [::accounts-subs/active-account])        
        response (subscribe [::gql/query (build-meme-query address @active-account)])]

    (when-not (:graphql/loading? @response)
      
      (cljs.pprint/pprint @response)

      (if-let [meme (:meme @response)]
        (let [{:keys [:reg-entry/status :meme/image-hash :meme/title :reg-entry/status :meme/total-supply
                      :meme/tags :meme/owned-meme-tokens :reg-entry/creator :challenge/challenger]} meme
              token-count (->> owned-meme-tokens
                               (map :meme-token/token-id)
                               (filter shared-utils/not-nil?)
                               count)
              tags (mapv :tag/name tags)]

          [app-layout/app-layout
           {:meta {:title "MemeFactory"
                   :description "Description"}}

           [:div.meme-detail {:style {:display "grid"
                                      :grid-template-areas
                                      "'image image image rank rank rank'
                                             'history history history history history history'
                                             'challenge challenge challenge challenge challenge challenge'"}}

            ;; meme
            [:div {:style {:grid-area "image"}}
             [tiles/meme-image image-hash]]

            [:div {:style {:grid-area "rank"}}
             [:h1 title]
             [:div.status (case (graphql-utils/gql-name->kw status)
                            :reg-entry.status/whitelisted [:label "In Registry"]
                            :reg-entry.status/blacklisted [:label "Rejected"]
                            [:label "Challenged"])]
             [:div.description description]
             [:div.text (format/pluralize total-supply "card") ]
             [:div.text (str "You own " token-count) ]

             [meme-creator-component creator]

             (for [tag-name tags]
               ^{:key tag-name} [:button {:on-click #(dispatch [::router-events/navigate
                                                                :route.marketplace/index
                                                                nil
                                                                {:search-tags [tag-name]}])
                                          :style {:display "inline"}} tag-name])

             [:div.buttons
              [:button {:on-click #(dispatch [::router-events/navigate
                                              :route.marketplace/index
                                              nil
                                              {:term title}])} "Search On Marketplace"]
              [:button {:on-click #(dispatch [::router-events/navigate
                                              :route.memefolio/index
                                              nil
                                              {:term title}])} "Search On Memefolio"]]]
            ;; history
            [:div.history {:style {:grid-area "history"}}
             [history-component address]]

            [:div.challenge {:style {:grid-area "challenge"}}
             [challenge-component meme]]

            ]
           ])))))
