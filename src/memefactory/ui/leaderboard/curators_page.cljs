(ns memefactory.ui.leaderboard.curators-page
  (:require
   [district.format :as format]
   [district.ui.component.form.input :refer [select-input]]
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

(defn build-curators-query [{:keys [order-by after]}]
  [:search-users
   (cond->
     {:first page-size
      :order-by order-by}
     after (assoc :after after))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:user/address
             :user/total-created-challenges
             :user/total-created-challenges-success
             :user/challenger-total-earned
             :user/challenger-rank
             :user/total-participated-votes
             :user/total-participated-votes-success
             :user/voter-total-earned
             :user/voter-rank
             :user/curator-total-earned
             :user/curator-rank]]]])

(defmethod page :route.leaderboard/curators []
  (let [form-data (r/atom {:order-by "curator-total-earned"})]
    (fn []
      (let [order-by (keyword "users.order-by" (:order-by @form-data))
            lazy-search-users (fn [after]
                                (dispatch [:district.ui.graphql.events/query
                                           {:query {:queries [(build-curators-query {:order-by order-by
                                                                                     :after after})]}
                                            :id @form-data}]))
            search-users (subscribe [::gql/query {:queries [(build-curators-query {:order-by order-by})]}
                                     {:id @form-data
                                      :disable-fetch? true}])
            lazy-curators (mapcat #(get-in % [:search-users :items]) @search-users)]
        [app-layout
         {:meta {:title "MemeFactory"
                 :description "Description"}}
         [:div.leaderboard-curators-page
          [:section.curators
           [:div.curators-panel
            [:div.icon]
            [:h2.title "LEADERBOARDS - CURATORS"]
            [:h3.title "lorem ipsum"]
            [:div.order
             (let [total (get-in @search-users [:search-users :total-count])]
               [select-input
                {:form-data form-data
                 :id :order-by
                 :options [{:key "curator-total-earned" :value (str "by total earnings: " total " total")}
                           {:key "challenger-total-earned" :value (str "by total challenges earnings: " total " total")}
                           {:key "voter-total-earned" :value (str "by total votes earnings: " total " total")}]}])]
            [:div.curators
             [react-infinite {:element-height 280
                              :container-height 300
                              :infinite-load-begin-edge-offset 100
                              :use-window-as-scroll-container true
                              :on-infinite-load (fn []
                                                  (when-not (:graphql/loading? @search-users)
                                                    (let [{:keys [has-next-page end-cursor]} (:search-users (last @search-users))]

                                                      (log/debug "Scrolled to load more" {:h has-next-page :e end-cursor} :route.leaderboard/curators)

                                                      (when (or has-next-page (empty? lazy-curators))
                                                        (lazy-search-users end-cursor)))))}
              (doall
               (for [curator lazy-curators]
                 ^{:key (:user/address curator)}
                 [:div.curator
                  [:p.number "#" (case order-by
                                   :users.order-by/curator-total-earned (:user/curator-rank curator)
                                   :users.order-by/challenger-total-earned (:user/challenger-rank curator)
                                   :users.order-by/voter-total-earned (:user/voter-rank curator))]
                  [:h3.address (:user/address curator)]

                  [:h4.challenges "CHALLENGES"]
                  [:p "Success rate: "
                   (let [total-challenges (:user/total-created-challenges curator)
                         success-challenges (:user/total-created-challenges curator)]
                     [:span total-challenges "/" success-challenges " " (format/format-percentage total-challenges success-challenges)])]
                  [:p "Earned: " (format/format-token (/ (:user/challenger-total-earned curator) 1e18) {:token "DANK"})]

                  [:h4.votes "VOTES"]
                  [:p "Success rate: "
                   (let [total-votes (:user/total-participated-votes curator)
                         success-votes (:user/total-participated-votes-success curator)]
                     [:span total-votes "/" success-votes " " (format/format-percentage total-votes success-votes)])]
                  [:p "Earned: " [:span (format/format-token (/ (:user/voter-total-earned curator) 1e18) {:token "DANK"})]]
                  [:p "Total Earnings: " [:span (format/format-token (/ (:user/curator-total-earned curator) 1e18) {:token "DANK"})]]]))]]]]]]))))
