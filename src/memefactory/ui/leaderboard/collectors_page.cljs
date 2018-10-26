(ns memefactory.ui.leaderboard.collectors-page
  (:require
   [district.format :as format]
   [district.ui.component.form.input :refer [select-input with-label]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [react-infinite]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   ))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 12)

(defn build-collectors-query [{:keys [order-by after]}]
  [:search-users
   (cond->
       {:first page-size
        :order-by order-by}
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
    [:div.user-tile
     [:div.number (str "#" num)]
     [:span.user-address address]
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
                                     {:id @form-data
                                      :disable-fetch? true}])
            totals (subscribe [::gql/query {:queries [[:overall-stats
                                                       [:total-memes-count
                                                        :total-tokens-count]]]}])]
        (if (:graphql/loading? @users-search)
          [:div "Loading ...."]
          (let [all-collectors (mapcat #(get-in % [:search-users :items]) @users-search)]

            (log/debug "All collectors" {:colectors all-collectors} :route.leaderboard/collectors)

            [app-layout
             {:meta {:title "MemeFactory"
                     :description "Description"}}
             [:div.leaderboard-collectors-page
              [:section.collectors
               [:div.collectors-panel
                [:div.icon]
                [:h2.title "LEADERBOARDS - COLLECTORS"]
                [:h3.title "lorem ipsum"]
                [:div.order
                 (let [total (get-in @users-search [:search-users :total-count])]
                   [select-input
                    {:form-data form-data
                     :id :order-by ;; TODO Do this !!!!!!!!!!
                     :options [{:key "curator-total-earned" :value (str "by total earnings: " total " total")}
                               {:key "challenger-total-earned" :value (str "by total challenges earnings: " total " total")}
                               {:key "voter-total-earned" :value (str "by total votes earnings: " total " total")}]}])]
                [:div.collectors
                 [react-infinite {:element-height 280
                                  :container-height 300
                                  :infinite-load-begin-edge-offset 100
                                  :use-window-as-scroll-container true
                                  :on-infinite-load (fn []
                                                      (when-not (:graphql/loading? @users-search)
                                                        (let [{:keys [has-next-page end-cursor]} (:search-users (last @users-search))]

                                                          (log/debug "Scrolled to load more" {:h has-next-page :e end-cursor} :route.leaderboard/collectors)

                                                          (when (or has-next-page (empty? all-collectors))
                                                            (re-search-users end-cursor)))))}
                  (if (:graphql/loading? @users-search)
                    [:div.loading]
                    (doall
                     (map
                      (fn [collector num]
                        ^{:key (:user/address collector)}
                        [collectors-tile collector (:overall-stats @totals) num])
                      all-collectors
                      (iterate inc 1))))]]]]]]))))))
