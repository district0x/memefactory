(ns memefactory.ui.components.panes
  (:require [reagent.core :as r]))

(defn tabbed-pane [tabs]
  (let [selected-tab (r/atom (-> tabs first :title))]
    (fn [tabs]
      [:div.tabbed-panel
       [:ol.tabs-titles
        (doall
         (for [t tabs]
           [:li {:class (when (= (:title t) @selected-tab) "selected")
                 :key (:title t)
                 :on-click #(reset! selected-tab (:title t))}
            (:title t)]))]
       [:div.selected-tab-body
        (some #(when (= (:title %) @selected-tab)
                 (:content %))
              tabs)]])))

