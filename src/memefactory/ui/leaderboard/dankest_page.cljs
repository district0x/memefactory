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
   [taoensso.timbre :as log]))

(def page-size 12)

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
        last-meme (last @meme-search)]

    (if (and (empty? all-memes)
             (not (:graphql/loading? last-meme)))
      [:div.no-items-found "No items found."]
      [:div.scroll-area
       [:div.tiles
        (when-not (:graphql/loading? (first @meme-search))
          (doall
           (for [{:keys [:reg-entry/address] :as meme} all-memes]
             ^{:key address}
             [tiles/meme-tile meme])))
        (when (:graphql/loading? last-meme)
          [:div.spinner-container [spinner/spin]])]

       [infinite-scroll {:load-fn (fn []
                                    (when-not (:graphql/loading? @meme-search)
                                      (let [ {:keys [has-next-page end-cursor] :as r} (:search-memes (last @meme-search))]

                                        (log/debug "Scrolled to load more" {:h has-next-page :e end-cursor})

                                        (when has-next-page
                                          (dispatch [:district.ui.graphql.events/query
                                                     {:query {:queries [(build-tiles-query end-cursor @form-data)]}
                                                      :id :dankest}])))))}]])))

(defmethod page :route.leaderboard/dankest []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (r/atom {:order-by "average-price"})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}}
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
