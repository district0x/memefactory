(ns memefactory.ui.leaderboard.dankest
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.dank-registry.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [memefactory.shared.utils :as shared-utils]
   [district.ui.router.subs :as router-subs]
   [memefactory.ui.components.tiles :as tiles]
   [print.foo :refer [look] :include-macros true]
   [memefactory.ui.components.search :refer [search-tools]]))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 2)

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
             :meme/image-hash
             :meme/total-minted
             :meme/total-trade-volume]]]])

(defn dankest-memes-tiles []
  (let [meme-search (subscribe [::gql/query {:queries [(build-tiles-query nil)]}
                                {:id :dankest
                                 :disable-fetch? true}])
        all-memes (->> @meme-search
                       (mapcat (fn [r] (-> r :search-memes :items))))]
    (.log js/console "All memes " (map :reg-entry/address all-memes))
    [:div.tiles
     [react-infinite {:element-height 280
                      :container-height 300
                      :infinite-load-begin-edge-offset 100
                      :use-window-as-scroll-container true
                      :on-infinite-load (fn []
                                          (when-not (:graphql/loading? @meme-search)
                                            (let [ {:keys [has-next-page end-cursor] :as r} (:search-memes (last @meme-search))]
                                              (.log js/console "Scrolled to load more" has-next-page end-cursor)
                                              (when (or has-next-page (empty? all-memes))
                                                (dispatch [:district.ui.graphql.events/query
                                                           {:query {:queries [(build-tiles-query end-cursor)]}
                                                            :id :dankest}])))))}
      (doall
       (for [{:keys [:reg-entry/address] :as meme} all-memes]
         ^{:key address}
         [tiles/meme-tile {} meme]))]])) 

(defmethod page :route.leaderboard/dankest []
  (let [active-page (subscribe [::router-subs/active-page])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}}
       [:div.leaderboard.dankests
        [dankest-memes-tiles]]])))


