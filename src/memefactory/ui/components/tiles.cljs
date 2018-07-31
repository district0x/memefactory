(ns memefactory.ui.components.tiles
  (:require [cljs-time.core :as t]
            [clojure.string :as str]
            [district.format :as format]
            [district.time :as time]
            [district.ui.component.form.input :as inputs]
            [district.ui.component.tx-button :as tx-button]
            [district.ui.graphql.subs :as gql]
            [district.ui.now.subs]
            [district.ui.web3-accounts.subs :as accounts-subs]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [memefactory.shared.utils :as shared-utils]
            [print.foo :refer [look] :include-macros true]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn- img-url-stub
  ([]
   (img-url-stub (rand-int 2)))
  ([n]
   (let [img (if (string? n)
               (get {"IHASH3" 1
                     "IHASH2" 0
                     "IHASH5" 2} n n)
               n)]
     (str "/assets/images/examplememe" img ".png"))))

(defn meme-image [image-hash]
  (let [gateway (subscribe [::gql/query
                            {:queries [[:config 
                                        [[:ipfs [:gateway]]]]]}])]        
    (when-not (:graphql/loading? @gateway)
      [:img {:src (str (format/ensure-trailing-slash (-> @gateway :config :ipfs :gateway)) image-hash)}])))

(defn flippable-tile [{:keys [:front :back :id]}]
  (let [flipped? (r/atom false)
        flip #(swap! flipped? not)]
    (fn [{:keys [:front :back]}]
      [:div.container (merge {:class (when @flipped? "flipped")
                              :on-click (fn [event]
                                          (if id
                                            (when (= id (-> event
                                                            (aget "target")
                                                            (aget "id")))
                                              (flip))
                                            (flip)))}
                             (when id {:id id}))
       back
       front])))

(defn auction-front-tile [opts meme-token]
  [:div.meme-card.front
   [:img {:src (img-url-stub (get-in meme-token [:meme-token/meme :meme/image-hash]))}]])

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
        [:img {:src (img-url-stub (get-in meme-auction [:meme-auction/meme-token
                                                        :meme-token/meme
                                                        :meme/image-hash]))}]
        [:div.overlay
         [:div.info
          [:div (str meme-auction)]
          [:ul.meme-data
           [:li [:label "Seller:"] [:span (:user/address (:meme-auction/seller meme-auction))]]
           [:li [:label "Current Price:"] [:span (format/format-eth price)]]
           [:li [:label "Start Price:"] [:span (format/format-eth (:meme-auction/start-price meme-auction))]]
           [:li [:label "End Price:"] [:span (format/format-eth (:meme-auction/end-price meme-auction))]]
           [:li [:label "End Price in:"] [:span (format/format-time-units remaining)]]]
          [:hr]
          [:p.description (:meme-auction/description meme-auction)]
          [inputs/pending-button {:pending? @tx-pending?
                                  :pending-text "Buying auction ..."
                                  :on-click (fn []
                                              (dispatch [:meme-auction/buy {:send-tx/id tx-id
                                                                            :meme-auction/address (:meme-auction/address meme-auction)
                                                                            :value price}]))}
           "Buy"]]]]))))


(defn auction-tile [{:keys [:on-buy-click] :as opts} {:keys [:meme-auction/meme-token] :as meme-auction}]
  (let [now (subscribe [:district.ui.now.subs/now])]
    (fn []
      (let [price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
        [:div.compact-tile
        [flippable-tile {:front [auction-front-tile opts meme-token]
                         :back [auction-back-tile opts meme-auction]}]
        [:div.footer
         [:div.title (-> meme-token :meme-token/meme :meme/title)]
         [:div.number-minted (str (:meme-token/number meme-token)
                                  "/"
                                  (-> meme-token :meme-token/meme :meme/total-minted))]
         [:div.price (format/format-eth price)]]]))))

(defn meme-front-tile [opts {:keys [:meme/image-hash] :as meme}]

  [:div.meme-card.front
   (meme-image image-hash)])

(defn meme-back-tile [opts {:keys [] :as meme}]
  [:div.meme-card.back
   [:div.overlay
    [:div.info
     [:ul.meme-data
      [:li [:label "Creator:"] [:span (-> meme :reg-entry/creator :user/address )]]]]]])

(defn meme-tile [opts {:keys [] :as meme}]
  [:div.compact-tile
   [flippable-tile {:front [meme-front-tile opts meme]
                    :back [meme-back-tile opts meme]}]])
