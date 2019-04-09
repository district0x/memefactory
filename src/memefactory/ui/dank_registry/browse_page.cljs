(ns memefactory.ui.dank-registry.browse-page
  (:require [district.ui.component.page :refer [page]]
            [district.ui.graphql.subs :as gql]
            [district.ui.router.subs :as router-subs]
            [memefactory.shared.utils :as shared-utils]
            [memefactory.ui.components.app-layout :refer [app-layout]]
            [memefactory.ui.components.infinite-scroll :as infinite-scroll]
            [memefactory.ui.components.search :refer [search-tools]]
            [memefactory.ui.components.tiles :as tiles]
            [memefactory.ui.dank-registry.events :as mk-events]
            [memefactory.ui.components.spinner :as spinner]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [memefactory.ui.components.panels :refer [no-items-found]]))

(def page-size 12)

(defn build-tiles-query [{:keys [:search-term :order-by :search-tags :order-dir :only-cheapest?]} after]
  [:search-memes
   (cond-> {:first page-size
            :statuses [:reg-entry.status/whitelisted]}
     (not-empty search-term) (assoc :title search-term)
     (not-empty search-tags) (assoc :tags search-tags)
     after                   (assoc :after after)                    ;; TODO: fix this HACK!
     order-by                (assoc :order-by (keyword "memes.order-by" (case order-by
                                                                          "number-asc" "number"
                                                                          "number-desc" "number"
                                                                          order-by)))
     order-dir               (assoc :order-dir (keyword (case order-by
                                                          "number-asc"         :asc
                                                          "number-desc"        :desc
                                                          "total-trade-volume" :desc
                                                          "created-on"         :desc))))
   [:total-count
    :end-cursor
    :has-next-page
    [:items [:reg-entry/address
             :reg-entry/created-on
             :meme/image-hash
             :meme/title
             :meme/total-minted
             :meme/number
             [:reg-entry/creator [:user/address
                                  :user/creator-rank]]]]]])

(defn- puke [{:keys [:reg-entry/address] :as col}]
  [:pre {:style {:height "435px"
                 :text-align :center
                 :width "290px"
                 :margin :auto
                 :background-color "#ddd"
                 :border-bottom "1px solid #000"
                 :border-radius "1em"
                 :white-space :nowrap
                 :overflow :hidden
                 :text-overflow :ellipsis
                 }}
   address])

(defn memes-list [memes]
  [:div.tiles
   (for [{:keys [:reg-entry/address] :as meme} memes]
     ^{:key address} [puke meme])])

(defn dank-registry-tiles [form-data meme-search]
  (let [all-memes (->> @meme-search
                       (mapcat (fn [r] (-> r :search-memes :items))))
        last-meme (last @meme-search)
        no-memes? (empty? all-memes)
        loading? (:graphql/loading? last-meme)
        has-more? (-> last-meme :search-memes :has-next-page)]

    ;; (log/debug "#memes" {:c (count all-memes)})

    [:div.scroll-area
     ;;:div.tiles
     #_(if (and (empty? all-memes)
                (not (:graphql/loading? last-meme)))
         [no-items-found]
         (when-not (:graphql/loading? (first @meme-search))
           (doall
            (for [{:keys [:reg-entry/address] :as meme} all-memes]
              ^{:key address}
              #_[puke meme]
              [tiles/meme-tile meme]))))
     #_(when (:graphql/loading? last-meme)
         [:div.spinner-container [spinner/spin]])
     [infinite-scroll/infinite-scroll {:loading? loading?
                                       :has-more? has-more?
                                       :infinite-load-threshold 0
                                       :debounce-interval 200
                                       :load-fn #(let [{:keys [:end-cursor]} (:search-memes last-meme)]
                                                   (dispatch [:district.ui.graphql.events/query
                                                              {:query {:queries [(build-tiles-query @form-data end-cursor)]}
                                                               :id @form-data}]))}


      (when-not (:graphql/loading? (first @meme-search))
        (doall
         (for [{:keys [:reg-entry/address] :as meme} all-memes]
           ^{:key address}
           #_[puke meme]
           [tiles/meme-tile meme])))

      ]]))

(defmethod page :route.dank-registry/browse []
  (let [active-page (subscribe [::router-subs/active-page])
        form-data (let [{:keys [query]} @active-page]
                    (r/atom {:term ""
                             :order-by (or (:order-by query) "number-desc")
                             :order-dir (or (:order-dir query) "desc")}))
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
    (fn []
      (let [re-search (fn [& _]
                        (dispatch [:district.ui.graphql.events/query
                                   {:query {:queries [(build-tiles-query @form-data nil)]}}]))
            meme-search (subscribe [::gql/query {:queries [(build-tiles-query @form-data nil)]}
                                    {:id @form-data
                                     :disable-fetch? false}])
            search-total-count (-> @meme-search first :search-memes :total-count)]
        [app-layout
         {:meta {:title "MemeFactory - Dank Registry"
                 :description "Browse all memes ever minted. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.dank-registry-index-page
          [search-tools {:form-data form-data
                         :tags (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                         :search-id :search-term
                         :selected-tags-id :search-tags
                         :title "Dank registry"
                         :sub-title "Browse all memes ever minted"
                         :on-selected-tags-change re-search
                         ;; TODO: Fix this hack, we need a way of passing a select more info
                         :select-options (->> [{:key "number-desc"             :value "Newest"}
                                               {:key "total-trade-volume" :value "Total trade volume"}
                                               {:key "number-asc"             :value "Registry Number"}])
                         :on-search-change re-search
                         :on-check-filter-change re-search
                         :on-select-change re-search}]
          [dank-registry-tiles form-data meme-search]]]))))
