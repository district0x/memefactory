(ns memefactory.ui.leaderboard.dankest-page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [district.ui.component.form.input :refer [select-input]]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.components.search :refer [search-tools]]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.dank-registry.events :as mk-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [memefactory.ui.components.panels :refer [no-items-found]]))

(def page-size 6)

(defn build-tiles-query [after {:keys [order-by]}]
  [:search-memes
   (cond-> {:first page-size
            :order-by ({"total-trade-volume"  :memes.order-by/total-trade-volume
                        "average-price"       :memes.order-by/average-price
                        "highest-single-sale" :memes.order-by/highest-single-sale}
                       order-by)
            :statuses [:reg-entry.status/whitelisted]
            :order-dir :desc}
     after (assoc :after after))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:reg-entry/address
             [:reg-entry/creator [:user/address :user/creator-rank]]
             :meme/image-hash
             :reg-entry/created-on
             :meme/title
             :meme/number
             :meme/total-minted
             :meme/total-trade-volume
             :meme/average-price
             :meme/highest-single-sale]]]])

(defn dankest-memes-tiles [form-data]
  (let [meme-search (subscribe [::gql/query {:queries [(build-tiles-query nil @form-data)]}
                                {:id @form-data}])
        all-memes (->> @meme-search
                       (mapcat (fn [r] (-> r :search-memes :items))))
        last-meme (last @meme-search)
        loading? (:graphql/loading? last-meme)
        has-more? (-> (last @meme-search) :search-memes :has-next-page)]
    [:div.scroll-area
     (if (and (empty? all-memes)
              (not loading?))
       [no-items-found]
       [infinite-scroll {:class "tiles"
                         :loading? loading?
                         :has-more? has-more?
                         :loading-spinner-delegate (fn []
                                                     [:div.spinner-container [spinner/spin]])
                         :load-fn #(let [{:keys [:end-cursor] :as r} (:search-memes (last @meme-search))]
                                     (dispatch [:district.ui.graphql.events/query
                                                {:query {:queries [(build-tiles-query end-cursor @form-data)]}
                                                 :id @form-data}]))}
        (when-not (:graphql/loading? (first @meme-search))
          (doall
           (for [{:keys [:reg-entry/address] :as meme} all-memes]
             ^{:key address} [tiles/meme-tile meme])))])]))

(defmethod page :route.leaderboard/dankest []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (r/atom {:order-by "total-trade-volume"})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory - Dankest Memes"
               :description "Memes traded for the most ETH. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
       [:div.leaderboard-dankest-page
        [:section.dankest
         [:div.dankest-panel
          [:div.icon]
          [:h2.title "LEADERBOARDS - DANKEST"]
          [:h3.title "Memes traded for the most ETH"]
          [:div.order
           [select-input
            {:form-data form-data
             :id :order-by
             :options [{:key "average-price" :value "by average price"}
                       {:key "total-trade-volume" :value "by total volume"}
                       {:key "highest-single-sale" :value "by highest single sale"}]}]]
          [dankest-memes-tiles form-data]]]]])))
