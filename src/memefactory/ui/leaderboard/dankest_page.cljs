(ns memefactory.ui.leaderboard.dankest-page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.search :refer [search-tools]]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.dank-registry.events :as mk-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   ))

(def page-size 12)

(defn build-tiles-query [after]
  [:search-memes
   (cond-> {:first page-size
            :order-by :memes.order-by/total-trade-volume
            :order-dir :desc}
     after (assoc :after after))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:reg-entry/address
             [:reg-entry/creator [:user/address]]
             :meme/image-hash
             :meme/title
             :meme/total-minted
             :meme/total-trade-volume]]]])

(defn dankest-memes-tiles []
  (let [meme-search (subscribe [::gql/query {:queries [(build-tiles-query nil)]}
                                {:id :dankest
                                 :disable-fetch? true}])
        all-memes (->> @meme-search
                       (mapcat (fn [r] (-> r :search-memes :items))))]

    (log/debug "All memes" {:memes (map :reg-entry/address all-memes)} ::dankest-memes-tiles)

    [:div.scroll-area
     [:div.tiles
      (if (:graphql/loading? @meme-search)
        [:div.loading]
        (doall
         (for [{:keys [:reg-entry/address] :as meme} all-memes]
           ^{:key address}
           [tiles/meme-tile meme])))]
     [infinite-scroll {:load-fn (fn []
                                  (when-not (:graphql/loading? @meme-search)
                                    (let [ {:keys [has-next-page end-cursor] :as r} (:search-memes (last @meme-search))]

                                      (log/debug "Scrolled to load more" {:h has-next-page :e end-cursor})

                                      (when (or has-next-page (empty? all-memes))
                                        (dispatch [:district.ui.graphql.events/query
                                                   {:query {:queries [(build-tiles-query end-cursor)]}
                                                    :id :dankest}])))))}]]))

(defmethod page :route.leaderboard/dankest []
  (let [active-page (subscribe [::router-subs/active-page])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}}
       [:div.leaderboard-dankest-page
        [:section.dankest
         [:div.dankest-panel
          [:div.icon]
          [:h2.title "LEADERBOARDS - DANKEST"]
          [:h3.title "lorem ipsum"]
          [dankest-memes-tiles]]]]])))
