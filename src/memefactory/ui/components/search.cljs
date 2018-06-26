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

(defn search-tools [{:keys [form-data tags selected-tags-id search-id check-filter]}]
  (let [{:keys [:name :query :params]} (look @(re-frame/subscribe [::router-subs/active-page]))]
    [:div.container
     [:div.left-section
      [:div.header
       [:img]
       [:h2 (cond
              (= :route.marketplace/index name)
              "Marketplace"

              (= :route.memefolio/index name)
              "My Memefolio"

              :else name)]
       [:h3 "Lorem ipsum dolor..."]]
      [:div.body
       [text-input {:on-change #(re-frame/dispatch [::router-events/navigate name params (merge query
                                                                                                {search-id %})])
                    :form-data form-data
                    :id search-id}]
       [select-input {:form-data form-data
                      :id :order-by
                      :options [{:key "started-on" :value "Newest"}
                                {:key "meme-total-minted" :value "Rarest"}
                                {:key "price" :value "Cheapest"}
                                {:key "random" :value "Random"}]
                      :on-change #(re-frame/dispatch [::router-events/navigate name params (merge query
                                                                                                  {:order-by %})])}]
       [chip-input {:form-data form-data
                    :chip-set-path [selected-tags-id]
                    :ac-options tags
                    :chip-render-fn chip-render}]
       (when check-filter
         (let [{:keys [id label ]} check-filter]
           [with-label label
            [checkbox-input {:form-data (look form-data)
                             :id id
                             :checked (when (= "true" (id query))
                                        true)
                             :on-change #(re-frame/dispatch [::router-events/navigate name params (merge query
                                                                                                         {id (str (id @form-data))})])}]]))]]
     [:div.right-section
      [:img]]]))
