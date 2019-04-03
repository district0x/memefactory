(ns memefactory.ui.dank-registry.browse-page
  (:require
   [ajax.core :as ajax :refer [POST]]
   [district.ui.graphql.utils :as graphql-ui-utils]
   [district.graphql-utils :as graphql-utils]

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
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log :refer [spy]]
   [memefactory.ui.components.panels :refer [no-items-found]]))

(def page-size 3 #_12)

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

;; (def scroll-interval 3)

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

(defn puke [col]
  [:pre {:style {:height (str elem-height "px")
                 :text-align :center
                 :width "120px"
                 :margin :auto
                 :background-color "#ddd"
                 :border-bottom "1px solid #000"}}
   (with-out-str (cljs.pprint/pprint col))])

;;;;;;;;;;;;;;;;;;;;;;

(defn dank-registry-tiles [form-data meme-search]
  (let [

        ;; all-memes (->> (spy @meme-search)
        ;;                (mapcat (fn [r] (-> r :search-memes :items))))

        ;; last-meme (last @meme-search)

        state (r/atom nil)
        loading? (r/atom false)

        update-state (fn [{:keys [:from :to]}]

                       (log/debug "@update-state" {
                                                   ;; :q (graphql-ui-utils/parse-query {:queries [(build-tiles-query @form-data to)]}
                                                   ;;                                  {:kw->gql-name graphql-utils/kw->gql-name})
                                                   :f from
                                                   :t to})

                       #_(swap! state (fn [old new] (into old (concat new)))
                              (build-elems from to))

                       (reset! loading? true)

                       (POST "http://qa.district0x.io:6300/graphql"
                           :params {:query (:query-str (graphql-ui-utils/parse-query {:queries [(build-tiles-query @form-data (str to))]}
                                                                                     {:kw->gql-name graphql-utils/kw->gql-name}))
                                    :id @form-data}
                           :format :json
                           :response-format :json
                           :handler #_#(log/info "response: "(graphql-utils/js->clj-response (clj->js %) {:gql-name->kw graphql-utils/gql-name->kw}))
                           #(do
                              (swap! state (fn [old new] (into old (concat new)))
                                       (get-in (graphql-utils/js->clj-response (clj->js %) {:gql-name->kw graphql-utils/gql-name->kw})
                                               [:data :search-memes :items]))
                              (reset! loading? false))
                           :error-handler #(log/error "error: " %))


                       )

        ]

    (fn []

      (log/debug "#state" {:c (-> @state count)})

      (if true #_(empty? all-memes)
          ;;[no-items-found]
          [:div.scroll-area
           [:div.tiles
            [infinite-scroll/infinite-scroll {:loading? @loading?
                                              :load-fn (fn []

                                                         (log/debug "@on-load")

                                                         #_(let [{:keys [:has-next-page :end-cursor]} (:search-memes last-meme)]
                                                             (when has-next-page
                                                               (dispatch [:district.ui.graphql.events/query
                                                                          {:query {:queries [(build-tiles-query @form-data end-cursor)]}
                                                                           :id @form-data}])))

                                                         (let [from (-> @state count)
                                                               to (+ page-size from)]
                                                           (log/debug "@on-load" {:f from :t to})
                                                           (update-state {:from from :to to}))

                                                         )}

             #_(reverse @state)

             (doall
              (for [{:keys [:reg-entry/address] :as meme} (reverse @state)]
                ^{:key address} [puke meme]))

             ;; (doall
             ;;  (for [{:keys [:reg-entry/address] :as meme} all-memes]
             ;;    ^{:key address} [tiles/meme-tile meme]))

             ]]]))


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


          [dank-registry-tiles form-data meme-search]


          ]]))))
