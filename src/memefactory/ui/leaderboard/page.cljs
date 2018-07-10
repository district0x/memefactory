(ns memefactory.ui.leaderboard.page
  (:require [district.ui.component.page :refer [page]]
            [memefactory.ui.components.app-layout :refer [app-layout]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [district.ui.graphql.subs :as gql]))

(defn build-curators-query []
  [:search-users
   {:first 10
    :order-by :users.order-by/curator-total-earned}
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
  (let [sort (r/atom :earnings)
        curators (subscribe [::gql/query {:queries [(build-curators-query)]}])
        ]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}}
       [:div.leaderboard-curators
        [:h1 "LEADERBOARDS - CURATORS"]
        [:p "lorem ipsum"]
        [:select
         [:option {:value :earnings} "by total earnings: 145 total"]
         [:option {:value :???} "???"]]
        [:p (pr-str @curators)]
        #_(into [:div.curators]
              (map-indexed (fn [idx curator]
                             [:div.curator
                              [:p "#" (inc idx)]
                              [:h3 (:hash curator)]

                              [:p "CHALLENGES"]
                              [:p "Success rate: " [:span "20/22 (89%)"]]
                              [:p "Earned :" [:span "123,222.2 DANK"]]

                              [:p "VOTES"]
                              [:p "Success rate: " [:span "20/22 (89%)"]]
                              [:p "Earned: " [:span "123,222.2 DANK"]]
                              [:p "Total Earnings: " [:span "136,222.4 DANK"]]]) @curators))]])))
