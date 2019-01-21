(ns memefactory.ui.components.app-layout
  (:require
   [reagent.core :as r]
   [district.ui.component.active-account :refer [active-account]]
   [district.ui.component.active-account-balance :refer [active-account-balance] :as account-balances]
   [district.ui.component.form.input :as inputs :refer [text-input*]]
   [district.ui.component.tx-log :refer [tx-log]]
   [district.ui.router.events]
   [district.ui.web3-tx-log.events :as tx-log-events]
   [district.ui.web3-tx-log.subs :as tx-log-subs]
   ;; [district.ui.web3-tx-log-core]
   [re-frame.core :refer [subscribe dispatch]]
   [memefactory.ui.subs :as mf-subs]
   [district.ui.router.subs :as router-subs]
   [memefactory.ui.utils :as mf-utils]
   [district.ui.component.notification :as notification]
   [district.format :as format]))

(def nav-menu-items [{:text "Marketplace"
                      :route :route.marketplace/index
                      :class :marketplace}
                     {:text "Dank Registry"
                      :route :route.dank-registry/browse
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
                      :route :route.leaderboard/dankest
                      :class :leaderboard
                      :children [{:text "Dankest Memes"
                                  :route :route.leaderboard/dankest}
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
                      :class :about}
                     {:text "Get DANK"
                      :route :route.get-dank/index
                      :class :faucet}])

(defn search-form [form-data]
  [:div.search
   [text-input* {:form-data form-data
                 :id :term}]
   [:div.go-button]])

(defn app-bar-mobile [drawer-open?]
  (let [open? (r/atom nil)];;(subscribe [:district0x.transaction-log/open?])]
    (fn []
      [:div.app-bar-mobile
       [:div.logo]
       [:div.menu-selection
        [:i.icon.hamburger
         {:on-click (fn [e]
                      (.stopPropagation e)
                      (swap! drawer-open? not)
                      ;; (dispatch [:district0x.menu-drawer/set true])
                      )}]]
       ])))

(defn app-bar [{:keys [search-atom]}]
  (let [open? (r/atom nil)
        my-addresses (r/atom nil);;(subscribe [:district0x/my-addresses])
        search-term (r/atom {})
        tx-log-open? (subscribe [::tx-log-subs/open?])]
    (fn []
      [:div.app-bar
       [:div.account-section
        [active-account]]
       #_[:div.search-section
        [search-form search-term]]
       [:div.tracker-section
        {:on-click (fn []
                     (if (empty? @my-addresses)
                       (dispatch [:district.ui.router.events/navigate :route/how-it-works {}])
                       (dispatch [:district0x.transaction-log/set-open (not @open?)])))}
        (if false;;(empty? @my-addresses)
          [:div "No Accounts"]
          [:div.accounts
           [:div {:on-click #(dispatch [::tx-log-events/set-open (not @tx-log-open?)])}
            [active-account-balance
             {:token-code :DANK
              :contract :DANK
              :class "dank"
              :locale "en-US"
              :max-fraction-digits 0}]]
           [:div {:on-click #(dispatch [::tx-log-events/set-open (not @tx-log-open?)])}
            [active-account-balance
             {:token-code :ETH
              :locale "en-US"
              :class "eth"}]]
           [tx-log
            {:header-props {:text "Transaction Log"}
             :transactions-props {:transaction-props {:tx-value-el (fn [{:keys [tx]}]
                                                                     [:span.tx-value (format/format-eth (if-let [v (:value tx)]
                                                                                                (/ v 1e18)
                                                                                                0)
                                                                                              {:max-fraction-digits 3})])}}}]])]])))

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
                        {:class (str (when class (name class)) (when (= active-page route) " active"))}
                        [:a {:href href
                             :on-click #(js/window.scrollTo 0 0)}
                         text]]
                       (when children
                         [app-menu children active-page (inc depth)])]))
                  items))]))

(defn app-layout []
  (let [active-page (subscribe [::router-subs/active-page])
        drawer-open? (r/atom false)]
    (fn [{:keys [:meta :search-atom]} & children]
      [:div.app-container
       [:div.app-menu
        {:class (when-not @drawer-open? "closed")}
        [:div.menu-content
         [:div.mf-logo {:on-click #(dispatch [:district.ui.router.events/navigate :route/home {}])}
          [:img {:src "/assets/icons/mememouth.png"}]
          [:span "MEME FACTORY"]]
         [app-menu nav-menu-items (:name @active-page)]]
        [district0x-banner]]
       [:div.app-content
        [app-bar {:search-atom search-atom}]
        [app-bar-mobile drawer-open?]
        [:div.main-content
         [:div.main-content-inner
          (map-indexed (fn [index item]
                         (with-meta item {:key (keyword "c" index)}))
                       children)]]]
       [notification/notification]])))
