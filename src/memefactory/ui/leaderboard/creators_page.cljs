(ns memefactory.ui.leaderboard.creators-page
  (:require
   [district.format :as format]
   [district.ui.component.form.input :refer [select-input with-label]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.utils :as ui-utils :refer [format-price format-dank]]
   [memefactory.ui.components.spinner :as spinner]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [district.ui.router.events :as router-events]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.ui.components.panels :refer [no-items-found]]))

(def page-size 12)

(defn build-creators-query [{:keys [order-by after]}]
  [:search-users
   (cond->
       {:first page-size
        :order-by order-by
        :order-dir :desc}
     after (assoc :after after))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:user/address
             :user/creator-total-earned
             :user/total-created-memes
             :user/total-created-memes-whitelisted
             [:user/largest-sale
              [:meme-auction/bought-for
               [:meme-auction/meme-token
                [:meme-token/number
                 [:meme-token/meme
                  [:meme/title
                   :reg-entry/address]]]]]]]]]])

(defn creator-tile [{:keys [:user/address :user/creator-total-earned :user/total-created-memes
                            :user/total-created-memes-whitelisted :user/largest-sale] :as creator}
                    num]
  (let [meme (-> largest-sale
                 :meme-auction/meme-token
                 :meme-token/meme)]
    [:div.user-tile {:class (when (= @(subscribe [::accounts-subs/active-account]) address) "account-tile")}
     [:div.number (str "#" num)]
     [:span.user-address {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                {:address address}
                                                {:tab :created}])}
      address]
     [:ul
      [:li "Earned: " [:span.earned (format-price creator-total-earned)]]
      [:li "Success Rate: " [:span.success-rate (gstring/format "%d/%d (%d%%)"
                                                                total-created-memes-whitelisted
                                                                total-created-memes
                                                                (if (pos? total-created-memes)
                                                                    (/ (* 100 total-created-memes-whitelisted)
                                                                       total-created-memes)
                                                                    0))]]
      (when (:meme/title meme)
        [:li "Best single card sale: " [:span.best-sale {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                                                               {:address (:reg-entry/address meme)}
                                                                               nil])}
                                        (gstring/format "%.2f ETH (#%d %s)"
                                                        (-> largest-sale :meme-auction/bought-for (/ 1e18))
                                                        (-> largest-sale
                                                            :meme-auction/meme-token
                                                            :meme-token/number)
                                                        (:meme/title meme))]])]]))

(defmethod page :route.leaderboard/creators []
  (let [form-data (r/atom {:order-by "total-earned"})]
    (fn []
      (let [order-by (keyword "users.order-by" (:order-by @form-data))
            re-search-users (fn [after]
                              (dispatch [:district.ui.graphql.events/query
                                         {:query {:queries [(build-creators-query {:order-by order-by
                                                                                   :after after})]}
                                          :id @form-data}]))
            users-search (subscribe [::gql/query {:queries [(build-creators-query {:order-by order-by})]}
                                     {:id @form-data}])
            all-creators (mapcat #(get-in % [:search-users :items]) @users-search)
            last-user (last @users-search)]

        (log/debug "All creators" {:creators all-creators})

        [app-layout
         {:meta {:title "MemeFactory - Creators Leaderboard"
                 :description "Meme makers ranked by registry success rate and total ETH earned. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.leaderboard-creators-page
          [:section.creators
           [:div.creators-panel
            [:div.icon]
            [:h2.title "LEADERBOARDS - CREATORS"]
            [:h3.title "Meme makers ranked by registry success rate and total ETH earned"]
            [:div.order
             (let [total (get-in last-user [:search-users :total-count])]
               [select-input
                {:form-data form-data
                 :id :order-by
                 :options [{:key "total-earned" :value "by earnings"}
                           {:key "best-single-card-sale" :value "by single card sale"}
                           {:key "total-created-memes-whitelisted" :value "by created memes"}]}])]
            [:div.scroll-area
             [:div.creators
              (if (and (empty? all-creators)
                       (not (:graphql/loading? last-user)))
                [no-items-found]
                (when-not (:graphql/loading? (first @users-search))
                  (doall
                   (map
                    (fn [creator num]
                      ^{:key (:user/address creator)}
                      [creator-tile creator num])
                    all-creators
                    (iterate inc 1)))))
              (when (:graphql/loading? last-user)
               [:div.spinner-container [spinner/spin]])]
             [infinite-scroll {:load-fn (fn []
                                          (when-not (:graphql/loading? last-user)
                                            (let [{:keys [has-next-page end-cursor]} (:search-users last-user)]

                                              (log/debug "Scrolled to load more" {:h has-next-page :e end-cursor})

                                              (when has-next-page
                                                (re-search-users end-cursor)))))}]]]]]]))))
