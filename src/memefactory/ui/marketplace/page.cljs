(ns memefactory.ui.marketplace.page
  (:require
   [clojure.string :as str]
   [district.ui.component.form.input :refer [select-input text-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.panels :refer [no-items-found]]
   [memefactory.ui.components.search :refer [search-tools auctions-option-filters]]
   [memefactory.ui.components.search :refer [search-tools]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.marketplace.events :as mk-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log :refer [spy]]
   ))

(def page-size 6)

(defn build-tiles-query [{:keys [:search-term :search-tags :order-by :order-dir :option-filters]} after]
  [:search-meme-auctions
   (cond-> {:first page-size
            :statuses [:meme-auction.status/active]}
     (not-empty search-term)
     (assoc :title search-term)

     (not-empty search-tags)
     (assoc :tags search-tags)

     after
     (assoc :after after)

     order-by
     (assoc :order-by (keyword "meme-auctions.order-by" order-by))

     order-dir
     (assoc :order-dir (get {"started-on" :desc
                             "meme-total-minted" :asc
                             "price" :asc}
                            order-by
                            :desc))

     (#{:only-lowest-number :only-cheapest} option-filters)
     (assoc :group-by (get {:only-lowest-number :meme-auctions.group-by/lowest-card-number
                            :only-cheapest :meme-auctions.group-by/cheapest}
                           option-filters)))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:meme-auction/address
             :meme-auction/start-price
             :meme-auction/end-price
             :meme-auction/duration
             :meme-auction/description
             :meme-auction/started-on
             [:meme-auction/seller [:user/address]]
             [:meme-auction/meme-token
              [:meme-token/number
               :meme-token/token-id
               [:meme-token/meme
                [:meme/title
                 :reg-entry/address
                 :meme/image-hash
                 :meme/number
                 :meme/total-minted]]]]]]]])

(defn marketplace-tiles [form-data auctions-search]
  (let [all-auctions (->> @auctions-search
                          (mapcat (fn [r] (-> r :search-meme-auctions :items))))
        loading? (:graphql/loading? (last @auctions-search))
        has-more? (-> (spy (last @auctions-search)) :search-meme-auctions :has-next-page)]
    (log/debug "#auctions" {:auctions (count (map :meme-auction/address all-auctions))})
    [:div.scroll-area
     (if (and (empty? all-auctions)
              (not loading?))
       [no-items-found]
       [infinite-scroll {:class "tiles"
                         :fire-tutorial-next-on-items? true
                         :loading? loading?
                         :has-more? has-more?
                         :loading-spinner-delegate (fn []
                                                     [:div.spinner-container [spinner/spin]])
                         :load-fn #(let [{:keys [:end-cursor]} (:search-meme-auctions (last @auctions-search))]
                                     (dispatch [:district.ui.graphql.events/query
                                                {:query {:queries [(build-tiles-query @form-data end-cursor)]}
                                                 :id @form-data}]))}
        (doall
         (for [{:keys [:meme-auction/address] :as auc} all-auctions]
           ^{:key address} [tiles/auction-tile {:show-cards-left? (contains? #{:only-cheapest :only-lowest-number} (:option-filters @form-data))} auc]))])]))

(defn index-page []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (let [{:keys [query]} @active-page]
                    #_(log/debug "Starting with " (:term query))
                    (r/atom {:search-term (:term query)
                             :option-filters (if-let [opt (:option-filter query)]
                                               (keyword opt)
                                               :only-lowest-number)
                             :search-tags (when-let [tags (:search-tags query)]
                                            (str/split tags #","))
                             :order-by (or (:order-by query) "started-on")
                             :order-dir (or (:order-dir query) "desc")
                             :only-cheapest? true}))
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
    (fn []
      (let [re-search (fn [& _]
                        (dispatch [:district.ui.graphql.events/query
                                   {:query {:queries [(build-tiles-query @form-data nil)]}
                                    :id @form-data}]))
            auctions-search (subscribe [::gql/query {:queries [(build-tiles-query @form-data nil)]}
                                        {:id @form-data
                                         :disable-fetch? false}])
            search-total-count (-> @auctions-search first :search-meme-auctions :total-count)]
        [app-layout
         {:meta {:title "MemeFactory - Marketplace"
                 :description "Buy and Sell memes. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.marketplace-page
          [:section.marketplace
           [search-tools {:form-data form-data
                          :tags (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                          :search-id :search-term
                          :selected-tags-id :search-tags
                          :option-filters-id :option-filters
                          ;; :check-filters [{:label "Show only cheapest offering of meme"
                          ;;                  :id :only-cheapest?}]
                          :title "Marketplace"
                          :sub-title "Buy and Sell memes"
                          :select-options [{:key "started-on" :value "Newest"}
                                           {:key "meme-total-minted" :value "Rarest"}
                                           {:key "price" :value "Cheapest"}
                                           {:key "random" :value "Random"}]
                          :search-result-count search-total-count
                          :on-check-filter-change re-search
                          :option-filters auctions-option-filters
                          }]
           [:div.search-results
            [marketplace-tiles form-data auctions-search]]]]]))))

(defmethod page :route.marketplace/index []
  [index-page])
