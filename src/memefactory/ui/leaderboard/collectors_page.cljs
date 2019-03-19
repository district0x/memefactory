(ns memefactory.ui.leaderboard.collectors-page
  (:require
   [district.format :as format]
   [district.ui.component.form.input :refer [select-input with-label]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.spinner :as spinner]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [district.ui.router.events :as router-events]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.ui.components.panels :refer [no-items-found]]))

(def page-size 12)

(defn build-collectors-query [{:keys [order-by after]}]
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
             :user/total-collected-memes
             :user/total-collected-token-ids
             [:user/largest-buy
              [:meme-auction/bought-for
               [:meme-auction/meme-token
                [:meme-token/number
                 [:meme-token/meme
                  [:meme/title]]]]]]]]]])

(defn collectors-tile [{:keys [:user/address :user/largest-sale
                               :user/total-collected-memes :user/total-collected-token-ids] :as collector}
                       {:keys [:total-memes-count :total-tokens-count]}
                       num]
  (let [meme (-> largest-sale
                 :meme-auction/meme-token
                 :meme-token/meme)]
    [:div.user-tile {:class (when (= @(subscribe [::accounts-subs/active-account]) address) "account-tile")}
     [:div.number (str "#" num)]
     [:span.user-address {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                {:address address}
                                                {:tab :collected}])}
      address]
     [:ul ;; TODO complete these after Matus comments
      [:li "Unique Memes: " [:span.unique (gstring/format "%d/%d (%d%%)"
                                                          total-collected-memes
                                                          total-memes-count
                                                          (/ (* 100 total-collected-memes)
                                                             total-memes-count))]]
      [:li "Total Cards: " [:span.total-cards (gstring/format "%d/%d"
                                                              total-collected-token-ids
                                                              total-tokens-count)]]
      (when (:meme/title meme)
        [:li "Largest Buy: " [:span.best-sale (gstring/format "%f ETH (#%d %s)"
                                                              (-> largest-sale :meme-auction/bought-for)
                                                              (-> largest-sale
                                                                  :meme-auction/meme-token
                                                                  :meme-token/number)
                                                              (:meme/title meme))]])]]))

(defmethod page :route.leaderboard/collectors []
  (let [form-data (r/atom {:order-by "total-collected-token-ids"})]
    (fn []
      (let [order-by (keyword "users.order-by" (:order-by @form-data))
            re-search-users (fn [after]
                              (dispatch [:district.ui.graphql.events/query
                                         {:query {:queries [(build-collectors-query {:order-by order-by
                                                                                     :after after})]}
                                          :id @form-data}]))
            users-search (subscribe [::gql/query {:queries [(build-collectors-query {:order-by order-by})]}
                                     {:id @form-data}])
            totals (subscribe [::gql/query {:queries [[:overall-stats
                                                       [:total-memes-count
                                                        :total-tokens-count]]]}])
            all-collectors (mapcat #(get-in % [:search-users :items]) @users-search)
            last-user (last @users-search)]

        (log/debug "All collectors" {:colectors all-collectors} :route.leaderboard/collectors)

        [app-layout
         {:meta {:title "MemeFactory"
                 :description "Description"}}
         [:div.leaderboard-collectors-page
          [:section.collectors
           [:div.collectors-panel
            [:div.icon]
            [:h2.title "LEADERBOARDS - COLLECTORS"]
            [:h3.title "Meme addicts with the biggest Memefolios"]
            [:div.order
             (let [total (get-in last-user [:search-users :total-count])]
               [select-input
                {:form-data form-data
                 :id :order-by ;; TODO Do this !!!!!!!!!!
                 :options [{:key "total-collected-memes" :value "by unique memes"}
                           {:key "total-collected-token-ids" :value "by total cards"}]}])]
            [:div.scroll-area
             [:div.collectors
              (if (and (empty? all-collectors)
                       (not (:graphql/loading? last-user)))
                [no-items-found]
                (when-not (:graphql/loading? (first @users-search))
                  (doall
                   (map
                    (fn [collector num]
                      ^{:key (:user/address collector)}
                      [collectors-tile collector (:overall-stats @totals) num])
                    all-collectors
                    (iterate inc 1)))))
              (when (:graphql/loading? last-user)
               [:div.spinner-container [spinner/spin]])]

             [infinite-scroll {:load-fn (fn []
                                          (when-not (:graphql/loading? last-user)
                                            (let [{:keys [has-next-page end-cursor]} (:search-users last-user)]

                                              (log/debug "Scrolled to load more" {:h has-next-page :e end-cursor} :route.leaderboard/collectors)

                                              (when has-next-page
                                                (re-search-users end-cursor)))))}]]]]]]))))
