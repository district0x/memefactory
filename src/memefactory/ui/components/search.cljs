(ns memefactory.ui.components.search
  (:require
   [district.ui.component.form.chip-input :refer [chip-input]]
   [district.ui.component.form.input :refer [select-input text-input checkbox-input with-label]]
   [district.ui.router.events :as router-events]
   [district.ui.router.subs :as router-subs]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   ))

(defn chip-render [c]
  [:span c])

;; title -> (cond
;;               (= :route.marketplace/index name)
;;               "Marketplace"

;;               (= :route.memefolio/index name)
;;               "My Memefolio"

;;               :else name)

;; on-search-change #(re-frame/dispatch [::router-events/navigate name params (merge query
;;                                                                                   {search-id %})])

;; on-select-change #(re-frame/dispatch [::router-events/navigate name params (merge query
;;                                                                                   {:order-by %})])

;; on-check-filter-change #(re-frame/dispatch [::router-events/navigate name params (merge query
;;                                                                                         {id (str (id @form-data))})])

(defn search-tools [{:keys [title sub-title form-data tags selected-tags-id search-id check-filter
                            on-selected-tags-change on-search-change on-check-filter-change on-select-change]}]
  (let [{:keys [:name :query :params]} (look @(re-frame/subscribe [::router-subs/active-page]))]
    [:div.container
     [:div.left-section
      [:div.header
       [:img]
       [:h2 title]
       [:h3 "Lorem ipsum dolor..."]]
      [:div.body
       [text-input {:on-change on-search-change
                    :form-data form-data
                    :id search-id}]
       [select-input {:form-data form-data
                      :id :order-by
                      :options [{:key "started-on" :value "Newest"}
                                {:key "meme-total-minted" :value "Rarest"}
                                {:key "price" :value "Cheapest"}
                                {:key "random" :value "Random"}]
                      :on-change on-select-change}]
       [chip-input {:form-data form-data
                    :chip-set-path [selected-tags-id]
                    :ac-options tags
                    :on-change on-selected-tags-change
                    :chip-render-fn chip-render}]
       (when check-filter
         (let [{:keys [id label ]} check-filter]
           [with-label label
            [checkbox-input {:form-data (look form-data)
                             :id id
                             :on-change on-check-filter-change}]]))]]
     [:div.right-section
      [:img]]]))
