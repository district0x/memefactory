(ns memefactory.ui.leaderboard.curators-page
  (:require [district.ui.component.page :refer [page]]
            [memefactory.ui.components.app-layout :refer [app-layout]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [district.ui.graphql.subs :as gql]
            [goog.string :as gstring]
            [district.format :as format]
            [district.ui.component.form.input :refer [select-input]]))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 2)

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
         [:div.leaderboard-curators
          [:h1 "LEADERBOARDS - CURATORS"]
          [:p "lorem ipsum"]
          (let [total (get-in @search-users [:search-users :total-count])]
            [select-input
             {:form-data form-data
              :id :order-by
              :options [{:key "curator-total-earned" :value (str "by total earnings: " total " total")}
                        {:key "challenger-total-earned" :value (str "by total challenges earnings: " total " total")}
                        {:key "voter-total-earned" :value (str "by total votes earnings: " total " total")}]}])
          [:div.curators
           [react-infinite {:element-height 280
                            :container-height 300
                            :infinite-load-begin-edge-offset 100
                            :use-window-as-scroll-container true
                            :on-infinite-load (fn []
                                                (when-not (:graphql/loading? @search-users)
                                                  (let [{:keys [has-next-page end-cursor]} (:search-users (last @search-users))]
                                                    (.log js/console "Scrolled to load more" has-next-page end-cursor)
                                                    (when (or has-next-page (empty? lazy-curators))
                                                      (lazy-search-users end-cursor)))))}
            (doall
              (for [curator lazy-curators]
                ^{:key (:user/address curator)}
                [:div.curator
                 [:p "#" (case order-by
                           :users.order-by/curator-total-earned (:user/curator-rank curator)
                           :users.order-by/challenger-total-earned (:user/challenger-rank curator)
                           :users.order-by/voter-total-earned (:user/voter-rank curator))]
                 [:h3 (:user/address curator)]

                 [:p "CHALLENGES"]
                 [:p "Success rate: "
                  (let [total-challenges (:user/total-created-challenges curator)
                        success-challenges (:user/total-created-challenges curator)
                        ratio (-> (/ total-challenges success-challenges)
                                  (* 100))]
                    [:span total-challenges "/" success-challenges (gstring/format " (%d%)" ratio)])]
                 [:p "Earned: " (format/format-token (:user/challenger-total-earned curator) {:token "DANK"})]

                 [:p "VOTES"]
                 [:p "Success rate: "
                  (let [total-votes (:user/total-participated-votes curator)
                        success-votes (:user/total-participated-votes-success curator)
                        ratio (-> (/ total-votes success-votes)
                                  (* 100))]
                    [:span total-votes "/" success-votes (gstring/format " (%d%)" ratio)])]
                 [:p "Earned: " (format/format-token (:user/voter-total-earned curator) {:token "DANK"})]
                 [:p "Total Earnings: " (format/format-token (:user/curator-total-earned curator) {:token "DANK"})]]))]]]]))))
