(ns memefactory.ui.memefolio.page
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.core.match :refer-macros [match]]
    [clojure.string :as str]
    [district.cljs-utils :as cljs-utils]
    [district.format :as format]
    [district.graphql-utils :as gql-utils]
    [district.parsers :as parsers]
    [district.time :as time]
    [district.ui.component.form.input :as inputs]
    [district.ui.component.page :refer [page]]
    [district.ui.component.tx-button :as tx-button]
    [district.ui.graphql.events :as gql-events]
    [district.ui.graphql.subs :as gql]
    [district.ui.now.subs :as now-subs]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [goog.string :as gstring]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [memefactory.ui.components.general :refer [nav-anchor]]
    [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
    [memefactory.ui.components.panels :refer [panel no-items-found]]
    [memefactory.ui.components.search :as search]
    [memefactory.ui.components.spinner :as spinner]
    [memefactory.ui.components.tiles :as tiles]
    [memefactory.ui.contract.meme :as meme]
    [memefactory.ui.contract.meme-token :as meme-token]
    [memefactory.ui.utils :as ui-utils :refer [copy-to-clipboard!]]
    [print.foo :refer [look] :include-macros true]
    [re-frame.core :as re-frame :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.ratom :as ratom]
    [taoensso.timbre :as log :refer [spy]]))

(def default-tab :collected)

(def page-size 6)

(def collected-created-order
  [{:key :memes.order-by/created-on :value "Newest" :order-dir :desc}
   {:key :memes.order-by/number :value "Registry Number" :order-dir :asc}
   {:key :memes.order-by/total-trade-volume :value "Trade Volume" :order-dir :desc}
   {:key :memes.order-by/total-minted :value "Issued Cards" :order-dir :asc}])

(def curated-order
  [{:key :memes.order-by/created-on :value "Newest" :order-dir :desc}
   {:key :memes.order-by/reveal-period-end :value "Recently Revealed" :order-dir :asc}
   {:key :memes.order-by/commit-period-end :value "Recently Commited" :order-dir :asc}
   {:key :memes.order-by/challenge-period-end :value "Recently challenged" :order-dir :asc}])

(def selling-sold-order
  [{:key :meme-auctions.order-by/started-on :value "Newest" :order-dir :desc}
   {:key :meme-auctions.order-by/meme-registry-number :value "Registry Number" :order-dir :asc}
   {:key :meme-auctions.order-by/meme-total-minted :value "Rarest" :order-dir :asc}
   {:key :meme-auctions.order-by/price-asc :value "Cheapest" :order-dir :asc}
   {:key :meme-auctions.order-by/price-desc :value "Most Expensive" :order-dir :desc}])

(defmulti rank (fn [tab & opts] tab))

(defmulti total (fn [tab & opts] tab))


(defn send-form [{:keys [:meme/title :meme-auction/token-count :reg-entry/address] :as meme}
                 send-sell-atom]
  (let [tx-id (str (random-uuid))
        active-account @(subscribe [::accounts-subs/active-account])
        meme-sub (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                     [[:meme/owned-meme-tokens {:owner active-account}
                                                       [:meme-token/token-id]]]]]}])
        token-ids (ratom/reaction (->> @meme-sub
                                       :meme :meme/owned-meme-tokens
                                       (map :meme-token/token-id)))
        form-data (r/atom {:send/amount 1
                           :send/address ""})
        errors (ratom/reaction (let []
                                 {:local {:send/amount (cond-> {:hint (str "Max " (count @token-ids))}
                                                         (and (not (< 0 (parsers/parse-int (:send/amount @form-data)) (inc (count @token-ids))))
                                                              (pos? (count @token-ids)))
                                                         (assoc :error (str "Should be between 1 and " (count @token-ids))))
                                          :send/address (when-not (web3/address? (:send/address @form-data))
                                                          {:error "Invalid address format"})}}))
        critical-errors (ratom/reaction (inputs/index-by-type @errors :error))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-token/safe-transfer-from-multi tx-id}])]
    (fn [{:keys [:meme/title
                 :meme-auction/token-count
                 :reg-entry/address] :as meme}]
      [:div.form-panel
       [inputs/with-label
        "Amount"
        [inputs/text-input {:form-data form-data
                            :errors errors
                            :id :send/amount
                            :on-click #(.stopPropagation %)
                            :dom-id (str address :send/amount)
                            :type :number
                            :min 1}]
        {:form-data form-data
         :id :send/amount
         :for (str address :send/amount)}]
       [:div.outer
        [inputs/with-label
         "Address"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :send/address
                             :dom-id (str address :send/address)
                             :on-click #(.stopPropagation %)}]
         {:id :send/address
          :form-data form-data
          :for (str address :send/address)}]]

       [:div.buttons
        [:button.cancel {:on-click #(reset! send-sell-atom :sell)} "Cancel"]
        [tx-button/tx-button {:primary true
                              :disabled (boolean (or (not-empty @critical-errors)
                                                     (not (pos? (count @token-ids)))))
                              :class "create-offering"
                              :pending? @tx-pending?
                              :pending-text "Sending..."
                              :on-click (fn []
                                          (dispatch [::meme-token/safe-transfer-from-multi
                                                     (merge @form-data
                                                            {:send-tx/id tx-id
                                                             :meme/title title
                                                             :reg-entry/address address
                                                             :meme-auction/token-ids
                                                             (->> @token-ids
                                                               (take (int (:send/amount @form-data)))
                                                               (map int))})]))}
         "Send"]]])))


(defn sell-form [{:keys [:meme/title :meme-auction/token-count :reg-entry/address] :as meme}
                 {:keys [min-auction-duration max-auction-duration] :as params}
                 send-sell-atom]
  (let [tx-id (str (random-uuid))
        active-account @(subscribe [::accounts-subs/active-account])
        meme-sub (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                     [[:meme/owned-meme-tokens {:owner active-account}
                                                       [:meme-token/token-id]]]]]}])
        token-ids (ratom/reaction (->> @meme-sub
                                       :meme :meme/owned-meme-tokens
                                       (map :meme-token/token-id)))
        form-data (r/atom {:meme-auction/duration 14
                           :meme-auction/amount 1
                           :meme-auction/description ""})

        errors (ratom/reaction (let [{:keys [:meme-auction/start-price :meme-auction/end-price :meme-auction/description]} @form-data
                                     sp (parsers/parse-float start-price)
                                     ep (parsers/parse-float end-price)]
                                 {:local {:meme-auction/amount (cond-> {:hint (str "Max " (count @token-ids))}
                                                                 (and (not (< 0 (parsers/parse-int (:meme-auction/amount @form-data)) (inc (count @token-ids))))
                                                                      (pos? (count @token-ids)))
                                                                 (assoc :error (str "Should be between 1 and " (count @token-ids))))
                                          :meme-auction/start-price (when (not (pos? sp))
                                                                      "Start price should contain a positive value")
                                          :meme-auction/end-price (when (or (not (pos? ep))
                                                                            (< sp ep))
                                                                    {:error "End price should be lower than start price"})
                                          ;; HACK ALERT
                                          ;; TODO: this durations should be specified in days and not in seconds at param level
                                          :meme-auction/duration (let [duration (parsers/parse-int (:meme-auction/duration @form-data))
                                                                       max-duration (-> max-auction-duration
                                                                                      time/seconds->days
                                                                                      int)
                                                                       min-duration (let [md (-> min-auction-duration
                                                                                               time/seconds->days
                                                                                               int)]
                                                                                      (if (zero? md) 1 md))]
                                                                   (cond-> {:hint (str "Max " max-duration)}
                                                                     (not (<= min-duration duration max-duration))
                                                                     (assoc :error (str "Should be between " min-duration " and " max-duration))))}}))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-token/transfer-multi-and-start-auction tx-id}])
        critical-errors (ratom/reaction (inputs/index-by-type @errors :error))]

    (fn []
      [:div.form-panel
       [inputs/with-label
        "Amount"
        [inputs/text-input {:form-data form-data
                            :errors errors
                            :id :meme-auction/amount
                            :on-click #(.stopPropagation %)
                            :dom-id (str address :meme-auction/amount)
                            :type :number
                            :min 1}]
        {:form-data form-data
         :id :meme-auction/amount
         :for (str address :meme-auction/amount)}]
       [:div.outer
        [inputs/with-label
         "Start Price"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :meme-auction/start-price
                             :dom-id (str address :meme-auction/start-price)
                             :on-click #(.stopPropagation %)}]
         {:id :meme-auction/start-price
          :form-data form-data
          :for (str address :meme-auction/start-price)}]
        [:span.unit "ETH"]]
       [:div.outer
        [inputs/with-label
         "End Price"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :meme-auction/end-price
                             :dom-id (str address :meme-auction/end-price)
                             :on-click #(.stopPropagation %)
                             :type :number}]
         {:form-data form-data
          :for (str address :meme-auction/end-price)
          :id :meme-auction/end-price}]
        [:span.unit "ETH"]]
       [:div.outer
        [inputs/with-label
         "Duration"
         [inputs/text-input {:form-data form-data
                             :errors errors
                             :id :meme-auction/duration
                             :dom-id (str address :meme-auction/duration)
                             :on-click #(.stopPropagation %)
                             :type :number
                             :min 1}]
         {:form-data form-data
          :id :meme-auction/duration
          :for (str address :meme-auction/duration)}]
        [:span.unit "days"]]
       [:span.short-sales-pitch "Short sales pitch"]

       [:div.area
        [inputs/textarea-input {:form-data form-data
                                :class "sales-pitch"
                                :errors errors
                                :maxLength 100
                                :id :meme-auction/description
                                :dom-id (str address :meme-auction/description)
                                :on-click #(.stopPropagation %)}]]
       [:div.buttons
        [:button.cancel "Cancel"]
        [tx-button/tx-button {:primary true
                              :disabled (boolean (or (not-empty @critical-errors)
                                                     (not (pos? (count @token-ids)))))
                              :class "create-offering"
                              :pending? @tx-pending?
                              :pending-text "Creating..."
                              :on-click (fn []
                                          (dispatch [::meme-token/transfer-multi-and-start-auction (merge (-> @form-data
                                                                                                              (update :meme-auction/duration time/days->seconds))
                                                                                                          {:send-tx/id tx-id
                                                                                                           :meme/title title
                                                                                                           :reg-entry/address address
                                                                                                           :meme-auction/token-ids (->> @token-ids
                                                                                                                                        (take (int (:meme-auction/amount @form-data)))
                                                                                                                                        (map int))})]))}
         "Create Offering"]]
       [:div.send-tokens {:on-click #(reset! send-sell-atom :send)}
        "Send to a Friend"]])))


(defn collected-tile-back [{:keys [:meme/number :meme/title :meme-auction/token-count :meme-auction/token-ids :reg-entry/address]}]
  (let [params (subscribe [:memefactory.ui.config/memefactory-db-params])
        form (r/atom :sell)]
    (fn []

      [:div.collected-tile-back.meme-card.back {:id "collected-back"}

       (when @params
         (if (= @form :sell)
           [:div.form.sell-form {:id "collected-back-sell"}
            [:h1 (str "Sell " "#" number " " title)]

            [sell-form {:meme/title title
                        :meme-auction/token-ids token-ids
                        :meme-auction/token-count token-count
                        :reg-entry/address address}
             @params
             form]]

           [:div.form.send-form {:id "collected-back-send"}
            [:h1 (str "Send to a friend")]
            [send-form {:meme/title title
                        :meme-auction/token-ids token-ids
                        :meme-auction/token-count token-count
                        :reg-entry/address address}
             form]]))])))


(defmethod panel :collected [_ {:keys [:state :loading-first? :loading-more? :has-more? :re-search]}]
  (let [url-address (-> @(re-frame/subscribe [::router-subs/active-page]) :params :address)
        active-account @(subscribe [::accounts-subs/active-account])]
    (log/debug _ {:c (count state)})
    (if (and (empty? state)
             (not loading-first?))
      [no-items-found]
      [infinite-scroll {:class "tiles"
                        :loading? loading-more?
                        :loading-spinner-delegate (fn []
                                                    [:div.spinner-container [spinner/spin]])
                        :has-more? has-more?
                        :load-fn re-search}
       (when-not loading-first?
         (doall (map (fn [{:keys [:reg-entry/address :reg-entry/status :meme/image-hash :meme/number
                                  :meme/title :meme/total-supply :meme/owned-meme-tokens] :as meme}]
                       (when address
                         (let [token-ids (map :meme-token/token-id owned-meme-tokens)
                               token-count (->> token-ids
                                                (filter cljs-utils/not-nil?)
                                                count)
                               active-user-page? (or (empty? url-address) (= url-address active-account))]
                           [:div.compact-tile {:key address}
                            [tiles/flippable-tile {:front (tiles/meme-image image-hash
                                                                            {:class "collected-tile-front"})
                                                   :back (if active-user-page?
                                                           [collected-tile-back {:meme/number number
                                                                                 :meme/title title
                                                                                 :reg-entry/address address
                                                                                 :meme/owned-meme-tokens owned-meme-tokens
                                                                                 :meme-auction/token-count token-count
                                                                                 :meme-auction/token-ids token-ids}]
                                                           [tiles/meme-back-tile meme])
                                                   :flippable-classes (when active-user-page?
                                                                        #{"meme-image" "cancel" "info" "create-offering"})}]
                            [nav-anchor {:route :route.meme-detail/index
                                         :params {:address address}
                                         :query nil
                                         :class "footer"}
                             [:div.title (str "#" number " " title)]
                             (when (and token-count total-supply)
                               [:div.number-minted (str "Owning " token-count " out of " total-supply)])]])))
                     state)))])))


(defmethod rank :collected [_ active-account]
  (let [query (if active-account
                (subscribe [::gql/query
                            {:queries [[:user {:user/address active-account}
                                        [:user/address
                                         :user/collector-rank
                                         :user/total-collected-memes
                                         :user/total-collected-token-ids
                                         [:user/largest-buy [:meme-auction/bought-for
                                                             [:meme-auction/meme-token
                                                              [:meme-token/token-id
                                                               [:meme-token/meme
                                                                [:meme/title
                                                                 :reg-entry/address
                                                                 :meme/number
                                                                 :meme/image-hash
                                                                 :meme/total-minted]]]]]]]]
                                       [:search-memes [:total-count]]
                                       [:search-meme-tokens [:total-count]]]}])
                (ratom/reaction {:graphql/loading? true}))]
    (let [{:keys [:user/collector-rank :user/total-collected-memes :user/total-collected-token-ids
                  :user/largest-buy]} (:user @query)
          {:keys [:meme-auction/bought-for :meme-auction/meme-token]} largest-buy
          {:keys [:meme-token/token-id :meme-token/meme]} meme-token
          {:keys [:meme/title]} meme
          total-memes-count (-> @query :search-memes :total-count)
          total-meme-tokens-count (-> @query :search-meme-tokens :total-count)]
      [:div.stats.collected
       [:div.rank
        (str "RANK: #" collector-rank) ]
       [:div.var
        [:b "Unique Memes: "]
        (when (and total-collected-memes total-memes-count)
          (str total-collected-memes "/" total-memes-count))]
       [:div.var
        [:b "Total Cards: "]
        (when (and total-collected-token-ids total-meme-tokens-count)
          [:span (str total-collected-token-ids "/" total-meme-tokens-count)])]
       [:div.var
        [:b "Largest buy: "]
        (if (and bought-for token-id title)
          [nav-anchor {:route :route.meme-detail/index
                       :params {:address (:reg-entry/address meme)}
                       :query nil}
           [:span (gstring/format "%f (#%d %s)"
                                  (ui-utils/format-price bought-for)
                                  (:meme/number meme)
                                  (:meme/title meme))]]
          [:span "None"])]])))


(defmethod total :collected [_ active-account]
  (let [query (subscribe [::gql/query {:queries [[:search-memes {:owner active-account}
                                                  [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      [:div.total "Total "  (get-in @query [:search-memes :total-count])])))


(defn issue-form [{:keys [:meme/title :reg-entry/address :meme/total-supply :meme/total-minted]}]
  (let [tx-id (str (random-uuid))
        max-amount (- total-supply total-minted)
        form-data (r/atom {:meme/amount max-amount}) ;; TODO fix how can we refresh this when component updated
        errors (ratom/reaction {:local (let [{:keys [:amount]} @form-data]
                                         (when (and (not (js/isNaN amount))
                                                    (int? amount)
                                                    (<= amount max-amount))
                                           {:meme/amount {:error (str "Amount should be an integer, and smaller than " max-amount)}}))})
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme/mint tx-id}])]
    (fn [{:keys [:meme/title :reg-entry/address :meme/total-supply :meme/total-minted]}]
      (let [max-amount (- total-supply total-minted)]
        [:div.issue-form
         [:div.field
          [inputs/int-input {:form-data form-data
                             :errors errors
                             :id :meme/amount}]
          [tx-button/tx-button {:primary true
                                :disabled (not (pos? max-amount))
                                :pending? @tx-pending?
                                :pending-text "Issuing"
                                :on-click (fn []
                                            (dispatch [::meme/mint (merge @form-data
                                                                          {:meme/title title
                                                                           :reg-entry/address address
                                                                           :send-tx/id tx-id})]))}
           "Issue"]]
         [:div.label (if (pos? max-amount)
                       (str "Max " max-amount)
                       "All cards issued")]]))))


(defmethod panel :created [_ {:keys [:state :loading-first? :loading-more? :has-more? :re-search]}]
  (let [url-address (-> @(re-frame/subscribe [::router-subs/active-page]) :params :address)
        active-account @(subscribe [::accounts-subs/active-account])]
    (log/debug _ {:c (count state)})
    (if (and (empty? state)
             (not loading-first?))
      [no-items-found]
      [infinite-scroll {:class "tiles"
                        :loading? loading-more?
                        :has-more? has-more?
                        :loading-spinner-delegate (fn []
                                                    [:div.spinner-container [spinner/spin]])
                        :load-fn re-search}
       (when-not loading-first?
         (doall (map (fn [{:keys [:reg-entry/address :meme/image-hash :meme/number
                                  :meme/title :meme/total-supply :meme/total-minted
                                  :reg-entry/status] :as meme}]
                       (when address
                         (let [status (gql-utils/gql-name->kw (or status :undefined))
                               rejected? (= status :reg-entry.status/blacklisted)]
                           ^{:key address} [:div.compact-tile
                                            [:div.container
                                             [tiles/meme-image image-hash {:rejected? rejected?}]]
                                            [nav-anchor {:route :route.meme-detail/index
                                                         :params {:address address}
                                                         :query nil
                                                         :class "footer"}
                                             [:div.title (str (when number (str "#" number " ")) title)]
                                             [:div.issued (str total-minted "/" total-supply" Issued")]
                                             [:div.status
                                              (case status
                                                :reg-entry.status/challenge-period "In Challenge Period"
                                                :reg-entry.status/whitelisted "In Registry"
                                                :reg-entry.status/blacklisted "Rejected"
                                                "Challenged")]]
                                            (when (and (or (empty? url-address)
                                                           (= url-address active-account))
                                                       (= status :reg-entry.status/whitelisted))
                                              [issue-form {:meme/title title
                                                           :reg-entry/address address
                                                           :meme/total-supply total-supply
                                                           :meme/total-minted total-minted}])])))
                     state)))])))


(defmethod rank :created [_ active-account]
  (let [query (if active-account
                (subscribe [::gql/query
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
                                                                 [:meme/title
                                                                  :reg-entry/address]]]]]]]]]}])
                (ratom/reaction {:graphql/loading? true}))]
    (let [{:keys [:user/total-created-memes :user/total-created-memes-whitelisted :user/creator-rank :user/largest-sale]} (-> @query :user)
          {:keys [:meme-auction/meme-token]} largest-sale
          {:keys [:meme-token/number :meme-token/meme]} meme-token
          {:keys [:meme/title]} meme
          meme-auctions (-> @query :search-meme-auctions :items)
          creator-total-earned (when meme-auctions
                                 (reduce (fn [total-earned {:keys [:meme-auction/end-price] :as meme-auction}]
                                           (+ total-earned end-price))
                                         0
                                         meme-auctions))]
      [:div.stats.created
       [:div.rank
        (str "RANK: #" creator-rank)]
       [:div.var
        [:b "Earned: "] (ui-utils/format-price creator-total-earned)]
       [:div.var
        [:b "Success Rate: "]
        (when (and total-created-memes-whitelisted total-created-memes)
          (str total-created-memes-whitelisted "/" total-created-memes " ("
               (format/format-percentage total-created-memes-whitelisted total-created-memes) ")"))]
       [nav-anchor {:route :route.meme-detail/index
                    :params {:address (:reg-entry/address meme)}
                    :query nil
                    :class "var best-card-sale"}
        [:b "Best Single Card Sale: "]
        (if (and largest-sale number title)
          (str (ui-utils/format-price (:meme-auction/bought-for largest-sale))
               " (#" number " " title ")")
          "None")]])))


(defmethod total :created [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:creator active-account}
                                      [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      [:div.total "Total " (get-in @query [:search-memes :total-count])])))


(defmethod panel :curated [_ {:keys [:state :loading-first? :loading-more? :has-more? :re-search]}]
  (log/debug _ {:c (count state)})
  (if (and (empty? state)
           (not loading-first?))
    [no-items-found]
    [infinite-scroll {:class "tiles"
                      :loading? loading-more?
                      :has-more? has-more?
                      :loading-spinner-delegate (fn []
                                                  [:div.spinner-container [spinner/spin]])
                      :load-fn re-search}
     (when-not loading-first?
       (doall
        (map (fn [{:keys [:reg-entry/address :reg-entry/status
                          :meme/image-hash :meme/number
                          :meme/title :challenge/vote] :as meme}]
               (when address
                 (let [{:keys [:vote/option]} vote
                       status (gql-utils/gql-name->kw (or status :undefined))
                       rejected? (= status :reg-entry.status/blacklisted)]
                   ^{:key address}
                   [:div.compact-tile
                    [:div.container
                     [:div.meme-card-front
                      [tiles/meme-image image-hash {:rejected? rejected?}]]]
                    [nav-anchor {:route :route.meme-detail/index
                                 :params {:address address}
                                 :query nil
                                 :class "footer"}
                     [:div.title [:b (str (when number (str "#" number " ")) title)]]
                     [:div.vote-option
                      (cond
                        (= option (gql-utils/kw->gql-name :vote-option/not-revealed))
                        [:label.not-revealed "Vote - Unrevealed"]

                        (= option (gql-utils/kw->gql-name :vote-option/vote-for))
                        [:label.vote-dank "Voted Dank"]

                        (= option (gql-utils/kw->gql-name :vote-option/vote-against))
                        [:label.vote-stank "Voted Stank"])]]])))
             state)))]))


(defmethod rank :curated [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:user {:user/address active-account}
                                      [:user/curator-rank
                                       :user/total-created-challenges
                                       :user/total-created-challenges-success
                                       :user/challenger-total-earned
                                       :user/total-participated-votes
                                       :user/total-participated-votes-success
                                       :user/voter-total-earned]]]}])]
    (let [{:keys [:user/curator-rank :user/total-created-challenges :user/total-created-challenges-success
                  :user/challenger-total-earned :user/total-participated-votes :user/total-participated-votes-success
                  :user/voter-total-earned]} (:user @query)]
      [:div.stats
       [:div.rank.rank--big
        (str "RANK: #" curator-rank)]
       [:div.curator
        [:div
         [:div.label "CHALLENGES:"]
         [:div [:b "Success Rate:"]
          (when (and total-created-challenges-success total-created-challenges)
            (str total-created-challenges-success "/" total-created-challenges
                 " (" (format/format-percentage total-created-challenges-success total-created-challenges)  ")"))]
         [:div [:b "Earned:"]
          (when challenger-total-earned
            (ui-utils/format-dank challenger-total-earned))]]
        [:div
         [:div.label "VOTES:"]
         [:div [:b "Success Rate:"]
          (when (and total-participated-votes total-participated-votes-success)
            (str total-participated-votes-success "/" total-participated-votes
                 " (" (format/format-percentage total-participated-votes-success total-participated-votes)  ")"))]
         [:div [:b "Earned:"]
          (when voter-total-earned
            (ui-utils/format-dank voter-total-earned))]]
        [:div
         [:div.label "TOTAL-EARNINGS:"]
         [:div
          (when (and challenger-total-earned voter-total-earned)
            (ui-utils/format-dank (+ challenger-total-earned voter-total-earned)))]]]])))


(defmethod total :collected [_ active-account]
  (let [query (subscribe [::gql/query {:queries [[:search-memes {:owner active-account}
                                                  [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      (let [total (get-in @query [:search-memes :total-count])]
        [:div.total "Total " total]))))


(defmethod total :created [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:creator active-account}
                                      [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      (let [total (get-in @query [:search-memes :total-count])]
        [:div.total "Total " total]))))


(defmethod total :curated [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:curator active-account}
                                      [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      (let [total (get-in @query [:search-memes :total-count])]
        [:div.total "Total " total]))))


(defmethod total :selling [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/active]}
                                      [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      (let [total (get-in @query [:search-meme-auctions :total-count])]
        [:div.total "Total " total]))))


(defmethod total :sold [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/done]}
                                      [:total-count]]]}])]
    (let [total (get-in @query [:search-meme-auctions :total-count])]
      [:div.total (str "Total " total)])))


(defmethod panel :sold [_ {:keys [:state :loading-first? :loading-more? :has-more? :re-search]}]
  (log/debug _ {:c (count state)})
  (if (and (empty? state)
           (not loading-first?))
    [no-items-found]
    [infinite-scroll {:class "tiles"
                      :loading? loading-more?
                      :has-more? has-more?
                      :loading-spinner-delegate (fn []
                                                  [:div.spinner-container [spinner/spin]])
                      :load-fn re-search}
     (when-not loading-first?
       (doall
        (map (fn [{:keys [:meme-auction/address :meme-auction/bought-for]
                   {:keys [:meme-token/number]
                    {:keys [:meme/title :meme/total-minted] :as meme} :meme-token/meme :as meme-token} :meme-auction/meme-token
                   :as meme-auction}]
               [:div.compact-tile {:key address}
                [tiles/flippable-tile {:front [tiles/meme-image (get-in meme-token [:meme-token/meme :meme/image-hash])]
                                       :back [:div.meme-card
                                              [:div.overlay
                                               [:div.logo
                                                [:img {:src "/assets/icons/mf-logo.svg"}]]
                                               [nav-anchor {:route :route.meme-detail/index
                                                            :params {:address (:reg-entry/address meme)}
                                                            :query nil
                                                            :class "details-button"}
                                                [:span "View Details"]]
                                               [:ul.meme-data
                                                [:li [:label "Buyer:"]

                                                 [nav-anchor {:route :route.memefolio/index
                                                              :params {:address (:user/address (:meme-auction/buyer meme-auction))}
                                                              :query {:tab :collected}}
                                                  (:user/address (:meme-auction/buyer meme-auction))]]
                                                [:li [:label "Price:"] [:span (ui-utils/format-price (:meme-auction/bought-for meme-auction))]]
                                                [:li [:label "Bought:"] [:span (let [time-ago (format/time-ago (gql-utils/gql-date->date (:meme-auction/bought-on meme-auction))
                                                                                                               (t/date-time @(subscribe [::now-subs/now])))]
                                                                                 time-ago)]]]
                                               [:hr]
                                               [:p.description (:meme-auction/description meme-auction)]]]}]


                [nav-anchor {:route :route.meme-detail/index
                             :params {:address (-> meme-token :meme-token/meme :reg-entry/address) }
                             :query nil
                             :class "footer"}
                 [:div.meme-number (str "#" (:meme/number meme))]
                 [:div.title title]
                 [:div.number-minted (str number "/" total-minted)]
                 [:div.price (ui-utils/format-price bought-for)]]])
             state)))]))


(defn build-query [tab {:keys [:user-address :form-data :prefix :first :after] :as opts}]
  (let [{:keys [:term :order-by :order-dir :search-tags :group-by-memes? :voted? :challenged? :option-filters]} form-data]
    (case tab
      :collected [[:search-memes (merge {:owner user-address
                                         :first first}
                                        (when after
                                          {:after (str after)})
                                        (when term
                                          {:title term})
                                        {:order-by (ui-utils/build-order-by :memes (or order-by :created-on))
                                         :order-dir (or (ui-utils/order-dir-from-key-name order-by collected-created-order) :desc)}
                                        (when search-tags
                                          {:tags search-tags}))
                   [:total-count
                    :end-cursor
                    :has-next-page
                    [:items (remove nil? [:reg-entry/address
                                          :reg-entry/status
                                          :reg-entry/created-on
                                          :meme/image-hash
                                          :meme/meta-hash
                                          :meme/number
                                          :meme/title
                                          :meme/total-supply
                                          :meme/total-minted
                                          :meme/total-trade-volume
                                          :meme/average-price
                                          :meme/highest-single-sale
                                          [:reg-entry/creator [:user/address]]
                                          [:meme/owned-meme-tokens {:owner user-address}
                                           [:meme-token/token-id]]])]]]]
      :created [[:search-memes (merge {:creator user-address
                                       :first first}
                                      (when after
                                        {:after (str after)})
                                      (when term
                                        {:title term})
                                      {:order-by (ui-utils/build-order-by :memes (or order-by :created-on))
                                       :order-dir (or (ui-utils/order-dir-from-key-name order-by collected-created-order) :desc)}
                                      (when search-tags
                                        {:tags search-tags}))
                 [:total-count
                  :end-cursor
                  :has-next-page
                  [:items [:reg-entry/address
                           :meme/image-hash
                           :meme/meta-hash
                           :meme/number
                           :meme/title
                           :meme/total-minted
                           :meme/total-supply
                           :reg-entry/status]]]]]
      :curated [[:search-memes (merge {:curator user-address
                                       :first first}
                                      (cond
                                        (and voted? challenged?) {:curator user-address}
                                        voted?                   {:voter user-address}
                                        challenged?              {:challenger user-address})
                                      (when after
                                        {:after (str after)})
                                      (when term
                                        {:title term})
                                      {:order-by (ui-utils/build-order-by :memes (or order-by :created-on))
                                       :order-dir (or (ui-utils/order-dir-from-key-name order-by curated-order) :desc)}
                                      (when search-tags
                                        {:tags search-tags}))
                 [:total-count
                  :end-cursor
                  :has-next-page
                  [:items [:reg-entry/address
                           :meme/image-hash
                           :meme/meta-hash
                           :meme/number
                           :meme/title
                           :reg-entry/status
                           [:challenge/vote {:vote/voter user-address}
                            [:vote/option]]]]]]]
      :selling [[:search-meme-auctions (merge {:seller user-address
                                               :statuses [:meme-auction.status/active]
                                               :first first}
                                              (when after
                                                {:after (str after)})
                                              (when term
                                                {:title term})
                                              {:order-by (ui-utils/build-order-by :meme-auctions (or order-by :started-on))
                                               :order-dir (or (ui-utils/order-dir-from-key-name order-by selling-sold-order) :desc)}
                                              (when search-tags
                                                {:tags search-tags})
                                              (when (#{:only-lowest-number :only-cheapest} option-filters)
                                                {:group-by (get {:only-lowest-number :meme-auctions.group-by/lowest-card-number
                                                                 :only-cheapest :meme-auctions.group-by/cheapest}
                                                                option-filters)}))
                 [:total-count
                  :end-cursor
                  :has-next-page
                  [:items [:meme-auction/address
                           :meme-auction/status
                           :meme-auction/start-price
                           :meme-auction/end-price
                           :meme-auction/bought-for
                           :meme-auction/started-on
                           :meme-auction/duration
                           :meme-auction/description
                           [:meme-auction/seller [:user/address]]
                           [:meme-auction/meme-token
                            [:meme-token/number
                             [:meme-token/meme
                              [:reg-entry/address
                               :meme/title
                               :meme/number
                               :meme/image-hash
                               :meme/meta-hash
                               :meme/total-minted]]]]]]]]]
      :sold [[:search-meme-auctions (merge {:seller user-address
                                            :statuses [:meme-auction.status/done]
                                            :first first}
                                           (when after
                                             {:after (str after)})
                                           (when term
                                             {:title term})
                                           {:order-by (ui-utils/build-order-by :meme-auctions (or order-by :started-on))
                                            :order-dir (or (ui-utils/order-dir-from-key-name order-by selling-sold-order) :desc)}
                                           (when search-tags
                                             {:tags search-tags}))
              [:total-count
               :end-cursor
               :has-next-page
               [:items [:meme-auction/address
                        :meme-auction/status
                        :meme-auction/start-price
                        :meme-auction/end-price
                        :meme-auction/bought-for
                        :meme-auction/bought-on
                        :meme-auction/description
                        [:meme-auction/buyer [:user/address]]
                        [:meme-auction/meme-token
                         [:meme-token/number
                          [:meme-token/meme
                           [:reg-entry/address
                            :meme/title
                            :meme/image-hash
                            :meme/meta-hash
                            :meme/number
                            :meme/total-minted]]]]]]]]])))


(defn scrolling-container [tab {:keys [:user-address :form-data :prefix]}]
  (let [query (build-query tab {:user-address user-address
                                :prefix prefix
                                :form-data @form-data
                                :first page-size})
        query-id (merge @form-data {:tab tab :user-address user-address})
        query-sub (subscribe [::gql/query {:queries query}
                              {:id query-id}])
        k (case prefix
            :memes :search-memes
            :meme-auctions :search-meme-auctions)
        re-search (fn [after]
                    (dispatch [::gql-events/query
                               {:query {:queries (build-query tab {:user-address user-address
                                                                   :prefix prefix
                                                                   :form-data @form-data
                                                                   :first page-size
                                                                   :after after})}
                                :id query-id}]))

        state (mapcat (fn [q] (get-in q [k :items])) @query-sub)
        loading-first? (:graphql/loading? (first @query-sub))
        last-sub (last @query-sub)
        loading? (:graphql/loading? last-sub)
        has-more? (-> last-sub k :has-next-page)
        end-cursor (-> last-sub k :end-cursor)]

    ;; (log/debug "#memes" {:c (count state)})

    [:div.scroll-area
     [panel tab
      {:state state :query-key k :loading-first? loading-first? :loading-more? loading? :has-more? has-more?
       :re-search #(re-search end-cursor)
       :form-data form-data}]]))


(defn tabbed-pane [{:keys [:tab :prefix :form-data]
                    {:keys [:user-address :url-address?]} :user-account}]
  (let [tags (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])]
    [:div.tabbed-pane
     [:section.search-form
      [search/search-tools (merge {:title (if url-address?
                                            [:span (str "Memefolio " user-address)]
                                            [:span "My Memefolio" (let [loc-str (str (.-location js/window))
                                                                        loc (if-let [qidx (str/index-of loc-str "?")]
                                                                              (subs loc-str 0 qidx)
                                                                              loc-str)
                                                                        user-link (str loc user-address "?tab=" (name tab))]
                                                                    [:img.copy
                                                                     {:on-click
                                                                      (fn []
                                                                        (copy-to-clipboard! user-link)
                                                                        (dispatch [:district.ui.notification.events/show
                                                                                   "Your Memefolio URL was copied to clipboard!"]))
                                                                      :title "Copy shareable URL to clipboard"
                                                                      :src "/assets/icons/link.svg"}])])
                                   :form-data form-data
                                   ;; :on-selected-tags-change re-search
                                   ;; :on-search-change re-search
                                   ;; :on-check-filter-change re-search
                                   ;; :on-select-change re-search
                                   :tags (->> @tags :search-tags :items (mapv :tag/name))
                                   :selected-tags-id :search-tags
                                   :search-id :term
                                   :sub-title (if url-address?
                                                (str "User stats and current holdings for account " user-address)
                                                "A personal history of all memes bought, sold, created, and owned")
                                   :select-options (cond
                                                     (#{:collected :created} tab) collected-created-order
                                                     (#{:curated}            tab) curated-order
                                                     (#{:selling :sold}      tab) selling-sold-order)}
                                  (when (= :curated tab)
                                    {:check-filters [{:label "Voted"
                                                      :id :voted?}
                                                     {:label "Challenged"
                                                      :id :challenged?}]}))]]
     [:section.tabs
      (doall
       (map (fn [tab-id]
              ^{:key tab-id} [:div.tab
                              {:class (when (= tab
                                               tab-id) "selected")}

                              [nav-anchor {:route :route.memefolio/index
                                           :params (if url-address?
                                                     {:address user-address}
                                                     {})
                                           :query {:tab tab-id}}
                               (-> tab-id
                                   cljs.core/name
                                   (str/capitalize))]])
            [:collected :created :curated :selling :sold]))
      [total tab user-address]]
     [:section.stats
      (when (not (contains? #{:selling :sold} tab))
        [:div.rank
         [rank tab user-address]])]
     [:div.panel
      {:class (name tab)}
      [scrolling-container tab {:user-address user-address :form-data form-data :prefix prefix}]]]))


(defmethod page :route.memefolio/index []
  (let [{:keys [:query]} @(subscribe [::router-subs/active-page])
        active-tab (or (keyword (:tab query)) default-tab)
        active-account @(subscribe [::accounts-subs/active-account])
        active-page-sub (re-frame/subscribe [::router-subs/active-page])
        url-account (-> @active-page-sub :params :address)
        ;; by default whole page uses web3 provided account, overrides with url &address=<address> argument if present
        user-account (match [(nil? url-account) (nil? active-account)]
                            [true _] active-account
                            [false _] url-account
                            [true true] nil)]
    (when user-account
      (let [prefix (cond (contains? #{:collected :created :curated} active-tab)
                         :memes
                         (contains? #{:selling :sold} active-tab)
                         :meme-auctions)
            form-data (r/atom {:term (:term query)
                               :voted? true
                               :challenged? true
                               :option-filters :only-lowest-number})]
        [app-layout
         {:meta {:title (if url-account
                          (str "MemeFactory - Memefolio " user-account)
                          "MemeFactory - My Memefolio")
                 :description "A personal history of all memes bought, sold, created, and owned. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.memefolio-page
          ^{:key user-account}
          [tabbed-pane {:tab active-tab :prefix prefix :form-data form-data :user-account {:user-address user-account
                                                                                           :url-address? url-account}}]]]))))
