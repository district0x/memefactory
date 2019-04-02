(ns memefactory.ui.dank-registry.browse-page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.infinite-scroll :as infinite-scroll]
   [memefactory.ui.components.search :refer [search-tools]]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.dank-registry.events :as mk-events]
   [memefactory.ui.components.spinner :as spinner]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [memefactory.ui.components.panels :refer [no-items-found]]))

(def page-size 3 #_12)

;; (def state (atom []))

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


;;;;;;;;;;;;;;;;;;;;;;

(def scroll-interval 3)

(def elem-height 400)

(defn build-elems [from to]
  (log/debug "@build-elems" {:f from :t to})
  (doall
   (for [i (range from to)]
     [:div {:key i
            :style {:height (str elem-height "px")
                    :text-align :center
                    :width "120px"
                    :margin :auto
                    :background-color "#ddd"
                    :border-bottom "1px solid #000"}}
      (str "ELEM " i)])))

(defn infinite-list []
  (let [state (r/atom nil)
        update-state (fn [{:keys [:from :to]}]
                       (log/debug "@update-state" {:f from :t to})
                       (swap! state (fn [old new] (into old (concat new)))
                              (build-elems from to))
                       )]

    (fn []

      (log/debug "#state" {:c (-> @state count)})

      [infinite-scroll/infinite-scroll {:load-fn (fn []

                                                   (let [from (-> @state count)
                                                         to (+ scroll-interval from)]

                                                     (log/debug "@on-load" {:f from :t to})

                                                     (update-state {:from from :to to}))

                                                   )}
       (reverse @state)])

    ))

;;;;;;;;;;;;;;;;;;;;;;

(defn dank-registry-tiles [form-data meme-search]
  (let [all-memes (->> @meme-search
                       (mapcat (fn [r] (-> r :search-memes :items))))
        last-meme (last @meme-search)



        ]

    [infinite-list]

    #_[:div.scroll-area
       [:div.tiles
        (if (and (empty? all-memes)
                 (not (:graphql/loading? last-meme)))
          [no-items-found]
          (when-not (:graphql/loading? (first @meme-search))
            (doall
             (for [{:keys [:reg-entry/address] :as meme} all-memes]
               ^{:key address}
               [tiles/meme-tile meme]))))
        (when (:graphql/loading? last-meme)
          [:div.spinner-container [spinner/spin]])]
       [infinite-scroll {:load-fn (fn []
                                    (when-not (:graphql/loading? last-meme)
                                      (let [ {:keys [has-next-page end-cursor] :as r} (:search-memes last-meme)]
                                        (when has-next-page
                                          (dispatch [:district.ui.graphql.events/query
                                                     {:query {:queries [(build-tiles-query @form-data end-cursor)]}
                                                      :id @form-data}])))))}]]))



(defmethod page :route.dank-registry/browse []

  ;; [infinite-list]

  (let [active-page (subscribe [::router-subs/active-page])
        form-data (let [{:keys [query]} @active-page]
                    (r/atom {:term ""
                             :order-by (or (:order-by query) "number-desc")
                             :order-dir (or (:order-dir query) "desc")}))
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
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
        [dank-registry-tiles form-data meme-search]]])))
