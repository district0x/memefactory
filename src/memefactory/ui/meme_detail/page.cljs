(ns memefactory.ui.meme-detail.page
  (:require
;;   [district.time :as time]
   [cljsjs.d3]
   [cljs-time.format :as time-format]
   [cljs-time.core :as t]
   [district.ui.router.events :as router-events]
   [cljs-web3.core :as web3]
   [district.format :as format]
   [memefactory.ui.utils :as ui-utils]
   [memefactory.shared.utils :as shared-utils]
   [district.graphql-utils :as graphql-utils]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.ui.components.app-layout :as app-layout]
   [print.foo :refer [look] :include-macros true]
   [reagent.core :as r]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [memefactory.ui.components.tiles :as tiles]
   ))

;; (def d3 js/d3)

(def description "Lorem ipsum dolor sit amet, consectetur adipiscing elit")

(def time-formatter (time-format/formatter "EEEE, ddo MMMM, yyyy 'at' HH:mm:ss Z"))

(defn meme-creator [{:keys [:user/address :user/creator-rank :user/total-created-memes
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

(defn history [address]
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
                            :height "50px"}}
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
                                         (.value (fn [d] (prn (aget d "value"))
                                                   (aget d "value"))))
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

(defn challenge [{:keys [:reg-entry/status :challenge/created-on :challenge/comment
                         :challenge/votes-for :challenge/votes-against :challenge/votes-total
                         :challenge/challenger :challenge/vote] :as meme}]
  (let [status (graphql-utils/gql-name->kw status)
        areas "'header header header header header header'
               'status status challenger challenger votes votes'"]

    ;; (prn meme status)

    (case status
      :reg-entry.status/whitelisted
      [:div.whitelisted {:style {:display "grid"
                                 :grid-template-areas areas}}

       [:div.header {:style {:grid-area "header"}}
        [:div.title [:h1 "Challenge"]]
        [:span (str "This meme was challenged on " (time-format/unparse time-formatter (t/local-date-time (ui-utils/gql-date->date created-on))))]]

       [:div.status {:style {:grid-area "status"}}
        [:b "Challenge status"]
        [:div.description "Resolved, accepted"]]

       (let [{:keys [:user/address :user/challenger-rank :user/challenger-total-earned
                     :user/total-created-challenges :user/total-created-challenges-success]} challenger]
         [:div.challenger {:style {:grid-area "challenger"}}
          [:b "Challenger"]
          [:div.rank (str "Rank: #" challenger-rank " (" (format/format-dnt challenger-total-earned) ")")]
          [:div.success (str "Success rate: " total-created-challenges-success "/" total-created-challenges " ("
                             (format/format-percentage total-created-challenges-success total-created-challenges) ")")]
          [:div.address (str "Address: " address)]
          [:i comment]])

       ;; TODO: graph
       ;; TODO: challenge from active address
       (let [{:keys [:vote/option :vote/reward :vote/claimed-reward-on]} vote
             option (graphql-utils/gql-name->kw option)]
         [:div.votes {:style {:grid-area "votes"}}

          [:div {:style {:float "left"}}
           [donut-chart meme]]

          [:div {:style {:float "right"}}
           [:div.dank (str "Voted Dank: " (format/format-percentage votes-for votes-total) " - " votes-total)]
           [:div.stank (str "Voted Stank: " (format/format-percentage votes-against votes-total) " - " votes-total)]
           [:div.total (str "Total voted: " votes-total)]
           (when (contains? #{:vote-option/vote-for :vote-option/vote-against} option)
             [:div.vote (str "You voted: " (case option
                                             :vote-option/vote-for "DANK"
                                             :vote-option/vote-against "STANK"))]
             [:div.reward (str "Your reward: " (format/format-dnt reward))])]

          ;; TODO: claim reward button

          ])

       ]

      :default [:div "TODO"]

      )))

;;  TODO:  related
(defmethod page :route.meme-detail/index []
  (let [{:keys [:name :query :params]} @(re-frame/subscribe [::router-subs/active-page])
        {:keys [:address]} query
        active-account (subscribe [::accounts-subs/active-account])
        query (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                 [
                                                  :reg-entry/status
                                                  :meme/image-hash
                                                  :meme/meta-hash
                                                  :meme/number
                                                  :meme/title
                                                  :meme/total-supply

                                                  [:meme/owned-meme-tokens {:owner @active-account}
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

                                                  [:challenge/vote {:vote/voter @active-account}
                                                   [:vote/option
                                                    :vote/reward
                                                    :vote/claimed-reward-on]]

                                                  #_[:meme/meme-auctions
                                                   [:meme-auction/address]]

                                                  ]]
                                                [:search-tags [[:items [:tag/name]]]]]}])]

    (when-not (:graphql/loading? @query)

;;      (prn @query)

      (if-let [meme (:meme @query)]
        (let [{:keys [:meme/image-hash :meme/title :reg-entry/status :meme/total-supply
                      :meme/owned-meme-tokens :reg-entry/creator #_:meme/meme-auctions]} meme
              token-count (->> owned-meme-tokens
                               (map :meme-token/token-id)
                               (filter shared-utils/not-nil?)
                               count)
              ]

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

                      [meme-creator creator]

                      (for [tag-name (->> @query :search-tags :items (mapv :tag/name))]
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
                   [history address]]

                  [:div.challenge {:style {:grid-area "challenge"}}
                   [challenge meme]]

                  ]
         ])))))
