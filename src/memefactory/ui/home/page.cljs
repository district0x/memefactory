(ns memefactory.ui.home.page
  (:require
   [district.format :as format]
   [district.time :as time]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.events :as router-events]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.challenge-list :refer [current-period-ends]]
   [memefactory.ui.components.general :refer [nav-anchor]]
   [memefactory.ui.components.panels :refer [no-items-found]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.marketplace.events :as mk-events]
   [memefactory.ui.utils :as utils]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]))

(defn take-max-multiple-of [n xs]
  (if (< (count xs) n)
    xs
    (->> (partition n xs)
         (apply concat))))

(defn auctions-list [auctions loading?]
  (let [auctions (take-max-multiple-of 3 auctions)]
    [:div.tiles
     (if (and (empty? auctions)
              (not loading?))
       [no-items-found]
       (if loading?
         [:div.spinner-container [spinner/spin]]
         [:div (doall
                (for [{:keys [:meme-auction/address] :as auc} auctions]
                  (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
                    [tiles/auction-tile {:key address
                                         :show-cards-left? true} auc])))]))]))

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
                                      [nav-anchor {:route :route.memefolio/index
                                                   :params {:address (:user/address creator)}
                                                   :query {:tab :created}}
                                       (:user/address creator)]]
                                     [:li [:label "Voting period ends in: "]
                                      [:span (-> (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                      (utils/gql-date->date commit-period-end))
                                                 format/format-time-units)]]
                                     [:li [:label "Challenger :"]
                                      [nav-anchor {:route :route.memefolio/index
                                                   :params {:address (:user/address challenger)}
                                                   :query {:tab :curated}}
                                       (:user/address creator)
                                       (-> challenger :user/address)]]
                                     [:li [:label "Challenger success rate:"]
                                      [:span (gstring/format "%d/%d (%d%%)"
                                                             total-created-challenges-success
                                                             total-created-challenges
                                                             (if (pos? total-created-challenges) (/ (* 100 total-created-challenges-success) total-created-challenges) 0))]]]
                                    [:hr]
                                    [:p.comment comment]]]]}]
     [nav-anchor {:route :route.meme-detail/index
                  :params {:address address}}
      [:div.footer
       [:div.title (-> meme :meme/title)]]]]))


(defn memes-list [memes empty-msg loading?]
  (let [memes (take-max-multiple-of 3 memes)]
    [:div.tiles

     (if (and (empty? memes)
              (not loading?))
       [:div.no-items-found empty-msg]
       (if loading?
         [:div.spinner-container [spinner/spin]]
         (doall
          (for [{:keys [:reg-entry/address :challenge/votes-total] :as m} memes]
            ^{:key address}
            ;;:div.tile-wrapper
            [trending-vote-tile m]
            #_[:div.votes-total (str "Vote amount "(or votes-total 0))]))))]))

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
    :order-dir :desc
    :group-by :meme-auctions.group-by/cheapest
    :statuses [:meme-auction.status/active]
    :first 6}
   [[:items auction-node-graph]]])

(def rare-finds-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/meme-total-minted
    :statuses [:meme-auction.status/active]
    :group-by :meme-auctions.group-by/cheapest
    :order-dir :asc
    :first 6}
   [[:items auction-node-graph]]])

(def random-picks-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/random
    :statuses [:meme-auction.status/active]
    :group-by :meme-auctions.group-by/cheapest
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
        trending-votes (subscribe [::gql/query {:queries [trending-votes-query]}])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}
        :search-atom search-atom}
       [:div.home
        [:p.inspired "A decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."]
        [:section.meme-highlights
         [:div.new-on-marketplace
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "New On Marketplace"]
            [:h3.title "The latest additions to Meme Factory"]]]
          [auctions-list (-> @new-on-market :search-meme-auctions :items) (:graphql/loading? @new-on-market)]
          [nav-anchor {:route :route.marketplace/index
                       :query {:order-by "started-on" :order-dir "desc"}
                       :class "more"}
           "See More"]]

         [:div.rare-finds
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Rare Finds"]
            [:h3.title "Exceptional, infrequently traded memes"]]]
          [auctions-list (-> @rare-finds :search-meme-auctions :items) (:graphql/loading? @rare-finds)]
          [nav-anchor {:route :route.marketplace/index
                       :query {:order-by "meme-total-minted" :order-dir "asc"}
                       :class "more"}
           "See More"]]

         [:div.random-pics
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Random Picks"]
            [:h3.title "A random assortment of memes for sale"]]]
          [auctions-list (-> @random-picks :search-meme-auctions :items) (:graphql/loading? @random-picks)]
          [nav-anchor {:route :route.marketplace/index
                       :query {:order-by "random"}
                       :class "more"}
           "See More"]]

         [:div.trending-votes
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Trending Votes"]
            [:h3.title "The most active challenges in the last day"]]]
          [memes-list
           (-> @trending-votes :search-memes :items)
           "No votes are running currently"
           (:graphql/loading? @trending-votes)]]]]])))
