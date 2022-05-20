(ns memefactory.ui.leaderboard.dankest-page
  (:require
   [district.ui.component.form.input :refer [select-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.panels :refer [no-items-found]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.components.general :as gen-comps]
   [memefactory.ui.components.search :as search]
   [memefactory.ui.subs :as mf-subs]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]))

(def page-size 6)

(defn build-tiles-query [after {:keys [order-by :nsfw-switch]}]
  [:search-memes
   (cond-> {:first page-size
            :order-by ({"total-trade-volume"  :memes.order-by/total-trade-volume
                        "average-price"       :memes.order-by/average-price
                        "highest-single-sale" :memes.order-by/highest-single-sale}
                       order-by)
            :statuses [:reg-entry.status/whitelisted]
            :order-dir :desc}
     after       (assoc :after after)
     (not nsfw-switch) (assoc :tags-not [search/nsfw-tag]))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:reg-entry/address
             [:reg-entry/creator [:user/address :user/creator-rank]]
             :meme/image-hash
             :meme/animation-hash
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
  (let [form-data (r/atom {:order-by "total-trade-volume"
                           :nsfw-switch @(subscribe [::mf-subs/nsfw-switch])})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory - Dankest Memes"
               :description "Memes traded for the most MATIC. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
       [:div.leaderboard-dankest-page
        [:section.dankest
         [:div.dankest-panel
          [:div.icon]
          [:h2.title "LEADERBOARDS - DANKEST"]
          [:h3.title "Memes traded for the most MATIC"]
          [:div.order
           [select-input
            {:form-data form-data
             :id :order-by
             :options [{:key "average-price" :value "by average price"}
                       {:key "total-trade-volume" :value "by total volume"}
                       {:key "highest-single-sale" :value "by highest single sale"}]}]]
          [gen-comps/nsfw-switch form-data]
          [dankest-memes-tiles form-data]]]]])))
