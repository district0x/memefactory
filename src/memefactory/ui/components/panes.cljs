(ns memefactory.ui.components.panes
  (:require [reagent.core :as r]))

(defn tabbed-pane [tabs]
  (let [selected-tab (r/atom (-> tabs first :title))]
    (fn [tabs]
      [:div.tabbed-panel
       [:div.tabs-titles
        (doall
         (for [t tabs]
           [:div {:class (when (= (:title t) @selected-tab) "selected")
                 :key (:title t)}
            [:a
             {:on-click (fn [e]
                          (.preventDefault e)
                          (reset! selected-tab (:title t)))
              :href "#"}
             (:title t)]]))]
       [:div.selected-tab-body
        (some #(when (= (:title %) @selected-tab)
                 (:content %))
              tabs)]])))

