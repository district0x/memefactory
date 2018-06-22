(ns memefactory.ui.memefolio.page
  (:require
   [cljs-web3.core :as web3]
   [clojure.string :as str]
   [cljs-solidity-sha3.core :as sha3]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.time :as time]
   [district.ui.component.form.input :as inputs]
   [district.ui.component.input :as input]
   [district.ui.component.page :refer [page]]
   [district.ui.component.tx-button :as tx-button]
   [district.ui.graphql.subs :as gql]
   [district.ui.server-config.subs :as config-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :as app-layout]
   [memefactory.ui.components.tiles :as tiles]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :as ratom]
   ))

;; TODO: search

;; TODO: move to district.format
(defn format-percentage [p t]
  (str (int (Math/fround (/ (* p 100.0) t))) "%"))

(def default-tab :collected)

(defn resolve-image [meta-hash]
  "http://upload.wikimedia.org/wikipedia/en/thumb/6/63/Feels_good_man.jpg/200px-Feels_good_man.jpg")

;; TODO: infinite scroll
(defmulti panel (fn [tab & opts] tab))

(defmulti header (fn [tab & opts] tab))

(defmethod header :collected [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:user {:user/address active-account}
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
                     :display "inline-block"}]
          [:div.header
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

(defn collected-tile-front [{:keys [:meme/meta-hash]}]
  [:img {:src (resolve-image meta-hash)}])

(defn sell-form [{:keys [:meme/title :meme-auction/token-count :meme-auction/token-ids :show?]}]
  (let [tx-id (str (random-uuid))
        max-duration (subscribe [::config-subs/config :deployer :initial-registry-params :meme-registry :max-auction-duration])
        form-data (r/atom {:meme-auction/amount token-count
                           :meme-auction/start-price 0.1
                           :meme-auction/end-price 0.01
                           :meme-auction/duration @max-duration
                           :meme-auction/description "description"})
        errors (r/atom {})
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-token/transfer-multi-and-start-auction tx-id}])]
    (fn []
      [:div.sell-form
       [:div.field
        [inputs/with-label "Amount"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :meme-auction/amount}]]
        [:label "Max " token-count]]
       [:div.field
        [inputs/with-label "Start Price"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :meme-auction/start-price}]]]
       [:div.field
        [inputs/with-label "End Price"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :meme-auction/end-price}]]]
       [:div.field
        [inputs/with-label "Duration"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :meme-auction/duration}]]
        [:label "Max " @max-duration]]
       [:div.field
        [inputs/with-label "Short sales pitch"
         [inputs/textarea-input {:form-data form-data
                                 :errors errors
                                 :id :meme-auction/description}]]]
       [:div.buttons
        [:button {:on-click #(swap! show? not)} "Cancel"]
        [tx-button/tx-button {:primary true
                              :disabled false
                              :pending? @tx-pending?
                              :pending-text "Creating offering..."
                              :on-click (fn []
                                          (re-frame/dispatch [:meme-token/transfer-multi-and-start-auction (merge @form-data
                                                                                                                  {:send-tx/id tx-id
                                                                                                                   :meme/title title
                                                                                                                   :meme-auction/token-ids (->> token-ids
                                                                                                                                                (take (int (:meme-auction/amount @form-data)))
                                                                                                                                                (map int))})]))}
         "Create Offering"]]])))

(defn collected-tile-back [{:keys [:meme/number :meme/title :meme-auction/token-count :meme-auction/token-ids]}]
  (let [sell? (r/atom false)]
    (fn []
      (if-not @sell?
        [:div.collected-tile-back
         [:div [:b (str "#" number)]]
         [:button {:on-click #(swap! sell? not)}
          "Sell"]]
        [:div
         [:h1 (str "Sell" "#" number " " title)]
         [sell-form {:meme/title title
                     :meme-auction/token-ids token-ids
                     :meme-auction/token-count token-count
                     :show? sell?}]]))))

(defmethod panel :collected [tab active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:owner active-account}
                                      [[:items [:reg-entry/address
                                                :reg-entry/status
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
          (map (fn [{:keys [:reg-entry/address :reg-entry/status :meme/meta-hash :meme/number
                            :meme/title :meme/total-supply :meme/owned-meme-tokens] :as meme}]
                 (when address
                   (let [token-ids (map :meme-token/token-id owned-meme-tokens)
                         token-count (count token-ids)]
                     ^{:key address} [:div
                                      [tiles/flippable-tile {:id address
                                                             :front [collected-tile-front {:meme/meta-hash meta-hash}]
                                                             :back [collected-tile-back {:meme/number number
                                                                                         :meme/title title
                                                                                         :meme/owned-meme-tokens owned-meme-tokens
                                                                                         :meme-auction/token-count token-count
                                                                                         :meme-auction/token-ids token-ids}]}]
                                      [:div [:b (str "#" number " " title)]]
                                      [:div [:span (str "Owning " token-count " out of " total-supply)]]])))
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
                     :white-space "normal"}]
          [:div.header {:style {:white-space "nowrap"}}
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

(defn issue-form [{:keys [:meme/title :reg-entry/address :max-amount]}]
  (let [tx-id (str (random-uuid))
        form-data (r/atom {:meme/amount max-amount})
        errors (ratom/reaction {:local #(let [{:keys [:amount]} @form-data]
                                          (and (not (js/isNaN amount))
                                               (int? amount)
                                               (<= amount max-amount)))})
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme/mint tx-id}])]
    (fn []
      [:div.issue-form
       [:div.field
        [inputs/text-input {:form-data form-data
                            :errors errors
                            :id :meme/amount}]
        [tx-button/tx-button {:primary true
                              :disabled false
                              :pending? @tx-pending?
                              :pending-text "Issuing..."
                              :on-click (fn []
                                          (re-frame/dispatch [:meme/mint (merge @form-data
                                                                                {:meme/title title
                                                                                 :reg-entry/address address
                                                                                 :send-tx/id tx-id})]))}
         "Issue"]
        [:label "Max " max-amount]]])))

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
                                         :total-count]]]}])]
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
                          (let [status (graphql-utils/gql-name->kw status)]
                            ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                           :height 280
                                                                           :display "block"}}
                                             [:img {:src (resolve-image meta-hash)}]
                                             [:div [:b (str "#" number " " title)]]
                                             [:div [:span (str total-minted "/" total-supply" Issued")]]
                                             [:div
                                              (cond
                                                (= status :reg-entry.status/whitelisted)
                                                [:label [:b "In Registry"]]

                                                (= status :reg-entry.status/blacklisted)
                                                [:label [:b "Rejected"]]

                                                :else
                                                [:label [:b "Challenged"]])]

                                             (when (= status :reg-entry.status/whitelisted)
                                               [issue-form {:meme/title title
                                                            :reg-entry/address address
                                                            :max-amount (- total-supply total-minted)}])])))
                      (-> @created :search-memes :items)))]]))))

(defmethod header :curated [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:user {:user/address active-account}
                                      [:user/curator-rank
                                       :user/total-created-challenges
                                       :user/total-created-challenges-success
                                       :user/challenger-total-earned
                                       :user/total-participated-votes
                                       :user/total-participated-votes-success
                                       :user/voter-total-earned]]]}])]
    (fn []
      (if (:graphql/loading? @query)
        [:div.loading "Loading..."]
        (let [{:keys [:user/curator-rank :user/total-created-challenges :user/total-created-challenges-success
                      :user/challenger-total-earned :user/total-participated-votes :user/total-participated-votes-success
                      :user/voter-total-earned]} (:user @query)
              style {:background-color "yellow"
                     :display "inline-block"}]
          [:div.header
           [:div.rank {:style {:background-color "orange"
                               :display "inline-block"}}
            (str "RANK: " curator-rank)]
           [:div.curator {:style style}
            [:div.challenges
             "CHALLENGES:"
             [:div {:style style} [:b "Success Rate:"] total-created-challenges-success "/" total-created-challenges
              " (" (format-percentage total-created-challenges-success total-created-challenges)  ")"]
             [:div {:style style} [:b "Earned:"] (str (web3/from-wei challenger-total-earned :ether) " DANK")]]
            [:div.votes "VOTES:"
             [:div {:style style} [:b "Success Rate:"] total-participated-votes "/" total-participated-votes-success
              " (" (format-percentage total-participated-votes-success total-participated-votes)  ")"]
             [:div {:style style} [:b "Earned:"] (str (web3/from-wei voter-total-earned :ether) " DANK")]]
            [:div.total-earnings "TOTAL-EARNINGS:"
             [:div {:style style} (str (web3/from-wei (+ challenger-total-earned voter-total-earned) :ether) " DANK")]]]])))))

(defmethod panel :curated [tab active-account]
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
        [:div.canvas
         [(header tab active-account)]
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
               (-> @query :search-memes :items))]]))))

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
      [app-layout/app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.memefolio
        [tabbed-pane]]])))
