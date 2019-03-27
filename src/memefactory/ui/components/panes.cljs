(ns memefactory.ui.components.panes
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [district.ui.router.subs :as router-subs]
            [district.ui.router.events :as router-events]
            [memefactory.ui.components.general :refer [nav-anchor]]))

(defn tabbed-pane [tabs]
  (let [page (subscribe [::router-subs/active-page])]
    (fn [tabs]
      (let [selected-tab (or (-> @page :query :tab)
                             (-> tabs first :title))]
        [:div.tabbed-panel
         [:div.tabs-titles
          (doall
           (for [t tabs]
             [:div {:class (when (= (:title t) selected-tab) "selected")
                    :key (:title t)}

              [nav-anchor {:route :route.dank-registry/vote
                           :params {}
                           :query {:tab (:title t)}}
               (:title t)]]))]
         [:div.selected-tab-body
          (some #(when (= (:title %) selected-tab)
                   (:content %))
                tabs)]]))))
