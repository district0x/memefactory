(ns memefactory.ui.leaderboard.page
  (:require [district.ui.component.page :refer [page]]
            [memefactory.ui.components.app-layout :refer [app-layout]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [district.ui.graphql.subs :as gql]
            [goog.string :as gstring]
            [district.format :as format]
            [district.ui.component.form.input :refer [select-input]]))

(defn build-curators-query [order-by]
  [:search-users
   {:first 10
    :order-by order-by}
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
            search-users (subscribe [::gql/query {:queries [(build-curators-query order-by)]}])
            curators (get-in @search-users [:search-users :items])
            total (get-in @search-users [:search-users :total-count])]
        [app-layout
         {:meta {:title "MemeFactory"
                 :description "Description"}}
         [:div.leaderboard-curators
          [:h1 "LEADERBOARDS - CURATORS"]
          [:p "lorem ipsum"]
          [select-input
           {:form-data form-data
            :id :order-by
            :options [{:key "curator-total-earned" :value (str "by total earnings: " total " total")}
                      {:key "challenger-total-earned" :value (str "by total challenges earnings: " total " total")}
                      {:key "voter-total-earned" :value (str "by total votes earnings: " total " total")}]}]
          (into [:div.curators]
                (->> curators
                     (map (fn [curator]
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
                             [:p "Total Earnings: " (format/format-token (:user/curator-total-earned curator) {:token "DANK"})]]))))]]))))
