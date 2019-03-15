(ns memefactory.ui.components.search
  (:require
   [district.ui.component.form.input :refer [with-label select-input text-input checkbox-input with-label chip-input]]
   [print.foo :refer [look] :include-macros true]
   [reagent.core :as r])
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

  (let [search-input-form-data (r/atom {search-id (get @form-data search-id)})
        chip-input-form-data (r/atom {})]
    (fn [{:keys [title sub-title form-data tags selected-tags-id search-id select-options check-filters
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
         [text-input {:on-key-down (fn [e]
                                     (let [key-code (-> e .-keyCode)
                                           input (get @search-input-form-data search-id)]
                                       (cond
                                         (= key-code 13) ;; return key
                                         (do
                                           (swap! form-data assoc search-id input)
                                           (when on-search-change (on-search-change input)))
                                         )))
                      ;; :placeholder "Name"
                      :form-data search-input-form-data
                      :id search-id
                      :dom-id :name-search}]

         {:for :name-search
          :form-data search-input-form-data
          :id search-id}]
        [select-input {:form-data form-data
                       :id :order-by
                       :group-class :options
                       :options select-options
                       :on-change on-select-change}]
        [with-label
         "Tags"
         [chip-input {:form-data chip-input-form-data
                      :chip-set-path [selected-tags-id]
                      :ac-options tags
                      :on-change (fn []
                                   (swap! form-data assoc selected-tags-id (get @chip-input-form-data selected-tags-id))
                                   (on-selected-tags-change))
                      :chip-render-fn chip-render
                      :select-keycodes #{13 188 32}
                      :dom-id :tags}]
         {:group-class :ac-options
          :for :tags
          :form-data form-data
          :id [selected-tags-id]}]
        (when check-filters
          [:div.check-group (doall
                             (for [{:keys [id label on-check-filter-change]} check-filters]
                               ^{:key id} [:div.single-check
                                           [checkbox-input {:form-data form-data
                                                            :id id
                                                            :on-change on-check-filter-change}]
                                           [:label {:on-click #(swap! form-data update id not)}
                                            label]]))])]]])))
