(ns memefactory.ui.components.search
  (:require
   [clojure.string :as str]
   [district.ui.component.form.input :refer [with-label select-input text-input checkbox-input with-label chip-input radio-group]]
   [print.foo :refer [look] :include-macros true]
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [memefactory.ui.events :as mf-events]
   [memefactory.ui.components.share-buttons :as share-buttons])
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


(def auctions-option-filters [{:key :only-lowest-number :label "Show only the lowest card number of meme"}
                              {:key :only-cheapest :label "Show only the cheapest card of meme"}
                              {:key :all-cards :label "All cards"}])

(def nsfw-check-filter {:label "NSFW"
                        :id :nsfw-switch
                        :on-check-filter-change #(re-frame/dispatch [::mf-events/nsfw-switch])})

(def nsfw-tag "nsfw")

(defn search-tools [{:keys [form-data selected-tags-id search-id]}]

  (let [search-input-form-data (r/atom {search-id (get @form-data search-id)})
        chip-input-form-data (r/atom {selected-tags-id (get @form-data selected-tags-id)})]
    (fn [{:keys [title sub-title form-data tags selected-tags-id search-id select-options check-filters
                 on-selected-tags-change on-search-change on-select-change option-filters option-filters-id]}]
      [:div.search-form
      [:div.icon]
      [:div.header
       [:h2 title]
       [:h3 sub-title]
       [share-buttons/share-buttons (. (. js/document -location) -href)]]
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
                                           (when on-search-change (on-search-change input))))))
                      :on-key-up (fn [e]
                                   (let [input (get @search-input-form-data search-id)]
                                     (when (str/blank? input)
                                       (swap! form-data assoc search-id input))))
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
                                   (when on-selected-tags-change (on-selected-tags-change)))
                      :chip-render-fn chip-render
                      :select-keycodes #{13 188 32}
                      :dom-id :tags}]
         {:group-class :ac-options
          :for :tags
          :form-data form-data
          :id [selected-tags-id]}]
        (when check-filters
          [:div.check-group
           (doall
                             (for [{:keys [id label on-check-filter-change]} check-filters]
                               ^{:key id} [:div.single-check
                                           [checkbox-input {:form-data form-data
                                                            :id id
                                                            :on-change on-check-filter-change}]
                                           [:label {:on-click #(swap! form-data update id not)}
                                            label]]))])
        (when option-filters
          [:div.options-group
           [radio-group {:id option-filters-id
                         :form-data form-data
                         :options option-filters}]])]]])))
