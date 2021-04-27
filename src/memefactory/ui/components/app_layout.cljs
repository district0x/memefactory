(ns memefactory.ui.components.app-layout
  (:require
    [district.ui.component.active-account :refer [active-account]]
    [district.ui.component.active-account-balance :refer [active-account-balance]]
    [district.ui.component.form.input :refer [text-input*]]
    [district.ui.component.meta-tags :as meta-tags]
    [district.ui.component.notification :as notification]
    [district.ui.component.tx-log :refer [tx-log]]
    [district.ui.graphql.subs :as gql]
    [district.ui.mobile.subs :as mobile-subs]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.ui.web3-tx-log.subs :as tx-log-subs]
    [memefactory.ui.components.account-balances :refer [account-balances]]
    [memefactory.ui.contract.param-change :as param-change]
    [memefactory.ui.components.general :refer [nav-anchor]]
    [memefactory.ui.subs :as mf-subs]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [taoensso.timbre :as log :refer [spy]]
    [goog.string :as gstring]))


(def nav-menu-items [{:text "Marketplace"
                      :route :route.marketplace/index
                      :class :marketplace}
                     {:text "Dank Registry"
                      :route :route.dank-registry/browse
                      :class :dankregistry
                      :children [{:text "Submit"
                                  :route :route.dank-registry/submit
                                  :class "dank-registry-submit"}
                                 {:text "Vote"
                                  :route :route.dank-registry/vote
                                  :class "dank-registry-vote"}
                                 {:text "Challenge"
                                  :route :route.dank-registry/challenge
                                  :class "dank-registry-challenge"}
                                 {:text "Browse"
                                  :route :route.dank-registry/browse}
                                 {:text "Parameters"
                                  :route :route.param-change/index
                                  :class :param-change
                                  :needs-account? true
                                  :counter :open-param-proposals}]}
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
                     #_ {:text "Get DANK"
                      :route :route.get-dank/index
                      :class :faucet
                      :needs-account? true}
                     {:text "Discord"
                      :url "https://discord.com/invite/sS2AWYm"
                      :class :discord
                      :needs-account? false}
                     {:text "Telegram"
                      :url "https://t.me/district0x"
                      :class :telegram
                      :needs-account? false}
                     {:text "Privacy Policy"
                      :route :route.privacy-policy/index
                      :class :privacy-policy
                      :needs-account? false}
                     {:text "Terms of Use"
                      :route :route.terms/index
                      :class :terms
                      :needs-account? false}])


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
        (when (seq @accounts)
          [account-balances {:with-tx-logs? true :app-bar-width? true}])]])))


(defn district0x-banner []
  [:div.district0x-banner
   [:div.logo ""]
   [:div "Part of the"]
   [:a
    {:href "https://district0x.io"
     :target :_blank
     :rel "noopener noreferrer"}
    "district0x Network"]])


(defn app-menu
  ([items active-page counters] (app-menu items active-page counters 0))
  ([items active-page counters depth]
   (let [active-account @(subscribe [:district.ui.web3-accounts.subs/active-account])]
     [:<>
      ;; Account Balance Header for Mobile
      (when (and active-account (= depth 0))
        [account-balances {:with-tx-logs? false}])
      [:ul.node {:key (str depth)}
       (doall
        (map-indexed (fn [idx {:keys [:text :route :params :query :class :url :children :needs-account? :counter]}]
                       (let [entry-text(if (and counter (pos? (get counters counter)))
                                         (gstring/format "%s (%d)" text (get counters counter))
                                         text)]
                         [:li.node-content {:key (str depth "-" idx)}
                          [:div.item
                           {:class (str (when class (name class))
                                        (when (= active-page route) " active")
                                        (when (and needs-account? (not active-account)) " disabled"))}
                           (if url
                             [:a {:href url
                                  :target :_blank
                                  :rel "noopener noreferrer"
                                  :class (when class (name class))}
                              entry-text]
                             [nav-anchor (merge
                                          {:disabled true}
                                          (when-not (and needs-account? (not active-account))
                                            {:route route
                                             :params params
                                             :query query
                                             :class (when class (name class))}))
                              entry-text])]
                          (when children
                            [app-menu children active-page counters (inc depth)])]))
                     items))]])))


(defn app-layout []
  (let [root-url (subscribe [::gql/query
                             {:queries [[:config
                                         [[:ui [:root-url]]]]]}])

        active-page (subscribe [::router-subs/active-page])
        drawer-open? (r/atom false)
        active-account (subscribe [:district.ui.web3-accounts.subs/active-account])
        mobile-device? (subscribe [::mobile-subs/coinbase-compatible?])
        coinbase-appstore-link (subscribe [::mf-subs/mobile-coinbase-appstore-link])
        open-param-proposals (subscribe [::gql/query {:queries [[:search-param-changes {;;:order-dir :desc
                                                                                        ;;:order-by :param-changes.order-by/created-on
                                                                                        :statuses [:reg-entry.status/commit-period
                                                                                                   :reg-entry.status/reveal-period
                                                                                                   :reg-entry.status/challenge-period]}
                                                                 [:total-count]]]}
                                         {:refetch-on #{::param-change/create-param-change-success}}])]
    (fn [{:keys [:search-atom :meta :tags]}
         & children]
      [:div.app-container {:id "app-container"}
       [apply meta-tags/meta-tags meta (or tags
                                           [{:id "image"
                                             :name "og:image"
                                             :content (str (-> @root-url :config :ui :root-url) "/assets/images/1_OInGI_RrVwH6uF3OcqJuwQ.jpg")}])]
       [:div.app-menu
        {:class (when-not @drawer-open? "closed")}
        [:div.menu-content
         [nav-anchor {:route :route/home}
          [:div.mf-logo
           [:img {:src "/assets/icons/mememouth.png"}]
           [:span "MEME FACTORY"]]]
         [app-menu nav-menu-items (:name @active-page) {:open-param-proposals (-> @open-param-proposals :search-param-changes :total-count)}]]
        [district0x-banner]]
       [:div.app-content
        [app-bar {:search-atom search-atom}]
        [app-bar-mobile drawer-open?]
        (when (and (not @active-account) @mobile-device?)
         [:a.coinbase-promotion
          {:href @coinbase-appstore-link}
          [:span "Submit Memes with "] [:img {:src "/images/coinbase_logo.png"}]])
        [:div.main-content
         [:div.main-content-inner
          (map-indexed (fn [index item]
                         (with-meta item {:key (keyword "c" index)}))
                       children)]]]
       [notification/notification]])))
