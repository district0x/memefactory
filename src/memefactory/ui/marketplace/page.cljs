(ns memefactory.ui.marketplace.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.marketplace.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [react-infinite]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.tiles :as tiles]
   [district.ui.router.subs :as router-subs]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.form.input :refer [select-input text-input]]
   [district.ui.component.form.chip-input :refer [chip-input]]
   [memefactory.ui.components.search :refer [search-tools]]))

(def react-infinite (r/adapt-react-class js/Infinite))

(defn build-tiles-query [{:keys [:search-term :order-by :order-dir]} after]
  [:search-meme-auctions
   (cond-> {:first 2}
     (not-empty search-term) (assoc :title search-term)
     after                   (assoc :after after)
     order-by                (assoc :order-by order-by)
     order-dir               (assoc :order-dir order-dir))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:meme-auction/address
             :meme-auction/start-price
             :meme-auction/end-price
             :meme-auction/duration
             :meme-auction/description
             [:meme-auction/seller [:user/address]]
             [:meme-auction/meme-token
              [:meme-token/number
               [:meme-token/meme
                [:meme/title
                 :meme/image-hash
                 :meme/total-minted]]]]]]]])

(defn marketplace-tiles [{:keys [:search-term :order-by :order-dir] :as params}]
  (let [auctions-search (subscribe [::gql/query {:queries [(look (build-tiles-query params nil))]}
                                    {:id :auctions-search}])]
    (fn [{:keys [:search-term :order-by :order-dir] :as params}]
      (let [all-auctions (->> @auctions-search
                              (mapcat (fn [r] (-> r :search-meme-auctions :items))))]
        (.log js/console "All auctions" (map :meme-auction/address all-auctions))
        [:div.tiles
         [react-infinite {:element-height 280
                          :container-height 300
                          :infinite-load-begin-edge-offset 100
                          :on-infinite-load (fn []
                                              (let [ {:keys [has-next-page end-cursor] :as r} (:search-meme-auctions (last @auctions-search))]
                                               (.log js/console "Scrolled to load more" has-next-page end-cursor)
                                               (when has-next-page
                                                 (dispatch [:district.ui.graphql.events/query
                                                            {:query {:queries [(build-tiles-query params end-cursor)]}}
                                                            {:id :auctions-search}]))))}
          (doall
           (for [{:keys [:meme-auction/address] :as auc} all-auctions]
             (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
               ^{:key address}
               [tiles/auction-tile {:on-buy-click #()} auc])))]]))))



(defmethod page :route.marketplace/index []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (let [{:keys [query]} @active-page]
                    (r/atom {:term ""
                             :order-by (if-let [o (:order-by query)]
                                         (keyword "meme-auctions.order-by" o)
                                         :meme-auctions.order-by/started-on)
                             :order-dir (or (keyword (:order-dir query)) :desc)}))
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}}
       [:div.marketplace
        [search-tools {:form-data form-data
                       :tags #_(look (->> @all-tags-subs :search-tags :items (mapv :tag/name)))
                             ["tag1" "tag2"]
                       :search-id :term  
                       :selected-tags-id :search-tags
                       :check-filter {:label "Show only cheapest offering of meme"
                                      :id :only-cheapest?}}]
        [marketplace-tiles {:search-term (:term @form-data)
                            :order-by (:order-by @form-data)
                            :order-dir (:order-dir @form-data)}]]]))) 
