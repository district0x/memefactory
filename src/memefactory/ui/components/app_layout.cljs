(ns memefactory.ui.components.app-layout
  (:require
   [district.format :as format]
   [district.ui.component.active-account :refer [active-account]]
   [district.ui.component.active-account-balance :refer [active-account-balance] :as account-balances]
   [district.ui.component.form.input :as inputs :refer [text-input*]]
   [district.ui.component.meta-tags :as meta-tags]
   [district.ui.component.notification :as notification]
   [district.ui.component.tx-log :refer [tx-log]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.events :as router-events]
   [district.ui.router.subs :as router-subs]
   [district.ui.web3-tx-log.events :as tx-log-events]
   [district.ui.web3-tx-log.subs :as tx-log-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.ui.subs :as mf-subs]
   [memefactory.ui.utils :as mf-utils]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log :refer [spy]]
   [memefactory.ui.components.general :refer [nav-anchor]]))

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
                      :class :memefolio
                      :needs-account? true}
                     {:text "My Settings"
                      :route :route.my-settings/index
                      :class :my-settings
                      :needs-account? true}
                     {:text "How it Works"
                      :route :route.how-it-works/index
                      :class :how-it-works
                      :needs-account? false}
                     {:text "About"
                      :route :route.about/index
                      :class :about
                      :needs-account? false}
                     {:text "Get DANK"
                      :route :route.get-dank/index
                      :class :faucet
                      :needs-account? true}])

(defn search-form [form-data]
  [:div.search
   [text-input* {:form-data form-data
                 :id :term}]
   [:div.go-button]])

(defn app-bar-mobile [drawer-open?]
  (let [open? (r/atom nil)]
    (fn []
      [:div.app-bar-mobile
       [nav-anchor {:route :route/home}
        [:div.logo]]
       [:div.menu-selection
        [:i.icon.hamburger
         {:on-click (fn [e]
                      (.stopPropagation e)
                      (swap! drawer-open? not))}]]])))

(defn app-bar [{:keys [search-atom]}]
  (let [open? (r/atom nil)
        my-addresses (r/atom nil)
        accounts (subscribe [::accounts-subs/accounts])
        search-term (r/atom {})
        tx-log-open? (subscribe [::tx-log-subs/open?])]
    (fn []
      [:div.app-bar
       [:div.account-section
        [active-account]]
       [:div.tracker-section
        {:on-click (fn []
                     (if (empty? @my-addresses)
                       (dispatch [:district.ui.router.events/navigate :route/how-it-works])
                       (dispatch [:district0x.transaction-log/set-open (not @open?)])))}
        (if-not (seq @accounts)
          [:div.no-accounts "No Accounts"]
          [:div.accounts
           [:div.dank-section {:on-click #(dispatch [::tx-log-events/set-open (not @tx-log-open?)])}
            [:div.dank-logo
             [:img {:src "/assets/icons/dank-logo.svg"}]]
            [active-account-balance
             {:token-code :DANK
              :contract :DANK
              :class "dank"
              :locale "en-US"
              :max-fraction-digits 0}]]
           [:div.eth-section {:on-click #(dispatch [::tx-log-events/set-open (not @tx-log-open?)])}
            [:div.eth-logo
             [:img {:src "/assets/icons/ethereum.svg"}]]
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
   (let [active-account @(subscribe [:district.ui.web3-accounts.subs/active-account])]
     [:ul.node {:key (str depth)}
      (doall
       (map-indexed (fn [idx {:keys [:text :route :params :query :class :children :needs-account?]}]
                      [:li.node-content {:key (str depth "-" idx)}
                       [:div.item
                        {:class (str (when class (name class))
                                     (when (= active-page route) " active")
                                     (when (and needs-account? (not active-account)) " disabled"))}
                        [nav-anchor (merge
                                     {:disabled true}
                                     (when-not (and needs-account? (not active-account))
                                       {:route route
                                        :params params
                                        :query query}))
                         text]]
                       (when children
                         [app-menu children active-page (inc depth)])])
                    items))])))


(defn app-layout []
  (let [root-url (subscribe [::gql/query
                             {:queries [[:config
                                         [[:ui [:root-url]]]]]}])

        active-page (subscribe [::router-subs/active-page])
        drawer-open? (r/atom false)]
    (fn [{:keys [:search-atom :meta]}
         & children]
      [:div.app-container
       [meta-tags/meta-tags meta {:id "image" :name "og:image" :content (str (-> @root-url :config :ui :root-url) "/assets/images/1_OInGI_RrVwH6uF3OcqJuwQ.jpg")}]
       [:div.app-menu
        {:class (when-not @drawer-open? "closed")}
        [:div.menu-content
         [nav-anchor {:route :route/home}
          [:div.mf-logo
           [:img {:src "/assets/icons/mememouth.png"}]
           [:span "MEME FACTORY"]]]
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
