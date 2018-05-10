(ns memefactory.ui.components.app-layout
  (:require ;;[soda-ash.core :as ui]
   [reagent.core :as r]
   [district.ui.component.active-account :refer [active-account]]
   [district.ui.component.active-account-balance :refer [active-account-balance]]
   [re-frame.core :refer [subscribe dispatch]]))

(def nav-menu-items-props [{:text "Marketplace"
                            :route :route.marketplace/index
                            :class :marketplace}
                           {:text "Dank Registry"
                            :route :route.dankregistry/index
                            :class :dankregistry
                            :children [{:text "Submit"
                                        :route :route.marketplace/index}
                                       {:text "Vote"
                                        :route :route.marketplace/index}]}])

(defn app-bar []
  (let [open? (r/atom nil);;(subscribe [:district0x.transaction-log/open?])
        my-addresses (r/atom nil);;(subscribe [:district0x/my-addresses])
        ]
    (fn []
      [:div.app-bar
       [:div.left-section
        [active-account]
        [:i.icon.hamburger
         {:on-click (fn [e]
                      (dispatch [:district0x.menu-drawer/set true])
                      (.stopPropagation e))}]]
       [:div.middle-section
        ;; [app-bar-search]
        ]
       [:div.right-section
        {:on-click (fn []
                     (if (empty? @my-addresses)
                       (dispatch [:district0x.location/nav-to :route/how-it-works {}])
                       (dispatch [:district0x.transaction-log/set-open (not @open?)])))}
        (if false;;(empty? @my-addresses)
          [:div "No Accounts"]
          [:div
           [active-account-balance
            {:token :DANK
             :locale "en-US"
             :max-fraction-digits 3
             :min-fraction-digits 2}]
           [active-account-balance
            {:token :ETH
             :locale "en-US"
             :max-fraction-digits 3
             :min-fraction-digits 2}]])
        [:i.icon.transactions]]])))

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

(defn app-menu [props active-page]
  [:div.ui.link.list
   (doall
    (for [{:keys [:text :route :href :class :children]} props]
      (let [href (or href (path-for route))]
        [:div
         [:div.item
          {:class (concat [class] (when (current-page? active-page href)));; (conj [class] )
           }
          [:a {:href href} text]]
         (when children
           [app-menu children active-page])
         ])))])

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
         [app-menu nav-menu-items-props @active-page]
         [district0x-banner]]]
       [:div.app-content
        [app-bar]
        [:div.main-content
         children]]
       ;; [snackbar]
       ])))
