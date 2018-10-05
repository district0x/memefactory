(ns memefactory.ui.dank-registry.browse-page
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

(def page-size 12)

(defn build-tiles-query [{:keys [:search-term :order-by :search-tags :order-dir :only-cheapest?]} after]
  [:search-memes
   (cond-> {:first page-size}
     (not-empty search-term) (assoc :title search-term)
     (not-empty search-tags) (assoc :tags search-tags)
     after                   (assoc :after after)
     order-by                (assoc :order-by (keyword "memes.order-by" order-by))
     order-dir               (assoc :order-dir (keyword order-dir)))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:reg-entry/address
             :meme/image-hash
             :meme/title
             [:reg-entry/creator [:user/address]]]]]])

(defn dank-registry-tiles [form-data]
  (let [meme-search (subscribe [::gql/query {:queries [(build-tiles-query @form-data nil)]}
                                {:id @form-data
                                 :disable-fetch? true}])
        all-memes (->> @meme-search
                       (mapcat (fn [r] (-> r :search-memes :items))))]
    (.log js/console "All memes " (map :reg-entry/address all-memes))
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
                                                          {:query {:queries [(build-tiles-query @form-data end-cursor)]}
                                                           :id @form-data}])))))}
     [:div.tiles
      (if (empty? all-memes)
        [:div.loading]
        (doall
         (for [{:keys [:reg-entry/address] :as meme} all-memes]
           ^{:key address}
           [tiles/meme-tile meme])))]]))

(defmethod page :route.dank-registry/browse []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (let [{:keys [query]} @active-page]
                    (r/atom {:term ""
                             :order-by (or (:order-by query) "created-on")
                             :order-dir (or (:order-dir query) "desc")}))
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
    (fn []
      (let [re-search (fn [& _]
                        (dispatch [:district.ui.graphql.events/query
                                   {:query {:queries [(build-tiles-query @form-data nil)]}}]))]
       [app-layout
        {:meta {:title "MemeFactory"
                :description "Description"}}
        [:div.dank-registry-index-page
         [search-tools {:form-data form-data
                        :tags (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                        :search-id :search-term
                        :selected-tags-id :search-tags
                        :title "Dank registry"
                        :sub-title "Sub title"
                        :on-selected-tags-change re-search
                        :select-options [{:key "number" :value "Number"}
                                         {:key "total-trade-volume" :value "Total trade volume"}]
                        :on-search-change re-search
                        :on-check-filter-change re-search
                        :on-select-change re-search}]
         [dank-registry-tiles form-data]]]))))
