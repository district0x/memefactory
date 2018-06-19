(ns memefactory.ui.memefolio.page
  (:require
   [cljs-web3.core :as web3]
   [clojure.string :as str]
   [district.format :as format]
   [district.time :as time]
   [district.ui.component.input :as input]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [district.graphql-utils :as graphql-utils]
   ))

;; TODO: move to district.format
(defn format-percentage [p t]
  (str (int (Math/fround (/ (* p 100.0) t))) "%"))

(def default-tab :created)

(defn resolve-image [meta-hash]
  "http://upload.wikimedia.org/wikipedia/en/thumb/6/63/Feels_good_man.jpg/200px-Feels_good_man.jpg")

(defmulti panel (fn [tab & opts] tab))

(defmulti header (fn [tab & opts] tab))

(defmethod header :collected [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:user {:user/address (look active-account)}
                                      [:user/address
                                       ;;:user/collector-rank
                                       :user/total-collected-memes
                                       :user/total-collected-token-ids
                                       [:user/largest-buy [:meme-auction/bought-for
                                                           [:meme-auction/meme-token
                                                            [:meme-token/number
                                                             [:meme-token/meme
                                                              [:meme/title
                                                               :meme/image-hash
                                                               :meme/total-minted]]]]]]]]
                                     [:search-memes [:total-count]]
                                     [:search-meme-tokens [:total-count]]]}])]
    (fn []
      (if (:graphql/loading? @query)
        [:div.loading "Loading..."]
        (let [{:keys [:user/collector-rank :user/total-collected-memes :user/total-collected-token-ids
                      :user/largest-buy]} (:user @query)
              {:keys [:meme-auction/bought-for :meme-auction/meme-token]} largest-buy
              {:keys [:meme-token/number :meme-token/meme]} meme-token
              {:keys [:meme/title]} meme
              style {:background-color "yellow"
                     :display "inline-block"
                     :white-space "normal"
                     :width "400px"}]
          [:div.header {:style {:white-space "nowrap"
                                :width "400px"}}
           [:div.rank {:style {:background-color "orange"
                               :display "inline-block"}}
            (str "RANK: " collector-rank)]
           [:div.unique-memes {:style style}
            [:b "Unique Memes: "]
            (str total-collected-memes "/" (-> @query :search-memes :total-count))]
           [:div.unique-memes {:style style}
            [:b "Total Cards: "  ]
            [:span (str total-collected-token-ids "/" (-> @query :search-meme-tokens :total-count))]]
           [:div.largest-buy {:style style}
            [:b "Largest buy: "]
            [:span (str (format/format-eth (web3/from-wei bought-for :ether))
                        "(#" number " " title ")")]]])))))

;; TODO :  sell button
(defmethod panel :collected [tab active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:owner active-account}
                                      [[:items [:reg-entry/address
                                                :meme/meta-hash
                                                :meme/number
                                                :meme/title
                                                [:meme/owned-meme-tokens {:owner active-account}
                                                 [:meme-token/token-id]]
                                                :meme/total-supply]]
                                       :total-count]]]}])]
    (fn []
      (if (:graphql/loading? @query)
        [:div.loading "Loading..."]
        [:div.canvas
         [(header tab active-account)]
         [:div.tiles
               (map (fn [{:keys [:reg-entry/address :meme/meta-hash :meme/number
                                 :meme/title :meme/total-supply :meme/owned-meme-tokens] :as meme}]
                      (when address
                        (do ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                           :height 280
                                                                           :display "block"}}
                                             [:img {:src (resolve-image meta-hash)}]
                                             [:div [:b (str "#" number " " title)]]
                                             [:div [:span (str "Owning " (count owned-meme-tokens) " out of " total-supply)]]])))
                    (-> @query :search-memes :items))]]))))

(defmethod header :created [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/done]}
                                      [[:items [:meme-auction/start-price
                                                :meme-auction/end-price
                                                :meme-auction/bought-for]]]]
                                     [:user {:user/address active-account}
                                      [:user/total-created-memes
                                       :user/total-created-memes-whitelisted
                                       :user/creator-rank
                                       [:user/largest-sale [:meme-auction/start-price
                                                            :meme-auction/end-price
                                                            :meme-auction/bought-for
                                                            [:meme-auction/meme-token
                                                             [:meme-token/number
                                                              [:meme-token/meme
                                                               [:meme/title]]]]]]]]]}])
        now (subscribe [:district.ui.now.subs/now])]
    (fn []
      (if (:graphql/loading? @query)
        [:div.loading "Loading..."]
        (let [{:keys [:user/total-created-memes :user/total-created-memes-whitelisted :user/creator-rank :user/largest-sale]} (-> @query :user)
              {:keys [:meme-auction/meme-token]} largest-sale
              {:keys [:meme-token/number :meme-token/meme]} meme-token
              {:keys [:meme/title]} meme
              creator-total-earned (reduce (fn [total-earned meme-auction]
                                             (+ total-earned (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))))
                                           0
                                           (-> @query :search-meme-auctions :items))
              style {:background-color "yellow"
                     :display "inline-block"
                     :white-space "normal"
                     :width "400px"}]
          [:div.header {:style {:white-space "nowrap"
                                :width "400px"}}
           [:div.rank {:style {:background-color "orange"
                               :display "inline-block"}}
            (str "RANK: " creator-rank)]
           [:div.earned {:style style}
            [:b "Earned: "]
            (format/format-eth (web3/from-wei creator-total-earned :ether))]
           [:div.success-rate {:style style}
            [:b "Success Rate: "]
            (str total-created-memes-whitelisted "/" total-created-memes " ("
                 (format-percentage total-created-memes-whitelisted total-created-memes) ")")]
           [:div.best-sale {:style style}
            [:b "Best Single Card Sale: "]
            (str (-> (shared-utils/calculate-meme-auction-price largest-sale (-> (.getTime @now) time/time-units :seconds))
                     (web3/from-wei :ether)
                     format/format-eth)
                 " (#" number " " title ")")]])))))

;; TODO: add issue button
;; TODO: add status
(defmethod panel :created [tab active-account]
  (let [created (subscribe [::gql/query
                            {:queries [[:search-memes {:creator active-account}
                                        [[:items [:reg-entry/address
                                                  :meme/meta-hash
                                                  :meme/number
                                                  :meme/title
                                                  :meme/total-minted
                                                  :meme/total-supply
                                                  :reg-entry/status]]
                                         :total-count]]]}])
        input-value (r/atom "0")]
    (fn []
      (if (:graphql/loading? @created)
        [:div "Loading..."]
        [:div.canvas
         [(header tab active-account)]
         [:div.tiles
          (doall (map (fn [{:keys [:reg-entry/address :meme/meta-hash :meme/number
                                   :meme/title :meme/total-supply :meme/total-minted
                                   :reg-entry/status] :as meme}]
                        (when address
                          (let [max-value (- total-supply total-minted)
                                valid? #(let [input (js/Number %)]
                                          (and (not (js/isNaN input))
                                               (int? input)
                                               (<= input max-value)))]
                            ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                           :height 280
                                                                           :display "block"}}
                                             [:img {:src (resolve-image meta-hash)}]
                                             [:div [:b (str "#" number " " title)]]
                                             [:div [:span (str total-minted "/" total-supply" Issued")]]

                                             ;; TODO: status here

                                             (when (= status (graphql-utils/kw->gql-name :reg-entry/status-whitelisted))
                                               [:div {:style {:margin-top 10}}
                                                [input/input
                                                 {:label "Issue"
                                                  :fluid true
                                                  :value @input-value
                                                  :error (not (valid? @input-value))
                                                  :on-change #(reset! input-value (aget %2 "value"))}]
                                                [:div [:span (str "Max " max-value)]]])])))
                      (-> @created :search-memes :items)))]]))))

(defmethod panel :curated [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:curator active-account}
                                      [[:items [:reg-entry/address
                                                :meme/meta-hash
                                                :meme/number
                                                :meme/title
                                                [:challenge/vote {:vote/voter active-account}
                                                 [:vote/option]]]]
                                       :total-count]]]}])]
    (fn []
      (if (:graphql/loading? @query)
        [:div "Loading..."]
        [:div.tiles
         (map (fn [{:keys [:reg-entry/address :meme/meta-hash :meme/number
                           :meme/title :challenge/vote] :as meme}]
                (when address
                  (let [{:keys [:vote/option]} vote]
                    ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                   :height 280
                                                                   :display "block"}}
                                     [:img {:src (resolve-image meta-hash)}]
                                     [:div [:b (str "#" number " " title)]]
                                     [:div
                                      (cond
                                        (= option (graphql-utils/kw->gql-name :vote-option/no-vote #_"voteOption_noVote"))
                                        [:label
                                         [:b "Voted Unrevealed"]]

                                        (= option (graphql-utils/kw->gql-name :vote-option/vote-for #_"voteOption_voteFor"))
                                        [:label "Voted Dank"
                                         [:i.icon.thumbs.up.outline]]

                                        (= option (graphql-utils/kw->gql-name :vote-option/vote-against #_"voteOption_voteAgainst"))
                                        [:label
                                         [:b "Voted Stank"]
                                         [:i.icon.thumbs.down.outline]])]])))
              (-> @query :search-memes :items))]))))

(defmethod panel :selling [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/active]}
                                      [[:items [:meme-auction/address
                                                :meme-auction/status
                                                :meme-auction/start-price
                                                :meme-auction/end-price
                                                :meme-auction/bought-for
                                                [:meme-auction/meme-token
                                                 [:meme-token/number
                                                  [:meme-token/meme
                                                   [:meme/title
                                                    :meme/image-hash
                                                    :meme/total-minted]]]]]]
                                       :total-count]]]}])]
    (fn []
      (if (:graphql/loading? @query)
        [:div "Loading..."]
        [:div.tiles
         (doall
          (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
                 (when address
                   (let [{:keys [:meme-token/number :meme-token/meme]} meme-token
                         {:keys [:meme/title :meme/image-hash :meme/total-minted]} meme
                         now (subscribe [:district.ui.now.subs/now])
                         price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
                     ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                    :height 280
                                                                    :display "block"}}
                                      [:img {:src (resolve-image image-hash)}]
                                      [:div.title [:b (str "#" number " " title)]]
                                      [:div.number-minted (str number "/" total-minted)]
                                      [:div.price (format/format-eth (web3/from-wei price :ether))]])))
               (-> @query :search-meme-auctions :items)))]))))

(defmethod panel :sold [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/done]}
                                      [[:items [:meme-auction/address
                                                :meme-auction/status
                                                :meme-auction/start-price
                                                :meme-auction/end-price
                                                :meme-auction/bought-for
                                                [:meme-auction/meme-token
                                                 [:meme-token/number
                                                  [:meme-token/meme
                                                   [:meme/title
                                                    :meme/image-hash
                                                    :meme/total-minted]]]]]]
                                       :total-count]]]}])]
    (fn []
      (if (:graphql/loading? @query)
        [:div "Loading..."]
        [:div.tiles
         (doall
          (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
                 (when address
                   (let [{:keys [:meme-token/number :meme-token/meme]} meme-token
                         {:keys [:meme/title :meme/image-hash :meme/total-minted]} meme
                         now (subscribe [:district.ui.now.subs/now])
                         price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
                     ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                    :height 280
                                                                    :display "block"}}
                                      [:img {:src (resolve-image image-hash)}]
                                      [:div.title [:b (str "#" number " " title)]]
                                      [:div.number-minted (str number "/" total-minted)]
                                      [:div.price (format/format-eth (web3/from-wei price :ether))]])))
               (-> @query :search-meme-auctions :items)))]))))

(defn tabbed-pane []
  (let [tab (r/atom default-tab)
        active-account (subscribe [::accounts-subs/active-account])]
    (fn []
      [:div.tabbed-pane
       [:div.tabs
        (map (fn [tab-id]
               ^{:key tab-id} [:label
                               {:on-click (fn [evt]
                                            (reset! tab tab-id))}
                               [:a (-> tab-id
                                       name
                                       (str/capitalize))]])
             [:collected :created :curated :selling :sold])]
       (if (nil? @active-account)
         [:div.loading "Loading..."]
         [:div.panel
          [(panel @tab @active-account)]])])))

(defmethod page :route.memefolio/index []
  (let [search-atom (r/atom {:term ""})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.memefolio
        [tabbed-pane]]])))
