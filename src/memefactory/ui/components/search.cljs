(ns memefactory.ui.components.search
  (:require
   [district.ui.component.form.input :refer [with-label select-input text-input checkbox-input with-label chip-input]]
   [print.foo :refer [look] :include-macros true])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defn get-by-path
  ([doc path]
   (get-by-path doc path nil))
  ([doc path default]
   (let [n-path (flatten
                 (if-not (seq? path)
                   [path]
                   path))]
     (get-in doc n-path default))))

(defn chip-render [c]
  [:span c])

(defn search-tools [{:keys [title sub-title form-data tags selected-tags-id search-id select-options check-filters
                            on-selected-tags-change on-search-change on-select-change]}]
  [:div.search-form
   [:div.icon]
   [:div.header
    [:h2 title]
    [:h3 sub-title]]
   [:div.body
    [:div.form
     [with-label
      "Name"
      [text-input {:on-change on-search-change
                   ;; :placeholder "Name"
                   :form-data form-data
                   :id search-id}]

      {:group-class :name
       :form-data form-data
       :id search-id}]
     [select-input {:form-data form-data
                    :id :order-by
                    :group-class :options
                    :options select-options
                    :on-change on-select-change}]
     [with-label
      "Tags"
      [chip-input {:form-data form-data
                   :chip-set-path [selected-tags-id]
                   :ac-options tags
                   :on-change on-selected-tags-change
                   :chip-render-fn chip-render}]
      {:group-class :ac-options
       :form-data form-data
       :id [selected-tags-id]}]
     (when check-filters
       (doall
        (for [{:keys [id label on-check-filter-change]} check-filters]
          ^{:key id} [:div {:class (str "check-cheapest" )};;(name id)
                      [:label label]
                      [checkbox-input {:form-data form-data
                                       :id id
                                       :on-change on-check-filter-change}]])))]]])
