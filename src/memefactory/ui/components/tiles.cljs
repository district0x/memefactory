(ns memefactory.ui.components.tiles
  (:require [cljs-time.core :as t]
            [clojure.string :as str]
            [district.format :as format]
            [district.time :as time]
            [district.ui.component.form.input :as inputs]
            [district.ui.component.tx-button :as tx-button]
            [district.ui.graphql.subs :as gql]
            [district.ui.now.subs]
            [district.ui.mobile.subs :as mobile-subs]
            [district.ui.router.events :as router-events]
            [district.ui.web3-accounts.subs :as accounts-subs]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [memefactory.shared.utils :as shared-utils]
            [memefactory.ui.contract.meme-auction :as meme-auction]
            [memefactory.ui.utils :as ui-utils :refer [format-price]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [clojure.set :as set]
            [memefactory.ui.components.general :refer [nav-anchor]]
            [cljs-web3.core :as web3]))

(defn meme-image [image-hash & [{:keys [rejected?] :as props}]]
  (let [props (dissoc props :rejected?)
        url (-> @(subscribe [::gql/query
                             {:queries [[:config
                                         [[:ipfs [:gateway]]]]]}])
                :config :ipfs :gateway)]
    [:div.meme-card
     props
     (if (and url (not-empty image-hash))
       [:img.meme-image.initial-fade-in-delay {:src (str (format/ensure-trailing-slash url) image-hash)}]
       [:div.meme-placeholder.initial-fade-in [:img {:src "/assets/icons/mememouth.png"}]])
     (when rejected?
       [:div.image-tape-container.initial-fade-in-delay
        [:div.image-tape
         [:span "Stank"]]])]))

(defn- event-target-classlist [event]
  (-> (aget event "target" "className")
      (str/split #"\ +")))      

(defn- event-target-has-class? [event class]
  (let [classlist (set (event-target-classlist event))]
    (.log js/console (clj->js classlist))
    (contains? classlist class)))

(defn- event-target-in-classlist? [event classlist]
  (some #(event-target-has-class? event %) classlist))

(defn flippable-tile [{:keys [:front :back :flippable-classes]}]
  (let [flipped? (r/atom false)
        flip #(swap! flipped? not)
        handle-click
        (if-not flippable-classes
          flip
          (fn [event]
            (when (event-target-in-classlist? event flippable-classes)
              (flip))))
        android-device? @(subscribe [::mobile-subs/android?])
        ios-device? @(subscribe [::mobile-subs/ios?])]
    (fn [{:keys [:front :back]}]
      [:div.flippable-tile.initial-fade-in-delay
       {:class [(when @flipped? "flipped") " "
                (when (or android-device? ios-device?) "mobile")]}
                    
       [:div.flippable-tile-front {:on-click handle-click} front]
       [:div.flippable-tile-back {:on-click handle-click} back]])))

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
    (fn [opts {:keys [:meme-auction/description] :as meme-auction}]
      (let [remaining (-> (time/time-remaining (t/date-time @now)
                                               end-time)
                          (dissoc :seconds))
            price (shared-utils/calculate-meme-auction-price (-> meme-auction
                                                                 (update :meme-auction/started-on #(.getTime (ui-utils/gql-date->date %))))
                                                             (.getTime @now))
            title (-> meme-auction :meme-auction/meme-token :meme-token/meme :meme/title)
            seller-address (:user/address (:meme-auction/seller meme-auction))
            meme-address (-> meme-auction :meme-auction/meme-token :meme-token/meme :reg-entry/address)]
        [:div.meme-card
         [:div.overlay
          [:div.logo
           [:img {:src "/assets/icons/mf-logo.svg"}]]

          [nav-anchor {:route :route.meme-detail/index
                       :params {:address meme-address}
                       :class "details-button"}
           [:span "View Details"]]
          [:ul.meme-data
           [:li [:label "Seller:"]
            [nav-anchor {:route :route.memefolio/index
                         :params {:address (:user/address (:meme-auction/seller meme-auction))}
                         :query {:tab :selling}
                         :title (str "Go to the Memefolio of " seller-address)}
             seller-address]]
           [:li [:label "Current Price:"] [:span (format-price price)]]
           [:li [:label "Start Price:"] [:span (format-price (:meme-auction/start-price meme-auction))]]
           [:li [:label "End Price:"] [:span (format-price (:meme-auction/end-price meme-auction))]]
           [:li [:label "End Price in:"] [:span (format/format-time-units remaining)]]]
          [:hr]
          [:div.description {:title description} description]
          [:div.input
           (if (= (-> meme-auction :meme-auction/seller :user/address)
                  @active-account)

             [inputs/pending-button {:pending? @cancel-tx-pending?
                                     :pending-text "Cancelling"
                                     :disabled (or @cancel-tx-pending? @cancel-tx-success? (not @active-account))
                                     :on-click (fn [e]
                                                 (.stopPropagation e)
                                                 (dispatch [::meme-auction/cancel {:send-tx/id tx-id
                                                                                   :meme-auction/address (:meme-auction/address meme-auction)
                                                                                   :meme/title title}]))}
              (if @cancel-tx-success? "Canceled" "Cancel Sell")]

             [inputs/pending-button {:pending? @buy-tx-pending?
                                     :pending-text "Buying"
                                     :disabled (or @buy-tx-pending? @buy-tx-success? (not @active-account))
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

         [nav-anchor {:route :route.meme-detail/index
                      :params {:address (-> meme-token :meme-token/meme :reg-entry/address)}}
          [:div.footer
           [:div.token-id (str "#"(-> meme-token :meme-token/meme :meme/number))]
           [:div.title (-> meme-token :meme-token/meme :meme/title)]
           [:div.number-minted (str (:meme-token/number meme-token)
                                    "/"
                                    (-> meme-token :meme-token/meme :meme/total-minted))]
           [:div.price (format-price price)]]]]))))

(defn meme-back-tile [{:keys [:reg-entry/created-on :meme/total-minted :meme/number :meme/total-trade-volume :meme/average-price :meme/highest-single-sale] :as meme}]
  (let [creator-address (-> meme :reg-entry/creator :user/address)
        meme-address (-> meme :reg-entry/address)]
    [:div.meme-card
     [:div.overlay
      [:div.logo
       [:img {:src "/assets/icons/mf-logo.svg"}]]
      [nav-anchor {:route :route.meme-detail/index
                   :params {:address meme-address}
                   :class "details-button"}
       [:span "View Details"]]
      [:ul.meme-data
       (when number
         [:li [:label "Registry Number:"]
          (str "#" number)])
       [:li [:label "Creator:"]
        [nav-anchor {:route :route.memefolio/index
                     :params {:address creator-address}
                     :query {:tab :created}
                     :title (str "Go to the Memefolio of " creator-address)}
         creator-address]]
       [:li [:label "Created:"]
        (let [formated-time (-> (time/time-remaining (t/date-time (ui-utils/gql-date->date created-on)) (t/now))
                                (dissoc :seconds)
                                format/format-time-units)]
          (if-not (empty? formated-time)
            (str formated-time " ago")
            "less than a minute ago"))]
       [:li [:label "Issued:"]
        (str total-minted " cards")]
       [:li [:label "Trade Volume:"] (ui-utils/format-price total-trade-volume)]
       [:li [:label "Average Price:"] (ui-utils/format-price average-price)]
       [:li [:label "Highest Single Sale:"] (ui-utils/format-price highest-single-sale)]]]]))

(defn meme-tile [{:keys [:reg-entry/address :meme/image-hash :meme/number] :as meme}]
  [:div.compact-tile
   [flippable-tile {:front [meme-image image-hash]
                    :back [meme-back-tile meme]}]
   [nav-anchor {:route :route.meme-detail/index
                :params {:address address}}
    [:div.footer
     [:div.title (str (when number (str "#" number " "))(-> meme :meme/title))]]]])
