(ns memefactory.ui.marketplace.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.marketplace.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [react-infinite]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.tiles :as tiles]))

(def react-infinite (r/adapt-react-class js/Infinite))
 
(defn marketplace-tiles [search-term]
  (let [auctions-search (subscribe [::gql/query {:queries [[:search-meme-auctions
                                                            #_{:first :after :title search-term}
                                                            [:total-count
                                                             :end-cursor
                                                             :has-next-page
                                                             [:items [:meme-auction/address
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
                                                                          :meme/total-minted]]]]]]]]]}])]
    (fn [search-term]
      (let [{:keys [:total-count :end-cursor :has-next-page :items]} (:search-meme-auctions @auctions-search)]
        [:div.tiles
         [react-infinite {:element-height 280
                          :container-height 300
                          :infinite-load-begin-edge-offset 100
                          :on-infinite-load (fn [] (.log js/console "Need to load more"))}
          (doall
           (for [{:keys [:meme-auction/address] :as auc} items]
             (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
               ^{:key address}
               [tiles/auction-tile {:on-click #()} auc])))]]))))

(defmethod page :route.marketplace/index []
  (let [search-atom (r/atom {:term ""})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.marketplace
        [:img.logo]
        [:p "Inspired by the work of Simon de la Rouviere and his Curation Markets design, the third district to be deployed to dthe district0x."]
        [:div.auctions.container
         [:h2.title "New On Marketplace"]
         [:h3.title "Lorem ipsum ..."]
         [marketplace-tiles (:term @search-atom)]]]])))


