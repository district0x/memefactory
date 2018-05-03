(ns memefactory.ui.components.app-layout
  (:require ;;[soda-ash.core :as ui]
            ))

(def nav-menu-items-props [{:text "Marketplace"
                            :route :route.marketplace/index
                            :class :marketplace}
                           {:text "Dank Registry"
                            :route :route.dankregistry/index
                            :class :dankregistry}])

(defn district0x-banner []
  [:div.district0x-banner
   [:div.logo ""]
   [:div "Part of the"]
   [:a
    {:href "https://district0x.io"
     :target :_blank}
    "district0x Network"]])

(defn app-layout []
  (let [active-page (subscribe [:district0x/active-page])]
    (fn [{:keys [:meta]} & children]
      [:div.app-container
       #_{:ref (fn [el]
               (when (and el (not @app-container-ref))
                 (reset! app-container-ref el)))}
       ;;[meta-tags meta]
       [:ul.app-menu
        {:class (when-not @drawer-open? "closed")}
        [:div.menu-content
         ;; [side-nav-menu-logo]
         (doall
          (for [{:keys [:text :route :href :class]} nav-menu-items-props]
            (let [href (or href (path-for route))]
              [:li.item
               {:class (conj [(when (current-page? @active-page href))])}
               [:a {:href href} text]])))
         [district0x-banner]]]
       [:div.app-content
        ;; [app-bar]
        [:div.main-content
         children]]
       ;; [snackbar]
       ])))
