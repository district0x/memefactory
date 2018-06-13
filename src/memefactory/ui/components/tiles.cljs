(ns memefactory.ui.components.tiles
  (:require [memefactory.shared.utils :as shared-utils]
            [reagent.core :as r]))

(defn flippable-tile [{:keys [:front :back]}]
  (let [flipped? (r/atom false)]
    (fn [{:keys [:front :back]}]
      [:div.container {:style {:width 200 :height 280 :background-color :orange :margin 5} ;; for testing
                       :on-click #(swap! flipped? not)}
       (if @flipped?
         back
         front)])))

(defn auction-front-tile [opts {:keys [:meme/image-hash] :as meme}]
  [:div.meme-card-front
   [:img]
   [:span (str "FRONT IMAGE" image-hash)]])

(defn auction-back-tile [{:keys [:on-buy-click] :as opts} meme-auction]
  (let [now 231
        price (shared-utils/calculate-meme-auction-price meme-auction now)]
    [:div.meme-card.back
     [:img.logo]
     [:ol
      [:li [:label "Seller"] [:span (:user/address (:meme-auction/seller meme-auction))]]
      [:li [:label "Current Price"] [:span "1.54 EHT" #_price]]
      [:li [:label "Start Price"] [:span (:meme-auction/start-price meme-auction)]]
      [:li [:label "End Price"] [:span (:meme-auction/end-price meme-auction)]]
      [:li [:label "End Price in"] [:span (:meme-auction/duration meme-auction)]]]
     [:p.description (:meme-auction/description meme-auction)]
     [:a {:on-click on-buy-click} "BUY"]]))

(defn auction-tile [{:keys [:on-buy-click] :as opts} {:keys [:meme-auction/meme-token] :as meme-auction}]
  [:div.compact-tile 
   [flippable-tile {:front [auction-front-tile (-> meme-token :meme-token/meme)]
                    :back [auction-back-tile opts meme-auction]}]
   [:div.footer
    [:div.title (-> meme-token :meme-token/meme :meme/title)]
    [:div.number-minted (str (:meme-token/number meme-token)
                             "/"
                             (-> meme-token :meme-token/meme :meme/total-minted))]
    [:div.price "1.54 EHT" #_price]]])

(defn meme-front-tile [])

(defn meme-back-tile [])
