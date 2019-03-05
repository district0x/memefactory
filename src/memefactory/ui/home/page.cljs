(ns memefactory.ui.home.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.marketplace.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [memefactory.ui.components.tiles :as tiles]
   [print.foo :refer [look] :include-macros true]
   [district.format :as format]
   [district.time :as time]
   [memefactory.ui.utils :as utils]
   [district.ui.router.events :as router-events]
   [memefactory.ui.components.challenge-list :refer [current-period-ends]]
   [memefactory.ui.components.spinner :as spinner]
   [goog.string :as gstring]))

(defn take-max-multiple-of [n xs]
  (if (< (count xs) n)
    xs
    (->> (partition n xs)
         (apply concat))))

(defn auctions-list [auctions]
  (let [auctions (take-max-multiple-of 3 auctions)]
    [:div.tiles
     (if (empty? auctions)
       [:div.no-items-found "No items found."]
       (doall
        (for [{:keys [:meme-auction/address] :as auc} auctions]
          (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
            [tiles/auction-tile {:key address
                                 :on-buy-click #()} auc]))))]))

(defn trending-vote-tile [{:keys [:reg-entry/address :meme/image-hash :reg-entry/creator :challenge/commit-period-end
                                  :challenge/challenger :challenge/comment] :as meme}]

  (let [{:keys [:user/total-created-challenges-success :user/total-created-challenges]} challenger]
    [:div.compact-tile
    [tiles/flippable-tile {:front [tiles/meme-image image-hash]
                           :back [:div.meme-card.back
                                  [:div.overlay
                                   [:div.info
                                    [:ul.meme-data
                                     [:li [:label "Creator:"]
                                      [:span {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                                    {:address (:user/address creator)}
                                                                    {:tab :created}])}
                                       (:user/address creator)]]
                                     [:li [:label "Voting period ends in: "]
                                      [:span (-> (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                      (utils/gql-date->date commit-period-end))
                                                 format/format-time-units)]]
                                     [:li [:label "Challenger :"]
                                      [:span {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                                    {:address (-> challenger :user/address)}
                                                                    {:tab :curated}])}
                                       (-> challenger :user/address)]]
                                     [:li [:label "Challenger success rate:"]
                                      [:span (gstring/format "%d/%d (%d%%)"
                                                             total-created-challenges-success
                                                             total-created-challenges
                                                             (if (pos? total-created-challenges) (/ (* 100 total-created-challenges-success) total-created-challenges) 0))]]]
                                    [:hr]
                                    [:p.comment comment]]]]}]
    [:div.footer {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                        {:address address}
                                        nil])}
     [:div.title (-> meme :meme/title)]]]))


(defn memes-list [memes empty-msg]
  (let [memes (take-max-multiple-of 3 memes)]
    [:div.tiles

     (if (empty? memes)
       [:div.no-items-found empty-msg]
       (doall
        (for [{:keys [:reg-entry/address :challenge/votes-total] :as m} memes]
          ^{:key address}
          ;;:div.tile-wrapper
          [trending-vote-tile m]
          #_[:div.votes-total (str "Vote amount "(or votes-total 0))])))]))

(def auction-node-graph [:meme-auction/address
                         :meme-auction/start-price
                         :meme-auction/end-price
                         :meme-auction/duration
                         :meme-auction/description
                         :meme-auction/started-on
                         [:meme-auction/seller [:user/address]]
                         [:meme-auction/meme-token
                          [:meme-token/number
                           [:meme-token/meme
                            [:reg-entry/address
                             :meme/title
                             :meme/image-hash
                             :meme/total-minted
                             :meme/number]]]]])

(def new-on-marketplace-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/started-on
    :statuses [:meme-auction.status/active]
    :first 6}
   [[:items auction-node-graph]]])

(def rare-finds-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/meme-total-minted
    :statuses [:meme-auction.status/active]
    :order-dir :asc
    :first 6}
   [[:items auction-node-graph]]])

(def random-picks-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/random
    :statuses [:meme-auction.status/active]
    :first 6}
   [[:items auction-node-graph]]])

(def trending-votes-query
  [:search-memes
   {:order-by :memes.order-by/daily-total-votes
    :order-dir :desc
    :statuses [:reg-entry.status/commit-period]
    :first 6}
   [[:items [:reg-entry/address
             [:reg-entry/creator
              [:user/address]]
             [:challenge/challenger
              [:user/address
               :user/total-created-challenges-success
               :user/total-created-challenges]]
             :meme/title
             :meme/image-hash
             :challenge/votes-total
             :challenge/commit-period-end
             :challenge/comment]]]])

(defmethod page :route/home []
  (let [search-atom (r/atom {:term ""})
        new-on-market (subscribe [::gql/query {:queries [new-on-marketplace-query]}])
        rare-finds (subscribe [::gql/query {:queries [rare-finds-query]}])
        random-picks (subscribe [::gql/query {:queries [random-picks-query]}])
        trending-votes (subscribe [::gql/query {:queries [trending-votes-query]}])
        ]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.home
        [:p.inspired "Inspired by the work of Simon de la Rouviere and his Curation Markets design, the third district to be deployed to the district0x."]
        [:section.meme-highlights
         [:div.new-on-marketplace
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "New On Marketplace"]
            [:h3.title "Lorem ipsum ..."]]]
          [auctions-list (-> @new-on-market :search-meme-auctions :items)]
          [:a.more {:on-click #(dispatch [::router-events/navigate :route.marketplace/index
                                          nil
                                          {:order-by "started-on" :order-dir "desc"}])}
           "See More"]]

         [:div.rare-finds
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Rare Finds"]
            [:h3.title "Lorem ipsum ..."]]]
          [auctions-list (-> @rare-finds :search-meme-auctions :items)]
          [:a.more {:on-click #(dispatch [::router-events/navigate :route.marketplace/index
                                         nil
                                         {:order-by "meme-total-minted" :order-dir "asc"}])}
           "See More"]]

         [:div.random-pics
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Random Picks"]
            [:h3.title "Lorem ipsum ..."]]]
          [auctions-list (-> @random-picks :search-meme-auctions :items)]
          [:a.more {:on-click #(dispatch [::router-events/navigate :route.marketplace/index
                                         nil
                                         {:order-by "random"}])}
           "See More"]]

         [:div.trending-votes
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Trending Votes"]
            [:h3.title "Lorem ipsum ..."]]]
          [memes-list (-> @trending-votes :search-memes :items) "No votes are running currently"]]]]])))
