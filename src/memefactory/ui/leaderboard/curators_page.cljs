(ns memefactory.ui.leaderboard.curators-page
  (:require
   [district.format :as format]
   [district.ui.component.form.input :refer [select-input]]
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
   [memefactory.ui.components.panels :refer [no-items-found]]
   [memefactory.ui.components.general :refer [dank-with-logo]]
   [memefactory.ui.components.general :refer [nav-anchor]]))

(def page-size 6)

(defn build-curators-query [{:keys [order-by after]}]
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
            users-search (subscribe [::gql/query {:queries [(build-curators-query {:order-by order-by})]}
                                     {:id @form-data}])
            lazy-curators (mapcat #(get-in % [:search-users :items]) @users-search)
            last-user (last @users-search)
            loading? (:graphql/loading? last-user)
            has-more? (-> last-user :search-users :has-next-page)]
        [app-layout
         {:meta {:title "MemeFactory - Curators Leaderboard"
                 :description "The most prolific challengers and voters on MemeFactory. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.leaderboard-curators-page
          [:section.curators
           [:div.curators-panel
            [:div.icon]
            [:h2.title "LEADERBOARDS - CURATORS"]
            [:h3.title "The most prolific challengers and voters on Meme Factory"]
            [:div.order
             (let [total (get-in last-user [:search-users :total-count])]
               [select-input
                {:form-data form-data
                 :id :order-by
                 :options [{:key "curator-total-earned" :value "by total earnings"}
                           {:key "challenger-total-earned" :value "by total challenges earnings"}
                           {:key "voter-total-earned" :value "by total votes earnings"}]}])]
            [:div.scroll-area
             [:div.curators
              (if (and (empty? lazy-curators)
                       (not loading?))
                [no-items-found]
                (when-not (:graphql/loading? (first @users-search))
                  (->> lazy-curators
                       (map-indexed
                        (fn [idx curator]
                          ^{:key (:user/address curator)}
                          [:div.curator {:class (when (= @(subscribe [::accounts-subs/active-account]) (:user/address curator)) "account-tile")}
                           [:p.number (str "#" (inc idx))]
                           [nav-anchor {:route :route.memefolio/index
                                        :params {:address (:user/address curator)}
                                        :query {:tab :curated}}
                            [:h3.address
                             (:user/address curator)]]

                           [:h4.challenges "CHALLENGES"]
                           [:p "Success rate: "
                            (let [total-challenges (:user/total-created-challenges curator)
                                  success-challenges (:user/total-created-challenges-success curator)]
                              [:span success-challenges "/" total-challenges " (" (format/format-percentage success-challenges total-challenges) ")"])]
                           [:p "Earned: " [:span (format/format-token (/ (:user/challenger-total-earned curator) 1e18) {:token "DANK"})]]

                           [:h4.votes "VOTES"]
                           [:p "Success rate: "
                            (let [total-votes (:user/total-participated-votes curator)
                                  success-votes (:user/total-participated-votes-success curator)]
                              [:span success-votes "/" total-votes " (" (format/format-percentage success-votes total-votes) ")"])]

                           [:p "Earned: " [:span (format/format-token (/ (:user/voter-total-earned curator) 1e18) {:token "DANK"})]]
                           [:p.total-earnings "Total Earnings: " [:span (format/format-token (/ (:user/curator-total-earned curator) 1e18) {:token "DANK"})]]]))
                       doall)))
              (when loading?
               [:div.spinner-container [spinner/spin]])]
             [infinite-scroll {:loading? loading?
                               :has-more? has-more?
                               :infinite-load-threshold 0
                               :debounce-interval 200
                               :load-fn #(let [{:keys [:end-cursor]} (:search-users last-user)]
                                           (lazy-search-users end-cursor))}]]]]]]))))
