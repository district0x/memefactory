(ns memefactory.ui.dank-registry.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.dank-registry.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.tiles :as tiles]
   [print.foo :refer [look] :include-macros true]))

(def react-infinite (r/adapt-react-class js/Infinite))

(defn search-tools []
  [:div.container
   [:div.left-section
    [:div.header
     [:img]
     [:h2 "Dank Registry"]
     [:h3 "Lorem ipsum dolor..."]]
    [:div.body
     [:input {:type :text}]
     [:select
      [:option {:value "Newest"} "Newest"]]
     [:ul.tags-list
      [:li "Some Tag"]
      [:li "Another Tag"]]]]
   [:div.right-section
    [:img]]])

(defn dank-registry-tiles [search-term]
  (let [build-query (fn [after]
                      [:search-memes
                       (cond-> {#_:title #_search-term :first 2}
                         after (assoc :after after))
                       [:total-count
                        :end-cursor
                        :has-next-page
                        [:items [:reg-entry/address
                                 :meme/image-hash]]]])
        meme-search (subscribe [::gql/query {:queries [(build-query nil)]}
                                    {:id :meme-search}])]
    (fn [search-term]
      (let [all-memes (->> @meme-search
                           (mapcat (fn [r] (-> r :search-memes :items))))]
        [:div.tiles
         [react-infinite {:element-height 280
                          :container-height 300
                          :infinite-load-begin-edge-offset 100
                          :on-infinite-load (fn []
                                              (let [ {:keys [has-next-page end-cursor] :as r} (:search-meme-auctions (last @meme-search))]
                                                (.log js/console "Scrolled to load more" has-next-page end-cursor)
                                                (when has-next-page
                                                  (dispatch [:district.ui.graphql.events/query
                                                             {:query {:queries [(build-query end-cursor)]}}
                                                             {:id :meme-search}]))))}
          (doall
           (for [{:keys [:reg-entry/address] :as meme} all-memes]
             ^{:key address}
             [tiles/meme-tile {:on-buy-click #()} meme]))]])))) 

(defmethod page :route.dank-registry/index []
  (let [search-atom (r/atom {:term ""})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.marketplace
        [search-tools] 
        [dank-registry-tiles (:term @search-atom)]]])))


