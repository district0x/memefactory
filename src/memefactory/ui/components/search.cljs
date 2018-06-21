(ns memefactory.ui.components.search
  (:require [district.ui.component.form.input :refer [select-input text-input checkbox-input with-label]]
            [district.ui.component.form.chip-input :refer [chip-input]]
            [print.foo :refer [look] :include-macros true]))

(defn chip-render [c]
  [:span c])

(defn search-tools [{:keys [form-data tags selected-tags-id search-id check-filter]}]
  (look tags)
  [:div.container 
   [:div.left-section
    [:div.header
     [:img]
     [:h2 "Marketplace"]
     [:h3 "Lorem ipsum dolor..."]]
    [:div.body
     [text-input {:form-data form-data
                  :id search-id}]
     [select-input {:form-data form-data
                    :id :order-by
                    :options [{:key "started-on" :value "Newest"}
                              {:key "meme-total-minted" :value "Rarest"}
                              {:key "price" :value "Cheapest"}
                              {:key "random" :value "Random"}]}]
     [chip-input {:form-data form-data
                  :chip-set-path [selected-tags-id]
                  :ac-options tags
                  :chip-render-fn chip-render}]
     (when check-filter
       (let [{:keys [id label]} check-filter]
        [with-label label
         [checkbox-input {:form-data form-data
                          :id id}]]))]]
   [:div.right-section
    [:img]]])
