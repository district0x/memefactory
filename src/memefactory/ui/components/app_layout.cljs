(ns memefactory.ui.components.app-layout
  (:require
   [reagent.core :as r]
   [district.ui.component.active-account :refer [active-account]]
   [district.ui.component.active-account-balance :refer [active-account-balance]]
   [district.ui.component.form.input :as inputs :refer [text-input*]]
   [re-frame.core :refer [subscribe dispatch]]
   [memefactory.ui.subs :as mf-subs]
   [memefactory.ui.utils :as mf-utils]))

(def nav-menu-items [{:text "Marketplace"
                      :route :route.marketplace/index
                      :class :marketplace}
                     {:text "Dank Registry"
                      :route :route.dank-registry/index
                      :class :dankregistry
                      :children [{:text "Submit"
                                  :route :route.dank-registry/submit}
                                 {:text "Vote" 
                                  :route :route.dank-registry/vote}
                                 {:text "Challenge"
                                  :route :route.dank-registry/challenge}
                                 {:text "Browse"
                                  :route :route.dank-registry/browse}]}
                     {:text "Leaderboard"
                      :route :route.leaderboard/index
                      :class :leaderboard
                      :children [{:text "Dankest Memes"
                                  :route :route.leaderboard/dankests}
                                 {:text "Creators"
                                  :route :route.leaderboard/creators}
                                 {:text "Collectors"
                                  :route :route.leaderboard/collectors}
                                 {:text "Curators"
                                  :route :route.leaderboard/curators}]}
                     {:text "My Memefolio"
                      :route :route.memefolio/index
                      :class :memefolio}
                     {:text "My Settings"
                      :route :route.my-settings/index
                      :class :my-settings}
                     {:text "How it Works"
                      :route :route.how-it-works/index
                      :class :how-it-works}
                     {:text "About"
                      :route :route.about/index
                      :class :about}])

(defn search-form [form-data]
  [:div.search
   [text-input* {:form-data form-data
                 :id :term}]
   [:div.go-button]])

(defn app-bar [{:keys [search-atom]}]
  (let [open? (r/atom nil);;(subscribe [:district0x.transaction-log/open?])
        my-addresses (r/atom nil);;(subscribe [:district0x/my-addresses])
        search-term (r/atom {})]
    (fn []
      [:div.app-bar
       [:div.left-section
        [active-account]
        [:i.icon.hamburger
         {:on-click (fn [e]
                      (dispatch [:district0x.menu-drawer/set true])
                      (.stopPropagation e))}]]
       [:div.middle-section
        [search-form search-term]]
       [:div.right-section
        {:on-click (fn []
                     (if (empty? @my-addresses)
                       (dispatch [:district0x.location/nav-to :route/how-it-works {}])
                       (dispatch [:district0x.transaction-log/set-open (not @open?)])))}
        (if false;;(empty? @my-addresses)
          [:div "No Accounts"]
          [:div.accounts
           [active-account-balance
            {:token :DANK
             :class :dank
             :locale "en-US"
             :max-fraction-digits 3
             :min-fraction-digits 2}]
           [active-account-balance
            {:token :ETH
             :class :eth
             :locale "en-US"
             :max-fraction-digits 3
             :min-fraction-digits 2}]])
        [:i.icon.transactions]]])))

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

(defn app-menu
  ([items active-page] (app-menu items active-page 0))
  ([items active-page depth]
   ^{:key (str depth)}
   [:ul.node
    (doall
     (map-indexed (fn [idx {:keys [:text :route :href :class :children]}]
                    (let [href (or href (mf-utils/path route))]
                      ^{:key (str depth "-" idx)}
                      [:li.node-content
                       [:div.item
                        {:class (concat [(when class (name class))] (when (current-page? active-page href)))}
                        [:a {:href href} text]]
                       (when children
                         [app-menu children active-page (inc depth)])]))
                  items))]))

(defn app-layout []
  (let [active-page (subscribe [::mf-subs/active-page])
        drawer-open? (subscribe [::mf-subs/menu-drawer-open?])]
    (fn [{:keys [:meta :search-atom]} & children]
      [:div.app-container
       [:div.app-menu
        {:class (when-not @drawer-open? "closed")}
        [:div.menu-content
         [app-menu nav-menu-items @active-page]]
        [district0x-banner]]
       [:div.app-content
        [app-bar {:search-atom search-atom}]
        (into [:div.main-content]
              children)]])))
