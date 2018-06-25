(ns memefactory.ui.dank-registry.page
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

(defn build-tiles-query [{:keys [:search-term :order-by :order-dir :only-cheapest?]} after]
  [:search-memes
   (cond-> {:first 2}
     (not-empty search-term) (assoc :title search-term)
     after                   (assoc :after after)
     order-by                (assoc :order-by order-by)
     order-dir               (assoc :order-dir order-dir))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:reg-entry/address
             :meme/image-hash]]]])

(defn dank-registry-tiles [form-data]
  (let [meme-search (subscribe [::gql/query {:queries [(build-tiles-query @form-data nil)]}
                                {:id :meme-search}])
        all-memes (->> @meme-search
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
                                                         {:query {:queries [(build-tiles-query @form-data end-cursor)]}}
                                                         {:id :meme-search}]))))}
      (doall
       (for [{:keys [:reg-entry/address] :as meme} all-memes]
         ^{:key address}
         [tiles/meme-tile {:on-buy-click #()} meme]))]])) 

(defmethod page :route.dank-registry/index []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (let [{:keys [query]} @active-page]
                    (r/atom {:term ""
                             :order-by (if-let [o (:order-by query)]
                                         (keyword "memes.order-by" o)
                                         :memes.order-by/created-on)
                             :order-dir (or (keyword (:order-dir query)) :desc)}))
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}}
       [:div.dank-registry.index
        [search-tools {:form-data form-data
                       :tags (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                       :search-id :term  
                       :selected-tags-id :search-tags}] 
        [dank-registry-tiles form-data]]])))


