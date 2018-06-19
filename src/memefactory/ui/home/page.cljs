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

(defn auctions-list [auctions]
  [:div.tiles
   (doall
    (for [{:keys [:meme-auction/address] :as auc} auctions]
      (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
        ^{:key address}
        [tiles/auction-tile {:on-buy-click #()} auc])))])

(def auction-node-graph [:meme-auction/address
                         :meme-auction/start-price
                         :meme-auction/end-price
                         :meme-auction/duration
                         :meme-auction/description
                         [:meme-auction/seller [:user/address]]
                         [:meme-auction/meme-token
                          [:meme-token/number
                           [:meme-token/meme
                            [:meme/title
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

(defmethod page :route/home [] 
  (let [search-atom (r/atom {:term ""})
        new-on-market (subscribe [::gql/query {:queries [new-on-marketplace-query]}])
        rare-finds (subscribe [::gql/query {:queries [rare-finds-query]}])
        random-picks (subscribe [::gql/query {:queries [random-picks-query]}])] 
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.home
        [:img.logo]
        [:p "Inspired by the work of Simon de la Rouviere and his Curation Markets design, the third district to be deployed to dthe district0x."]
        [:div.new-on-marketplace
         [:div.header
          [:img]
          [:div.middle
           [:h2.title "New On Marketplace"]
           [:h3.title "Lorem ipsum ..."]]
          [:a {:href (utils/path-with-query (utils/path :route.marketplace/index)
                                            {:order-by (str :meme-auctions.order-by/started-on)
                                             :order-dir :desc})} "See More"]
          [auctions-list (-> @new-on-market :search-meme-auctions :items)]]]

        [:div.rare-finds
         [:div.header
          [:img]
          [:div.middle
           [:h2.title "Rare Finds"]
           [:h3.title "Lorem ipsum ..."]]
          [:a {:href (utils/path-with-query (utils/path :route.marketplace/index)
                                            {:order-by (str :meme-auctions.order-by/meme-total-minted)
                                             :order-dir :asc})}
           "See More"]
          [auctions-list (-> @rare-finds :search-meme-auctions :items)]]]

        [:div.random-pics
         [:div.header
          [:img]
          [:div.middle
           [:h2.title "Random Picks"]
           [:h3.title "Lorem ipsum ..."]]
          [:a {:href (utils/path-with-query (utils/path :route.marketplace/index)
                                            {:order-by (str :meme-auctions.order-by/random)})}
           "See More"]]
         [auctions-list (-> @random-picks :search-meme-auctions :items)]]]])))


