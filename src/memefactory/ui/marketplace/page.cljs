(ns memefactory.ui.marketplace.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.marketplace.events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe]]
   [reagent.core :as r]))

(defn meme-tiles [search-term]
  (let [auctions-search (subscribe [::gql/query {:queries [[:search-meme-auctions
                                                            #_{:title search-term}
                                                            [:total-count
                                                             :end-cursor
                                                             :has-next-page
                                                             [:items [:meme-auction/address
                                                                      :meme-auction/start-price
                                                                      :meme-auction/end-price
                                                                      :meme-auction/duration
                                                                      [:meme-auction/seller [:user/address]]
                                                                      [:meme-auction/meme-token
                                                                       [[:meme-token/meme
                                                                         [:meme/title
                                                                          :meme/image-hash]]]]]]]]]}])
        card-flipped? (r/atom {})]
    (fn [search-term]
      (let [{:keys [:total-count :end-cursor :has-next-page :items]} (:search-meme-auctions @auctions-search)]
        [:ol.tiles
         (doall
          (for [{:keys [:meme-auction/address] :as auc} items]
            ^{:key address}      
            [:li.compact-tile
             [:div.container {:on-click #(swap! card-flipped? update address not)} 
              (if-not (get @card-flipped? address)
                [:div.meme-card-front
                 [:img][:span (str "FRONT IMAGE" (-> auc :meme-auction/meme-token :meme-token/meme :meme/image-hash))]]
              
                [:div.meme-card.back
                 [:img.logo]
                 [:ol
                  [:li [:label "Seller"] [:span (-> auc :meme-auction/seller :user/address)]]
                  [:li [:label "Current Price"] [:span "$$$$"]]
                  [:li [:label "Start Price"] [:span (:meme-auction/start-price auc)]]
                  [:li [:label "End Price"] [:span (:meme-auction/end-price auc)]]
                  [:li [:label "End Price in"] [:span (:meme-auction/duration auc)]]]
                 [:p.description (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
                 [:a "BUY"]])
              [:div.footer
               [:div.title (-> auc)]
               [:div.sold-amount (-> auc)]
               [:div.price "$$$$"]]]]))]))))

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
         [meme-tiles (:term @search-atom)]]]])))
