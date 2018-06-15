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
   [memefactory.ui.components.tiles :as tiles]))

(def react-infinite (r/adapt-react-class js/Infinite))

(defn search-tools []
  [:div.container
   [:div.left-section
    [:div.header
     [:img]
     [:h2 "Marketplace"]
     [:h3 "Lorem ipsum dolor..."]]
    [:div.body
     [:input {:type :text}]
     [:select
      [:option {:value "Cheapest"} "Cheapest"]]
     [:ul.tags-list
      [:li "Some Tag"]
      [:li "Another Tag"]]
     [:label "Show only cheapest offering of meme"]
     [:input {:type :checkbox}]]]
   [:div.right-section
    [:img]]])

(defn marketplace-tiles [search-term]
  (let [build-query (fn [after]
                      [:search-meme-auctions
                       (cond-> {#_:title #_search-term :first 2}
                         after (assoc :after after))
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
        auctions-search (subscribe [::gql/query {:queries [(build-query nil)]}
                                    {:id :auctions-search}])]
    (fn [search-term]
      (let [all-auctions (->> @auctions-search
                              (mapcat (fn [r] (-> r :search-meme-auctions :items))))]
        (.log js/console "All auctions" all-auctions)
        [:div.tiles
         [react-infinite {:element-height 280
                          :container-height 300
                          :infinite-load-begin-edge-offset 100
                          :on-infinite-load (fn []
                                              (let [ {:keys [has-next-page end-cursor] :as r} (:search-meme-auctions (last @auctions-search))]
                                               (.log js/console "Scrolled to load more" has-next-page end-cursor)
                                               (when has-next-page
                                                 (dispatch [:district.ui.graphql.events/query
                                                            {:query {:queries [(build-query end-cursor)]}}
                                                            {:id :auctions-search}]))))}
          (doall
           (for [{:keys [:meme-auction/address] :as auc} all-auctions]
             (let [title (-> auc :meme-auction/meme-token :meme-token/meme :meme/title)]
               ^{:key address}
               [tiles/auction-tile {:on-buy-click #()} auc])))]]))))

(defmethod page :route.marketplace/index []
  (let [search-atom (r/atom {:term ""})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.marketplace
        [search-tools] 
        [:div.auctions.container
         [:h2.title "New On Marketplace"]
         [:h3.title "Lorem ipsum ..."]
         [marketplace-tiles (:term @search-atom)]]]])))


