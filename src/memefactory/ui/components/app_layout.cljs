(ns memefactory.ui.components.app-layout
  (:require ;;[soda-ash.core :as ui]
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]))

(def nav-menu-items-props [{:text "Marketplace"
                            :route :route.marketplace/index
                            :class :marketplace}
                           {:text "Dank Registry"
                            :route :route.dankregistry/index
                            :class :dankregistry}])
;;stubs
(defn path-for [path]
  (str path))
(defn current-page? [a b]
  (= a b))

(defn district0x-banner []
  [:div.district0x-banner
   [:div.logo ""]
   [:div "Part of the"]
   [:a
    {:href "https://district0x.io"
     :target :_blank}
    "district0x Network"]])

(defn app-layout []
  (let [active-page (r/atom nil);;(subscribe [:district0x/active-page])
        drawer-open? (r/atom true);;(subscribe [:district0x/menu-drawer-open?])
        ]
    (fn [{:keys [:meta]} & children]
      [:div.app-container
       #_{:ref (fn [el]
               (when (and el (not @app-container-ref))
                 (reset! app-container-ref el)))}
       ;;[meta-tags meta]
       [:div.app-menu
        {:class (when-not @drawer-open? "closed")}
        [:div.menu-content
         ;; [side-nav-menu-logo]
         [:div.ui.link.list
          (doall
           (for [{:keys [:text :route :href :class]} nav-menu-items-props]
             (let [href (or href (path-for route))]
               [:div.item
                {:class (concat [class] (when (current-page? @active-page href)));; (conj [class] )
                 }
                [:a {:href href} text]])))]
         [district0x-banner]]]
       [:div.app-content
        ;; [app-bar]
        [:div.main-content
         children]]
       ;; [snackbar]
       ])))
