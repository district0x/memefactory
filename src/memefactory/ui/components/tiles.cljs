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
            [reagent.core :as r]
            [memefactory.ui.contract.meme-auction :as meme-auction]
            [district.ui.router.events :as router-events]))

(defn- format-price [price]
  (format/format-eth (/ price 1e18) {:max-fraction-digits 2
                                     :min-fraction-digits 2}))

(defn meme-image [image-hash]
  (let [url (-> @(subscribe [::gql/query
                             {:queries [[:config
                                         [[:ipfs [:gateway]]]]]}])
                :config :ipfs :gateway)]
    [:div.meme-card.front
     (if (and url image-hash)
       [:img.meme-image {:src (str (format/ensure-trailing-slash url) image-hash)}]
       [:div.meme-placehodler [:img {:src "/assets/icons/mememouth.png"}]])]))

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
         [meme-image (get-in meme-auction [:meme-auction/meme-token
                                           :meme-token/meme
                                           :meme/image-hash])]
         [:div.overlay
          [:div.info
           [:ul.meme-data
            [:li [:label "Seller:"] [:span (:user/address (:meme-auction/seller meme-auction))]]
            [:li [:label "Current Price:"] [:span (format-price price)]]
            [:li [:label "Start Price:"] [:span (format-price (:meme-auction/start-price meme-auction))]]
            [:li [:label "End Price:"] [:span (format-price (:meme-auction/end-price meme-auction))]]
            [:li [:label "End Price in:"] [:span (format/format-time-units remaining)]]]
           [:hr]
           [:p.description (:meme-auction/description meme-auction)]
           [inputs/pending-button {:pending? @tx-pending?
                                   :pending-text "Buying auction ..."
                                   :on-click (fn []
                                               (dispatch [::meme-auction/buy {:send-tx/id tx-id
                                                                              :meme-auction/address (:meme-auction/address meme-auction)
                                                                              :value price}]))}
            "Buy"]]]]))))

(defn auction-tile [{:keys [:on-buy-click] :as opts} {:keys [:meme-auction/meme-token] :as meme-auction}]
  (let [now (subscribe [:district.ui.now.subs/now])]
    (fn []
      (let [price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
        [:div.compact-tile
         [flippable-tile {:front [meme-image (get-in meme-token [:meme-token/meme :meme/image-hash])]
                          :back [auction-back-tile opts meme-auction]}]
         [:div.footer {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                             nil
                                             {:reg-entry/address (-> meme-token :meme-token/meme :reg-entry/address)}])}
          [:div.title (-> meme-token :meme-token/meme :meme/title)]
          [:div.token-id (str "#"(-> meme-token :meme-token/token-id))]
          [:div.number-minted (str (:meme-token/number meme-token)
                                   "/"
                                   (-> meme-token :meme-token/meme :meme/total-minted))]
          [:div.price (format-price price)]]]))))

(defn meme-back-tile [meme]
  [:div.meme-card.back
   [:div.overlay
    [:div.info
     [:ul.meme-data
      [:li [:label "Creator:"]
       [:span (-> meme :reg-entry/creator :user/address)]]]]]])

(defn meme-tile [{:keys [:reg-entry/address :meme/image-hash] :as meme}]
  [:div.compact-tile
   [flippable-tile {:front [meme-image image-hash]
                    :back [meme-back-tile meme]}]
   [:div.footer {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                                              nil
                                                              {:reg-entry/address address}])}
    [:div.title (-> meme :meme/title)]]])
