(ns memefactory.ui.components.chip-input
  (:require [reagent.core :as r]
            [district.ui.component.form.input :refer [#_text-input get-by-path assoc-by-path]]
            [clojure.string :as str])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defn text-input* [{:keys [id form-data errors on-change on-key-down attrs input-type] :as opts}]
  (fn [{:keys [id form-data errors on-change attrs input-type] :as opts}]
    (let [a (if (= input-type :textarea)
              :textarea
              :input)]
      [a (merge
          {:type "text"
           :value (get-by-path @form-data id "")
           :on-change #(let [v (-> % .-target .-value)]
                         (swap! form-data assoc-by-path id v)
                         (when on-change
                           (on-change v)))
           :on-key-down on-key-down}
          attrs)])))

(defn autocomplete-input [{:keys [form-data id ac-options on-option-selected on-empty-backspace]}]
  (let [selected-idx (r/atom 0)
        selectable-opts (reaction (let [input (get @form-data id)]
                                    (when (not-empty input)
                                      (filter #(str/starts-with? % input) @ac-options))))]
    (fn [{:keys [form-data id ac-options on-option-selected on-empty-backspace]}]
      (let [select-opt (fn [o]
                         (swap! form-data assoc id "") 
                         (reset! selected-idx 0)
                         (on-option-selected o))]
        [:div.autocomplete-input
         [text-input* {:form-data form-data
                       :id id
                       :on-key-down (fn [e]
                                      (let [key-code (-> e .-keyCode)
                                            input (get @form-data id)]
                                        (cond
                                          (and (= key-code 8) ;; backspace key
                                               (empty? input))
                                          (on-empty-backspace)

                                          (= key-code 13) ;; return key
                                          (select-opt (nth @selectable-opts @selected-idx))

                                          (= key-code 40) ;; down key
                                          (swap! selected-idx #(min (inc %) (dec (count @selectable-opts))))

                                          (= key-code 38) ;; up key
                                          (swap! selected-idx #(max (dec %) 0)))))}]
         (when (not-empty @selectable-opts)
           [:ol.options
            (doall
             (map-indexed
              (fn [idx opt]
                ^{:key opt}
                [:li.option {:class (when (= idx @selected-idx) "selected")
                             :style (when (= idx @selected-idx) {:background-color "red"}) ;; for testing only
                             :on-click #(select-opt opt)} 
                 opt])
             @selectable-opts))])]))))

(defn chip-input [{:keys [form-data chip-set-path ac-options chip-render-fn]}]
  (let [effective-options (reaction (into [] (remove (set (get-in @form-data chip-set-path)) ac-options)))]
    (fn [{:keys [form-data chip-set-path ac-options chip-render-fn]}]
     [:div.chip-input
      [:ol.chip-input
       (for [c (get-in @form-data chip-set-path)]
         ^{:key c}
         [:li.chip
          (chip-render-fn c)
          [:span {:on-click #(swap! form-data update-in chip-set-path (fn [cs] (remove #{c} cs)))}
           "X"]])]
      [autocomplete-input {:form-data form-data
                           :id :text
                           :ac-options effective-options
                           :on-option-selected #(swap! form-data update-in chip-set-path (fn [cs] (conj cs %)))
                           :on-empty-backspace #(swap! form-data update-in chip-set-path butlast)}]])))

;;;;;;;;;;;;;;;;;;;
;; Example usage ;;
;;;;;;;;;;;;;;;;;;;

#_(let [data (r/atom {:tags []})]
    [chip-input {:form-data data
                 :chip-set-path [:tags]
                 :ac-options ["some tag"
                              "some other tag"
                              "a nice tag"
                              "a beautiful tag"
                              "something else"
                              "another"]
                 :chip-render-fn chip-render}])
