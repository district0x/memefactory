(ns memefactory.ui.components.tiles
  (:require [memefactory.shared.utils :as shared-utils]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [district.ui.now.subs]
            [print.foo :refer [look] :include-macros true]
            [district.time :as time]
            [cljs-time.core :as t]
            [clojure.string :as str]
            [district.format :as format]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [district.ui.component.tx-button :as tx-button]))

(defn- random-img-url []
  (str "/assets/images/examplememe" (rand-int 2) ".png"))

(defn flippable-tile [{:keys [:front :back :id]}]
  (let [flipped? (r/atom false)
        flip #(swap! flipped? not)]
    (fn [{:keys [:front :back]}]
      [:div.container (merge {;;:style {:width 300 :height 500 :background-color :orange :margin 5} no styles here

                              :on-click (fn [event]
                                          (if id
                                            (when (= id (-> event
                                                            (aget "target")
                                                            (aget "id")))
                                              (flip))
                                            (flip)))}
                             (when id {:id id}))
       (if @flipped?
         back
         front)])))

(defn auction-front-tile [opts {:keys [:meme/image-hash] :as meme}]
  [:div.meme-card.front
   [:img {:src (random-img-url)}]
   [:div (str meme)]
   [:span (str "FRONT IMAGE" image-hash)]])

(defn auction-back-tile [{:keys [:on-buy-click] :as opts} meme-auction]
  (let [tx-id (str (random-uuid))
        now (subscribe [:district.ui.now.subs/now])
        end-time (t/plus (t/date-time (:meme-auction/started-on meme-auction))
                         (t/seconds (:meme-auction/duration meme-auction)))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/buy tx-id}])]
    (fn [{:keys [:on-buy-click] :as opts} meme-auction]
      (let [remaining (time/time-remaining (t/date-time @now)
                                           end-time)
            price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
       [:div.meme-card.back
        [:img {:src (random-img-url)}]
        [:img.logo]
        [:ol
         [:li [:label "Seller:"] [:span (:user/address (:meme-auction/seller meme-auction))]]
         [:li [:label "Current Price:"] [:span (format/format-eth price)]]
         [:li [:label "Start Price:"] [:span (format/format-eth (:meme-auction/start-price meme-auction))]]
         [:li [:label "End Price:"] [:span (format/format-eth (:meme-auction/end-price meme-auction))]]
         [:li [:label "End Price in:"] [:span (format/format-time-units remaining)]]]
        [:p.description (:meme-auction/description meme-auction)]
        [tx-button/tx-button {:primary true
                              :disabled false
                              :pending? @tx-pending?
                              :pending-text "Buying auction ..."
                              :on-click (fn []
                                          (dispatch [:meme-auction/buy {:send-tx/id tx-id
                                                                                 :meme-auction/address (:meme-auction/address meme-auction)
                                                                                 :value price}]))}
         "Buy"]]))))


(defn auction-tile [{:keys [:on-buy-click] :as opts} {:keys [:meme-auction/meme-token] :as meme-auction}]
  (let [now (subscribe [:district.ui.now.subs/now])]
    (fn []
      (let [price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
        [:div.compact-tile
        [flippable-tile {:front [auction-front-tile (-> meme-token :meme-token/meme)]
                         :back [auction-back-tile opts meme-auction]}]
        [:div.footer
         [:div.title (-> meme-token :meme-token/meme :meme/title)]
         [:div.number-minted (str (:meme-token/number meme-token)
                                  "/"
                                  (-> meme-token :meme-token/meme :meme/total-minted))]
         [:div.price (format/format-eth price)]]]))))

(defn meme-front-tile [opts {:keys [:meme/image-hash] :as meme}]
  [:div (str "FRONT " meme)])

(defn meme-back-tile [opts {:keys [] :as meme}]
  [:div (str "BACK " meme)])

(defn meme-tile [opts {:keys [] :as meme}]
  [:div.compact-tile
   [flippable-tile {:front [meme-front-tile opts meme]
                    :back [meme-back-tile opts meme]}]])
