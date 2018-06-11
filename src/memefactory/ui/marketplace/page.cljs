(ns memefactory.ui.marketplace.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.marketplace.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [memefactory.shared.utils :as shared-utils]))

(defn now-in-seconds []
  123)

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
                                                                      :meme-auction/description
                                                                      [:meme-auction/seller [:user/address]]
                                                                      [:meme-auction/meme-token
                                                                       [:meme-token/number
                                                                        [:meme-token/meme
                                                                         [:meme/title
                                                                          :meme/image-hash
                                                                          :meme/total-minted]]]]]]]]]}])
        card-flipped? (r/atom {})]
    (fn [search-term]
      (let [{:keys [:total-count :end-cursor :has-next-page :items]} (:search-meme-auctions @auctions-search)]
        [:ol.tiles
         (doall
          (for [{:keys [:meme-auction/address] :as auc} items]
            (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)
                  now (now-in-seconds)
                  price (shared-utils/calculate-meme-auction-price auc now)]
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
                    [:li [:label "Current Price"] [:span "1.54 EHT" #_price]]
                    [:li [:label "Start Price"] [:span (:meme-auction/start-price auc)]]
                    [:li [:label "End Price"] [:span (:meme-auction/end-price auc)]]
                    [:li [:label "End Price in"] [:span (:meme-auction/duration auc)]]]
                   [:p.description (:meme-auction/description auc)]
                   [:a {:on-click #(dispatch [::mk-events/buy-meme-auction address])} "BUY"]])
                [:div.footer
                 [:div.title (str title)]
                 [:div.number-minted (str (-> auc :meme-auction/meme-token :meme-token/number)
                                          "/"
                                          (-> auc :meme-auction/meme-token :meme-token/meme :meme/total-minted))]
                 [:div.price "1.54 EHT" #_price]]]])))]))))

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
