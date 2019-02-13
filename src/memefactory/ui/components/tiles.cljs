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
            [district.ui.router.events :as router-events]
            [memefactory.ui.utils :as ui-utils :refer [format-price]]))

(defn meme-image [image-hash]
  (let [url (-> @(subscribe [::gql/query
                             {:queries [[:config
                                         [[:ipfs [:gateway]]]]]}])
                :config :ipfs :gateway)]
    [:div.meme-card.front
     (if (and url (not-empty image-hash))
       [:img.meme-image {:src (str (format/ensure-trailing-slash url) image-hash)}]
       [:div.meme-placehodler [:img {:src "/assets/icons/mememouth.png"}]])]))

(defn flippable-tile [{:keys [:front :back :flippable-classes]}]
  (let [flipped? (r/atom false)
        flip #(swap! flipped? not)]
    (fn [{:keys [:front :back]}]
      [:div.container {:class (when @flipped? "flipped")
                       :on-click (fn [event]
                                   (if (not-empty flippable-classes)
                                     (when (contains? flippable-classes
                                                      (-> event
                                                          (aget "target")
                                                          (aget "className")))
                                       (flip))
                                     (flip)))}
       back
       front])))

(defn auction-back-tile [opts meme-auction]
  (let [tx-id (str (:meme-auction/address meme-auction) "auction")
        active-account (subscribe [:district.ui.web3-accounts.subs/active-account])
        now (subscribe [:district.ui.now.subs/now])
        end-time (t/plus (ui-utils/gql-date->date (:meme-auction/started-on meme-auction))
                         (t/seconds (:meme-auction/duration meme-auction)))
        buy-tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/buy tx-id}])
        cancel-tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/cancel tx-id}])
        buy-tx-success? (subscribe [::tx-id-subs/tx-success? {:meme-auction/buy tx-id}])
        cancel-tx-success? (subscribe [::tx-id-subs/tx-success? {:meme-auction/cancel tx-id}])]
    (fn [opts meme-auction]
      (let [remaining (-> (time/time-remaining (t/date-time @now)
                                               end-time)
                          (dissoc :seconds))
            price (shared-utils/calculate-meme-auction-price (-> meme-auction
                                                                 (update :meme-auction/started-on #(.getTime (ui-utils/gql-date->date %))))
                                                             (.getTime @now))
            title (-> meme-auction :meme-auction/meme-token :meme-token/meme :meme/title)]
        [:div.meme-card.back
         [meme-image (get-in meme-auction [:meme-auction/meme-token
                                           :meme-token/meme
                                           :meme/image-hash])]
         [:div.overlay
          [:div.info
           [:ul.meme-data
            [:li [:label "Seller:"] [:span {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                                  {:address (:user/address (:meme-auction/seller meme-auction))}
                                                                  {:tab :selling}])}
                                     (:user/address (:meme-auction/seller meme-auction))]]
            [:li [:label "Current Price:"] [:span (format-price price)]]
            [:li [:label "Start Price:"] [:span (format-price (:meme-auction/start-price meme-auction))]]
            [:li [:label "End Price:"] [:span (format-price (:meme-auction/end-price meme-auction))]]
            [:li [:label "End Price in:"] [:span (format/format-time-units remaining)]]]
           [:hr]
           [:p.description (:meme-auction/description meme-auction)]
           (if (= (-> meme-auction :meme-auction/seller :user/address)
                  @active-account)

             [inputs/pending-button {:pending? @cancel-tx-pending?
                                     :pending-text "Cancelling..."
                                     :disabled (or @cancel-tx-pending? @cancel-tx-success?)
                                     :on-click (fn [e]
                                                 (.stopPropagation e)
                                                 (dispatch [::meme-auction/cancel {:send-tx/id tx-id
                                                                                   :meme-auction/address (:meme-auction/address meme-auction)
                                                                                   :meme/title title}]))}
              (if @cancel-tx-success? "Canceled" "Cancel Sell")]

             [inputs/pending-button {:pending? @buy-tx-pending?
                                     :pending-text "Buying..."
                                     :disabled (or @buy-tx-pending? @buy-tx-success?)
                                     :class (when-not @buy-tx-success? "buy")
                                     :on-click (fn [e]
                                                 (.stopPropagation e)
                                                 (dispatch [::meme-auction/buy {:send-tx/id tx-id
                                                                                :meme-auction/address (:meme-auction/address meme-auction)
                                                                                :meme/title title
                                                                                :value price}]))}
              (if @buy-tx-success? "Bought" "Buy")])]]]))))

(defn auction-tile [opts {:keys [:meme-auction/meme-token] :as meme-auction}]
  (let [now (subscribe [:district.ui.now.subs/now])]
    (fn [opts {:keys [:meme-auction/meme-token] :as meme-auction}]
      (let [price (shared-utils/calculate-meme-auction-price (-> meme-auction
                                                                 (update :meme-auction/started-on #(.getTime (ui-utils/gql-date->date %))))
                                                             (.getTime @now))]
        [:div.compact-tile
         [flippable-tile {:front [meme-image (get-in meme-token [:meme-token/meme :meme/image-hash])]
                          :back [auction-back-tile opts meme-auction]}]
         [:div.footer {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                             {:address (-> meme-token :meme-token/meme :reg-entry/address) }
                                             nil])}
          [:div.token-id (str "#"(-> meme-token :meme-token/meme :meme/number))]
          [:div.title (-> meme-token :meme-token/meme :meme/title)]
          [:div.number-minted (str (:meme-token/number meme-token)
                                   "/"
                                   (-> meme-token :meme-token/meme :meme/total-minted))]
          [:div.price (format-price price)]]]))))

(defn meme-back-tile [{:keys [:reg-entry/created-on :meme/total-minted :meme/number] :as meme}]
  (let [user-address (-> meme :reg-entry/creator :user/address)]
    [:div.meme-card.back
     [:div.overlay
      [:div.info
       [:ul.meme-data
        (when number
          [:li [:label "Registry Number:"]
           (str "#" number)])
        [:li [:label "Creator:"]
         [:span {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                       {:address user-address}
                                       {:tab :created}])}
          user-address]]
        [:li [:label "Created:"]
         (let [formated-time (-> (time/time-remaining (t/date-time (ui-utils/gql-date->date created-on)) (t/now))
                                 (dissoc :seconds)
                                 format/format-time-units)]
           (if-not (empty? formated-time)
             (str formated-time " ago")
             "less than a minute ago"))]
        [:li [:label "Issued:"]
         (str total-minted " cards")]]]]]))

(defn meme-tile [{:keys [:reg-entry/address :meme/image-hash] :as meme}]
  [:div.compact-tile
   [flippable-tile {:front [meme-image image-hash]
                    :back [meme-back-tile meme]}]
   [:div.footer {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                       {:address address}
                                       nil])}
    [:div.title (-> meme :meme/title)]]])
