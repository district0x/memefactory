(ns memefactory.ui.components.panes
  (:require
    [district.ui.router.subs :as router-subs]
    [memefactory.ui.components.general :refer [nav-anchor]]
    [re-frame.core :refer [subscribe dispatch]]))


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

              [nav-anchor {:route :route.dank-registry/vote
                           :params {}
                           :query {:tab (:title t)}}
               (:title t)]]))]
         [:div.selected-tab-body
          (some #(when (= (:title %) selected-tab)
                   (:content %))
                tabs)]]))))
