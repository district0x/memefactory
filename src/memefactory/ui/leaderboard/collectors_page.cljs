(ns memefactory.ui.leaderboard.collectors-page
  (:require
   [district.ui.component.form.input :refer [select-input with-label]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.general :refer [nav-anchor]]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.panels :refer [no-items-found]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.components.ens-resolved-address :as ens-resolved-address]
   [memefactory.ui.utils :refer [format-price format-dank]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]))

(def page-size 6)

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
                  [:meme/title
                   :meme/number
                   :reg-entry/address]]]]]]]]]])


(defn collectors-tile [{:keys [:user/address :user/largest-buy :user/total-collected-memes :user/total-collected-token-ids] :as collector}
                       {:keys [:total-memes-count :total-tokens-count]}
                       num]
  (let [meme (-> largest-buy
                 :meme-auction/meme-token
                 :meme-token/meme)]
    [:div.user-tile {:class (when (= @(subscribe [::accounts-subs/active-account]) address) "account-tile")}
     [:div.number (str "#" num)]
     [nav-anchor {:route :route.memefolio/index
                  :params {:address address}
                  :query {:tab :collected}
                  :class "user-address"}
        [ens-resolved-address/ens-resolved-address {:resolvedOnly true :showBlockies false :presetValue address}]]
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
        [:li "Largest Buy: " [nav-anchor {:route :route.meme-detail/index
                                          :params {:address (:reg-entry/address meme)}
                                          :query nil}
                              [:span.best-sale (gstring/format "%f (#%d %s)"
                                                               (format-price (-> largest-buy :meme-auction/bought-for))
                                                               (:meme/number meme)
                                                               (:meme/title meme))]]])]]))


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
            last-user (last @users-search)
            loading? (:graphql/loading? last-user)
            has-more? (-> last-user :search-users :has-next-page)]

        (log/debug "#collectors" {:c (count all-collectors)})

        [app-layout
         {:meta {:title "MemeFactory - Collectors Leaderboard"
                 :description "Meme addicts with the biggest Memefolios. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
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
             (if (and (empty? all-collectors)
                      (not loading?))
               [no-items-found]
               [infinite-scroll {:class "collectors"
                                 :element-height 448
                                 :loading? loading?
                                 :has-more? has-more?
                                 :loading-spinner-delegate (fn []
                                                             [:div.spinner-container [spinner/spin]])
                                 :load-fn #(let [{:keys [:end-cursor]} (:search-users last-user)]
                                             (re-search-users end-cursor))}
                (when-not (:graphql/loading? (first @users-search))
                  (doall
                   (map
                    (fn [collector num]
                      ^{:key (:user/address collector)}
                      [collectors-tile collector (:overall-stats @totals) num])
                    all-collectors
                    (iterate inc 1))))])]]]]]))))
