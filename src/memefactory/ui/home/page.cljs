(ns memefactory.ui.home.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.marketplace.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [react-infinite]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.tiles :as tiles]
   [print.foo :refer [look] :include-macros true]
   [memefactory.ui.utils :as utils]))


(defn take-max-multiple-of [n xs]
  (if (< (count xs) n)
    xs
    (->> (partition n xs)
         (apply concat))))

(defn auctions-list [auctions]
  (let [auctions (take-max-multiple-of 3 auctions)]
   [:div.tiles
    (doall
     (for [{:keys [:meme-auction/address] :as auc} auctions]
       (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
         ^{:key address}
         [tiles/auction-tile {:on-buy-click #()} auc])))]))

(defn memes-list [memes]
  (let [memes (take-max-multiple-of 3 memes)]
   [:div.tiles
    (doall
     (for [{:keys [:reg-entry/address :challenge/votes-total] :as m} memes]
       ^{:key address}
       [:div
        [tiles/meme-tile {:on-buy-click #()} m]
        [:div.votes-total (str "Vote amount "(or votes-total 0))]]))]))

(def auction-node-graph [:meme-auction/address
                         :meme-auction/start-price
                         :meme-auction/end-price
                         :meme-auction/duration
                         :meme-auction/description
                         [:meme-auction/seller [:user/address]]
                         [:meme-auction/meme-token
                          [:meme-token/number
                           [:meme-token/meme
                            [:reg-entry/address
                             :meme/title
                             :meme/image-hash
                             :meme/total-minted]]]]])

(def new-on-marketplace-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/started-on
    :first 6}
   [[:items auction-node-graph]]])

(def rare-finds-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/meme-total-minted
    :order-dir :asc
    :first 6}
   [[:items auction-node-graph]]])

(def random-picks-query
  [:search-meme-auctions
   {:order-by :meme-auctions.order-by/random
    :first 6}
   [[:items auction-node-graph]]])

(def trending-votes-query
  [:search-memes
   {:order-by :memes.order-by/daily-total-votes
    :order-dir :desc
    :first 6}
   [[:items [:reg-entry/address
             :meme/title
             :meme/image-hash
             :challenge/votes-total]]]])

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
        [:p.inspired "Inspired by the work of Simon de la Rouviere and his Curation Markets design, the third district to be deployed to dthe district0x."]
        [:section.meme-highlights
         [:div.new-on-marketplace
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "New On Marketplace"]
            [:h3.title "Lorem ipsum ..."]]]
          [auctions-list (-> @new-on-market :search-meme-auctions :items)]
          [:a.more {:href (utils/path-with-query (utils/path :route.marketplace/index)
                                                 {:order-by "started-on"
                                                  :order-dir "desc"})} "See More"]]

         [:div.rare-finds
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Rare Finds"]
            [:h3.title "Lorem ipsum ..."]]]
          [auctions-list (-> @rare-finds :search-meme-auctions :items)]
          [:a.more {:href (utils/path-with-query (utils/path :route.marketplace/index)
                                                 {:order-by "meme-total-minted"
                                                  :order-dir "asc"})}
           "See More"]]

         [:div.random-pics
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Random Picks"]
            [:h3.title "Lorem ipsum ..."]]]
          [auctions-list (-> @random-picks :search-meme-auctions :items)]
          [:a.more {:href (utils/path-with-query (utils/path :route.marketplace/index)
                                                 {:order-by "random"})}
           "See More"]]

         [:div.trending-votes
          [:div.icon]
          [:div.header
           [:div.middle
            [:h2.title "Trending Votes"]
            [:h3.title "Lorem ipsum ..."]]]
          [memes-list (-> @trending-votes :search-memes :items)]]]]])))
