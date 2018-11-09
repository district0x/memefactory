(ns memefactory.ui.marketplace.page
  (:require
   [district.ui.component.form.input :refer [select-input text-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.search :refer [search-tools]]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.marketplace.events :as mk-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [react-infinite]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   ))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 12)

(defn build-tiles-query [{:keys [:search-term :search-tags :order-by :order-dir :only-cheapest?]} after]
  [:search-meme-auctions
   (cond-> {:first page-size}
     (not-empty search-term) (assoc :title search-term)
     (not-empty search-tags) (assoc :tags search-tags)
     after                   (assoc :after after)
     order-by                (assoc :order-by (keyword "meme-auctions.order-by" order-by))
     order-dir               (assoc :order-dir (keyword order-dir))
     only-cheapest?          (assoc :group-by :meme-auctions.group-by/cheapest))
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
               :meme-token/token-id
               [:meme-token/meme
                [:meme/title
                 :reg-entry/address
                 :meme/image-hash
                 :meme/total-minted]]]]]]]])

(defn marketplace-tiles [form-data auctions-search]
  (let [all-auctions (->> @auctions-search
                          (mapcat (fn [r] (-> r :search-meme-auctions :items))))]

    (log/debug "All auctions" {:auctions (map :meme-auction/address all-auctions)})

    [react-infinite {:element-height 280
                     :container-height 300
                     :infinite-load-begin-edge-offset 100
                     :use-window-as-scroll-container true
                     :on-infinite-load (fn []
                                         (when-not (:graphql/loading? @auctions-search)
                                           (let [ {:keys [has-next-page end-cursor] :as r} (:search-meme-auctions (last @auctions-search))]

                                             (log/debug "Scrolled to load more" {:h has-next-page :e end-cursor})

                                             (when (or has-next-page (empty? all-auctions))
                                               (dispatch [:district.ui.graphql.events/query
                                                          {:query {:queries [(build-tiles-query @form-data end-cursor)]}
                                                           :id @form-data}])))))}
     [:div.tiles
      (if (:graphql/loading? @auctions-search)
        [:div.loading]
        (doall
         (for [{:keys [:meme-auction/address] :as auc} all-auctions]
           ^{:key address}
           [tiles/auction-tile {} auc])))]]))

(defn index-page []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (let [{:keys [query]} @active-page]
                    (r/atom {:search-term (:term query)
                             :search-tags (:search-tags query)
                             :order-by (or (:order-by query) "started-on")
                             :order-dir (or (:order-dir query) "desc")}))
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
    (fn []
      (let [re-search (fn [& _]
                        (dispatch [:district.ui.graphql.events/query
                                   {:query {:queries [(build-tiles-query @form-data nil)]}}]))
            auctions-search (subscribe [::gql/query {:queries [(build-tiles-query @form-data nil)]}
                                    {:id @form-data
                                     :disable-fetch? true}])
            search-total-count (-> @auctions-search first :search-meme-auctions :total-count)]
        [app-layout
         {:meta {:title "MemeFactory"
                 :description "Description"}}
         [:div.marketplace-page
          [:section.marketplace
           [search-tools {:form-data form-data
                          :tags (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                          :search-id :search-term
                          :selected-tags-id :search-tags
                          :check-filters [{:label "Show only cheapest offering of meme"
                                           :id :only-cheapest?}]
                          :title "Marketplace"
                          :sub-title "Sub title"
                          :on-selected-tags-change re-search
                          :select-options (->> [{:key "started-on" :value "Newest"}
                                                {:key "meme-total-minted" :value "Rarest"}
                                                {:key "price" :value "Cheapest"}
                                                {:key "random" :value "Random"}]
                                               (map (fn [opt] (update opt :value str " - Total : " search-total-count))))
                          :on-search-change re-search
                          :on-check-filter-change re-search
                          :on-select-change re-search}]
           [:div.search-results
            [marketplace-tiles form-data auctions-search]]]]]))))

(defmethod page :route.marketplace/index []
  [index-page])
