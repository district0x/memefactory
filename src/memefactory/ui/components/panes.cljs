(ns memefactory.ui.components.panes
  (:require
    [district.ui.router.subs :as router-subs]
    [memefactory.ui.components.general :refer [nav-anchor]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))


(defn tabbed-pane [tabs]
  (let [page (subscribe [::router-subs/active-page])]
    (fn [tabs]
      (let [selected-tab (or (-> @page :query :tab)
                             (-> tabs first :title))]
        [:div.tabbed-panel
         [:div.tabs-titles
          (doall
           (for [t tabs]
             [:div.tab {:class (when (= (:title t) selected-tab) "selected")
                        :key (:title t)}

              [nav-anchor {:route (:route t)
                           :params {}
                           :query {:tab (:title t)}}
               (:title t)]]))]
         [:div.selected-tab-body
          (some #(when (= (:title %) selected-tab)
                   (:content %))
                tabs)]]))))

(defn simple-tabbed-pane [tabs]
  (let [selected-pane (r/atom (-> tabs first :title))]
    (fn [tabs]
      [:div.simple-tabbed-pane
       [:div.tabs-titles
        (doall
         (for [t tabs]
           [:div.tab {:class (when (= (:title t) @selected-pane) "selected")
                      :key (:title t)
                      :on-click #(reset! selected-pane (:title t))}
            [:a (:title t)]]))]
       [:div.selected-tab-body
        (some #(when (= (:title %) @selected-pane)
                 (:content %))
              tabs)]])))
