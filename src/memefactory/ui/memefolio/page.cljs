(ns memefactory.ui.memefolio.page
  (:require [cljs-web3.core :as web3]
            [cljs.core.match :refer-macros [match]]
            [clojure.data :as data]
            [clojure.string :as str]
            [district.format :as format]
            [district.graphql-utils :as graphql-utils]
            [district.time :as time]
            [district.ui.component.form.input :as inputs]
            [district.ui.component.page :refer [page]]
            [district.ui.component.tx-button :as tx-button]
            [district.ui.graphql.events :as gql-events]
            [district.ui.graphql.subs :as gql]
            [district.ui.router.events :as router-events]
            [district.ui.router.subs :as router-subs]
            [district.ui.now.subs :as now-subs]
            [district.ui.web3-accounts.subs :as accounts-subs]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [memefactory.shared.utils :as shared-utils]
            [memefactory.ui.components.app-layout :as app-layout]
            [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
            [memefactory.ui.components.panels :refer [panel]]
            [memefactory.ui.components.search :as search]
            [memefactory.ui.components.tiles :as tiles]
            [print.foo :refer [look] :include-macros true]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [reagent.ratom :as ratom]
            [memefactory.ui.contract.meme :as meme]
            [memefactory.ui.contract.meme-token :as meme-token]))

(def default-tab :collected)

(def scroll-interval 5)

(defmulti rank (fn [tab & opts] tab))

(defmulti total (fn [tab & opts] tab))

(defn sell-form [{:keys [:meme/title
                         :meme-auction/token-count
                         :meme-auction/token-ids
                         :reg-entry/address
                         :show?]}]
  (let [response (subscribe [::gql/query {:queries [[:search-param-changes {:key (graphql-utils/kw->gql-name :max-auction-duration)
                                                                            :db (graphql-utils/kw->gql-name :meme-registry-db)
                                                                            :group-by :param-changes.group-by/key
                                                                            :order-by :param-changes.order-by/applied-on}
                                                     [[:items [:param-change/value]]]]]}])]

    (when-not (:graphql/loading? @response)
      (let [max-duration (-> @response
                             (get-in [:search-param-changes :items])
                             first
                             :param-change/value)
            tx-id (str (random-uuid))
            form-data (r/atom {:meme-auction/amount token-count
                               :meme-auction/start-price 0.1
                               :meme-auction/end-price 0.01
                               :meme-auction/duration max-duration
                               :meme-auction/description ""})
            errors (ratom/reaction {:local {:meme-auction/amount {:hint (str "Max " token-count)
                                                                  :error (when-not (< 0 (js/parseInt (:meme-auction/amount @form-data)) (inc token-count))
                                                                           (str "Should be between 0 and " token-count))}
                                            :meme-auction/duration {:hint (str "Max " max-duration)
                                                                    :error (when-not (< 0 (js/parseInt (:meme-auction/duration @form-data)) max-duration)
                                                                             (str "Should be less than " max-duration))}}})
            tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-token/transfer-multi-and-start-auction tx-id}])]
        (fn []
          [:div.form-panel
           [inputs/text-input {:form-data form-data
                               :placeholder "Amount"
                               :errors errors
                               :id :meme-auction/amount
                               :on-click #(.stopPropagation %)}]
           [inputs/amount-input {:form-data form-data
                                 :placeholder "Start Price"
                                 :errors errors
                                 :id :meme-auction/start-price
                                 :on-click #(.stopPropagation %)}]
           [inputs/amount-input {:form-data form-data
                                 :placeholder "End Price"
                                 :errors errors
                                 :id :meme-auction/end-price
                                 :on-click #(.stopPropagation %)}]
           [inputs/int-input {:form-data form-data
                              :placeholder "Duration"
                              :errors errors
                              :id :meme-auction/duration
                              :on-click #(.stopPropagation %)}]
           [inputs/textarea-input {:form-data form-data
                                   :class "sales-pitch"
                                   :placeholder "Short sales pitch"
                                   :errors errors
                                   :id :meme-auction/description
                                   :on-click #(.stopPropagation %)}]
           [:div.buttons
            [:button.cancel {:on-click #(swap! show? not)} "Cancel"]
            [tx-button/tx-button {:primary true
                                  :disabled false
                                  :class "create-offering"
                                  :pending? @tx-pending?
                                  :pending-text "Creating offering..."
                                  :on-click (fn []
                                              (dispatch [::meme-token/transfer-multi-and-start-auction (merge @form-data
                                                                                                             {:send-tx/id tx-id
                                                                                                              :meme/title title
                                                                                                              :reg-entry/address address
                                                                                                              :meme-auction/token-ids (->> token-ids
                                                                                                                                           (take (int (:meme-auction/amount @form-data)))
                                                                                                                                           (map int))})]))}
             "Create Offering"]]])))))

(defn collected-tile-back [{:keys [:meme/number :meme/title :meme-auction/token-count :meme-auction/token-ids :reg-entry/address]}]
  (let [sell? (r/atom false)]
    (fn []
      [:div.collected-tile-back.meme-card.back
       (if-not @sell?
         [:div.sell
          [:div.top
           [:b (str "#" number)]
           [:img {:src "/assets/icons/mememouth.png"}]]
          [:div.bottom
           [:button {:on-click (fn [e]
                                 (.stopPropagation e)
                                 (swap! sell? not))}
            "Sell"]]]
         [:div.sell-form
          [:h1 (str "Sell" "#" number " " title)]
          [sell-form {:meme/title title
                      :meme-auction/token-ids token-ids
                      :meme-auction/token-count token-count
                      :reg-entry/address address
                      :show? sell?}]])])))

(defmethod panel :collected [_ state]
  [:div.tiles
   (doall (map (fn [{:keys [:reg-entry/address :reg-entry/status :meme/image-hash :meme/number
                            :meme/title :meme/total-supply :meme/owned-meme-tokens] :as meme}]
                 (when address
                   (let [token-ids (map :meme-token/token-id owned-meme-tokens)
                         token-count (->> token-ids
                                          (filter shared-utils/not-nil?)
                                          count)]
                     ^{:key address} [:div.compact-tile
                                      [tiles/flippable-tile {:front (tiles/meme-image image-hash)
                                                             :back [collected-tile-back {:meme/number number
                                                                                         :meme/title title
                                                                                         :reg-entry/address address
                                                                                         :meme/owned-meme-tokens owned-meme-tokens
                                                                                         :meme-auction/token-count token-count
                                                                                         :meme-auction/token-ids token-ids}]}]
                                      [:div.footer
                                       {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                                              nil
                                                              {:reg-entry/address address}])}
                                       [:div.title (str "#" number " " title)]
                                       (when (and token-count total-supply)
                                         [:div.number-minted (str "Owning " token-count " out of " total-supply)])]])))
               state))])

(defmethod rank :collected [_ active-account]
  (let [query (if active-account
                (subscribe [::gql/query
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
                                       [:search-meme-tokens [:total-count]]]}])
                (ratom/reaction {:graphql/loading? true}))]
    (let [{:keys [:user/collector-rank :user/total-collected-memes :user/total-collected-token-ids
                  :user/largest-buy]} (:user @query)
          {:keys [:meme-auction/bought-for :meme-auction/meme-token]} largest-buy
          {:keys [:meme-token/number :meme-token/meme]} meme-token
          {:keys [:meme/title]} meme
          total-memes-count (-> @query :search-memes :total-count)
          total-meme-tokens-count (-> @query :search-meme-tokens :total-count)]
      [:div.stats
       [:div.rank
        (str "RANK: ") (or collector-rank [:div.loading])]
       [:div.var
        [:b "Unique Memes: "]
        (if (and total-collected-memes total-memes-count)
          (str total-collected-memes "/" total-memes-count)
          [:div.loading])]
       [:div.var
        [:b "Total Cards: "]
        (if (and total-collected-token-ids total-meme-tokens-count)
          [:span (str total-collected-token-ids "/" total-meme-tokens-count)]
          [:div.loading])]
       [:div.var
        [:b "Largest buy: "]
        (if (and bought-for number title)
          [:span (str (format/format-eth (web3/from-wei bought-for :ether))
                      " (#" number " " title ")")]
          [:div.loading])]])))

(defmethod total :collected [_ active-account]
  (let [query (subscribe [::gql/query {:queries [[:search-memes {:owner active-account}
                                                  [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      [:div "Total "  (get-in @query [:search-memes :total-count])])))

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
          [inputs/text-input {:form-data form-data
                              :errors errors
                              :id :meme/amount}]
          [tx-button/tx-button {:primary true
                                :disabled (not (pos? max-amount))
                                :pending? @tx-pending?
                                :pending-text "Issuing..."
                                :on-click (fn []
                                            (dispatch [::meme/mint (merge @form-data
                                                                          {:meme/title title
                                                                           :reg-entry/address address
                                                                           :send-tx/id tx-id})]))}
           "Issue"]]
         [:label "Max " max-amount]]))))

(defmethod panel :created [_ state]
  [:div
   (doall (map (fn [{:keys [:reg-entry/address :meme/image-hash :meme/number
                            :meme/title :meme/total-supply :meme/total-minted
                            :reg-entry/status] :as meme}]
                 (when address
                   (let [status (graphql-utils/gql-name->kw status)]
                     ^{:key address} [:div.compact-tile
                                      [:div.container
                                       [tiles/meme-image image-hash]
                                       #_[tiles/meme-front-tile {} meme]]
                                      [:a.footer {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                                                        nil
                                                                        {:reg-entry/address address}])}
                                       [:div [:b (str "#" number " " title)]]
                                       [:div [:span (str total-minted "/" total-supply" Issued")]]
                                       [:div
                                        (case status
                                          :reg-entry.status/whitelisted [:label [:b "In Registry"]]
                                          :reg-entry.status/blacklisted [:label [:b "Rejected"]]
                                          [:label [:b "Challenged"]])]]
                                      (when (= status :reg-entry.status/whitelisted)
                                        [issue-form {:meme/title title
                                                     :reg-entry/address address
                                                     :meme/total-supply total-supply
                                                     :meme/total-minted total-minted}])])))
               state))])

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
                                                                 [:meme/title]]]]]]]]]}])
                (ratom/reaction {:graphql/loading? true}))]

    #_(prn _ active-account @query)

    (let [{:keys [:user/total-created-memes :user/total-created-memes-whitelisted :user/creator-rank :user/largest-sale]} (-> @query :user)
          {:keys [:meme-auction/meme-token]} largest-sale
          {:keys [:meme-token/number :meme-token/meme]} meme-token
          {:keys [:meme/title]} meme
          creator-total-earned (reduce (fn [total-earned {:keys [:meme-auction/end-price] :as meme-auction}]
                                         (+ total-earned end-price))
                                       0
                                       (-> @query :search-meme-auctions :items))]
      [:div.stats
       [:div.rank
        (str "RANK: ") (or creator-rank [:div.loading])]
       [:div.var
        [:b "Earned: "]
        (if-not (:graphql/loading? @query) #_creator-total-earned
          (format/format-eth (web3/from-wei creator-total-earned :ether))
          [:div.loading])]
       [:div.var
        [:b "Success Rate: "]
        (if (and total-created-memes-whitelisted total-created-memes)
          (str total-created-memes-whitelisted "/" total-created-memes " ("
               (format/format-percentage total-created-memes-whitelisted total-created-memes) ")")
          [:div.loading])]
       [:div.var
        [:b "Best Single Card Sale: "]
        (if (and largest-sale number title)
          (str (-> (:meme-auction/end-price largest-sale)
                   (web3/from-wei :ether)
                   format/format-eth)
               " (#" number " " title ")")
          [:div.loading])]])))

(defmethod total :created [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:creator active-account}
                                      [:total-count]]]}])]
    (when-not (:graphql/loading? @query)
      [:div "Total " (get-in @query [:search-memes :total-count])])))

(defmethod panel :curated [_ state]
  [:div.tiles
   (doall
    (map (fn [{:keys [:reg-entry/address :meme/image-hash :meme/number
                      :meme/title :challenge/vote] :as meme}]
           (when address
             (let [{:keys [:vote/option]} vote]
               ^{:key address} [:div.meme-card-front
                                [tiles/meme-image image-hash]
                                [:a {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                                           nil
                                                           {:reg-entry/address address}])}
                                 [:div [:b (str "#" number " " title)]]
                                 [:div
                                  (cond
                                    (= option (graphql-utils/kw->gql-name :vote-option/no-vote))
                                    [:label
                                     [:b "Voted Unrevealed"]]

                                    (= option (graphql-utils/kw->gql-name :vote-option/vote-for))
                                    [:label "Voted Dank"
                                     [:i.icon.thumbs.up.outline]]

                                    (= option (graphql-utils/kw->gql-name :vote-option/vote-against))
                                    [:label
                                     [:b "Voted Stank"]
                                     [:i.icon.thumbs.down.outline]])]]])))
         state))])

;; TODO : style spinners
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
       [:div.rank.big
        (str "RANK: ") (or curator-rank [:div.loading])]
       [:div.curator
        [:div
         [:div.label "CHALLENGES:"]
         [:div [:b "Success Rate:"]
          (if (and total-created-challenges-success total-created-challenges)
              (str total-created-challenges-success "/" total-created-challenges
                    " (" (format/format-percentage total-created-challenges-success total-created-challenges)  ")")
              [:div.loading])]
         [:div [:b "Earned:"]
          (if challenger-total-earned
              (str (web3/from-wei challenger-total-earned :ether) " DANK")
              [:div.loading])]]
        [:div
         [:div.label "VOTES:"]
         [:div [:b "Success Rate:"]
          (if (and total-participated-votes total-participated-votes-success)
              (str total-participated-votes "/" total-participated-votes-success
                   " (" (format/format-percentage total-participated-votes-success total-participated-votes)  ")")
              [:div.loading])]
         [:div [:b "Earned:"]
          (if voter-total-earned
            (str (web3/from-wei voter-total-earned :ether) " DANK")
            [:div.loading])]]
        [:div
         [:div.label "TOTAL-EARNINGS:"]
         [:div
          (if (and challenger-total-earned voter-total-earned)
              (str (web3/from-wei (+ challenger-total-earned voter-total-earned) :ether) " DANK")
              [:div.loading])]]]])))

(defmethod total :collected [_ active-account]
  (let [query (subscribe [::gql/query {:queries [[:search-memes {:owner active-account}
                                                  [:total-count]]]}])]
    (let [total (get-in @query [:search-memes :total-count])]
      [:div "Total " (or total [:div.loading])])))

(defmethod total :created [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:creator active-account}
                                      [:total-count]]]}])]
    (let [total (get-in @query [:search-memes :total-count])]
      [:div "Total " (or total [:div.loading])])))

(defmethod total :curated [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:curator active-account}
                                      [:total-count]]]}])]
    (let [total (get-in @query [:search-memes :total-count])] ;;when-not (:graphql/loading? @query)
      [:div "Total " (or total [:div.loading])])))

(defmethod total :selling [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/active]}
                                      [:total-count]]]}])]
    (let [total (get-in @query [:search-meme-auctions :total-count])]
      [:div "Total " (or total [:div.loading])])))

(defmethod total :sold [_ active-account form-data]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/done]}
                                      [:total-count]]]}])]
    (let [total (get-in @query [:search-meme-auctions :total-count])]
      [:div "Total " (or total [:div.loading])])))

(defmethod panel :sold [_ state]
  [:div.tiles
   (doall
    (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
           (when address
             (let [{:keys [:meme-token/number :meme-token/meme]} meme-token
                   {:keys [:meme/title :meme/image-hash :meme/total-minted]} meme
                   now (subscribe [:district.ui.now.subs/now])
                   price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
               ^{:key address} [:div.meme-card-front
                                [tiles/meme-image image-hash]
                                [:a {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                                           nil
                                                           {:reg-entry/address (:reg-entry/address meme)}])}
                                 [:div.title [:b (str "#" number " " title)]]
                                 [:div.number-minted (str number "/" total-minted)]
                                 [:div.price (format/format-eth (web3/from-wei price :ether))]]])))
         state))])

(defn- build-order-by [prefix order-by]
  (keyword (str (cljs.core/name prefix) ".order-by") order-by))

(defn build-query [tab {:keys [:active-account :form-data :prefix :first :after] :as opts}]

#_(prn "@build-query" active-account opts)

  (let [{:keys [:term :order-by :order-dir :search-tags :group-by-memes? :voted? :challenged?]} form-data]
    (case tab
      :collected [[:search-memes (merge {:owner active-account
                                         :first first}
                                        (when after
                                          {:after (str after)})
                                        (when term
                                          {:title term})
                                        (when order-by
                                          {:order-by (build-order-by prefix order-by)})
                                        (when order-dir
                                          {:order-dir order-dir})
                                        (when search-tags
                                          {:tags search-tags}))
                   [:total-count
                    :end-cursor
                    :has-next-page
                    [:items (remove nil? [:reg-entry/address
                                          :reg-entry/status
                                          :meme/image-hash
                                          :meme/meta-hash
                                          :meme/number
                                          :meme/title
                                          :meme/total-supply
                                          [:meme/owned-meme-tokens {:owner active-account}
                                           [:meme-token/token-id]]])]]]]
      :created [[:search-memes (merge {:creator active-account
                                       :first first}
                                      (when after
                                        {:after (str after)})
                                      (when term
                                        {:title term})
                                      (when order-by
                                        {:order-by (build-order-by prefix order-by)})
                                      (when order-dir
                                        {:order-dir order-dir})
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
      :curated [[:search-memes (merge {:curator active-account
                                       :first first}
                                      (when (or voted? challenged?)
                                        {:statuses (cond-> []
                                                     voted? (conj :reg-entry.status/blacklisted
                                                                  :reg-entry.status/whitelisted)
                                                     challenged? (conj :reg-entry.status/challenge-period))})
                                      (when after
                                        {:after (str after)})
                                      (when term
                                        {:title term})
                                      (when order-by
                                        {:order-by (build-order-by prefix order-by)})
                                      (when order-dir
                                        {:order-dir order-dir})
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
                           [:challenge/vote {:vote/voter active-account}
                            [:vote/option]]]]]]]
      :selling [[:search-meme-auctions (merge {:seller active-account
                                               :statuses [:meme-auction.status/active]
                                               :first first}
                                              (when after
                                                {:after (str after)})
                                              (when term
                                                {:title term})
                                              (when order-by
                                                {:order-by (build-order-by prefix order-by)})
                                              (when order-dir
                                                {:order-dir order-dir})
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
                           [:meme-auction/meme-token
                            [:meme-token/number
                             [:meme-token/meme
                              [:reg-entry/address
                               :meme/title
                               :meme/image-hash
                               :meme/meta-hash
                               :meme/total-minted]]]]]]]]]
      :sold [[:search-meme-auctions (merge {:seller active-account
                                            :statuses [:meme-auction.status/done]
                                            :first first}
                                           (when after
                                             {:after (str after)})
                                           (when term
                                             {:title term})
                                           (when order-by
                                             {:order-by (build-order-by prefix order-by)})
                                           (when order-dir
                                             {:order-dir order-dir})
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
                        [:meme-auction/meme-token
                         [:meme-token/number
                          [:meme-token/meme
                           [:reg-entry/address
                            :meme/title
                            :meme/image-hash
                            :meme/meta-hash
                            :meme/total-minted]]]]]]]]])))

(defn scrolling-container [tab {:keys [:form-data :active-account :prefix]}]
  (let [safe-mapcat (fn [f queries]
                      (loop [queries queries
                             result []]
                        (if (empty? queries)
                          result
                          (recur (rest queries)
                                 (let [not-contains? (complement contains?)
                                       new (filter #(not-contains? (set result) %)
                                                   (f (first queries)))]
                                   (into result new))))))
        query (build-query tab {:active-account active-account
                                :prefix prefix
                                :form-data form-data
                                :after 0
                                :first scroll-interval})
        query-id (merge form-data {:tab tab})
        query-subs (subscribe [::gql/query {:queries query}
                               {:id query-id}])
        k (case prefix
            :memes :search-memes
            :meme-auctions :search-meme-auctions)
        state (mapcat #_safe-mapcat (fn [q] (get-in q [k :items])) @query-subs)]

    (prn "re-render" tab (map #(get-in % [(case prefix
                                              :memes :reg-entry/address
                                              :meme-auctions :meme-auction/address)]) state))
    [:div.scroll-area
     (if (:graphql/loading? @query-subs)
       [:div.loading]
       [panel tab state])
     [infinite-scroll {:load-fn (fn []
                                  ;;when-not (:graphql/loading? @query-subs)
                                  (let [{:keys [:has-next-page :end-cursor]} (k (last @query-subs))]
                                    (when (or has-next-page (empty? state))
                                      (dispatch [::gql-events/query
                                                 {:query {:queries (build-query tab {:active-account active-account
                                                                                     :prefix prefix
                                                                                     :form-data form-data
                                                                                     :first scroll-interval
                                                                                     :after end-cursor})}
                                                  :id query-id}]))))}]]))

(defn tabbed-pane [tab prefix form-data]
  (let [active-account (subscribe [::accounts-subs/active-account])
        tags (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        re-search (fn [] (dispatch [::gql-events/query
                                    {:query {:queries (build-query @tab {:active-account @active-account
                                                                         :prefix prefix
                                                                         :form-data @form-data
                                                                         :first scroll-interval
                                                                         :after 0})}
                                     :id (merge @form-data {:tab @tab})}]))]

    (fn [tab prefix form-data]
      [:div.tabbed-pane
       [:section.search-form
        [search/search-tools (merge {:title "My Memefolio"
                                     :form-data form-data
                                     :on-selected-tags-change re-search
                                     :on-search-change re-search
                                     :on-check-filter-change re-search
                                     :on-select-change re-search
                                     :tags (->> @tags :search-tags :items (mapv :tag/name))
                                     :selected-tags-id :search-tags
                                     :search-id :term
                                     :sub-title (str "Search " (cljs.core/name prefix))
                                     :select-options (case prefix
                                                       :memes [{:key :memes.order-by/created-on :value "Newest"}
                                                               {:key :memes.order-by/reveal-period-end :value "Recently Revealed"}
                                                               {:key :memes.order-by/commit-period-end :value "Recently Commited"}
                                                               {:key :memes.order-by/challenge-period-end :value "Recently challenged"}
                                                               {:key :memes.order-by/total-trade-volume :value "Trade Volume"}
                                                               {:key :memes.order-by/number :value "Number"}
                                                               {:key :memes.order-by/total-minted :value "Total Minted"}]

                                                       :meme-auctions [{:key :meme-auctions.order-by/started-on :value "Newest"}
                                                                       {:key :meme-auctions.order-by/meme-total-minted :value "Rarest"}
                                                                       {:key :meme-auctions.order-by/price :value "Cheapest"}
                                                                       {:key :meme-auctions.order-by/random :value "Random"}])}
                                    (when (= :collected @tab)
                                      {:check-filters [{:label "Group by memes"
                                                        :id :group-by-memes?}]})
                                    (when (= :curated @tab)
                                      {:check-filters [{:label "Voted"
                                                        :id :voted?}
                                                       {:label "Challenged"
                                                        :id :challenged?}]}))]]
       [:section.tabs
        (doall
         (map (fn [tab-id]
                ^{:key tab-id} [:div
                                {:class (when (= @tab
                                                 tab-id) "selected")}
                                [:a {:on-click (fn [evt]
                                                 (.preventDefault evt)
                                                 (reset! tab tab-id))
                                     :href "#"}
                                 (-> tab-id
                                     cljs.core/name
                                     (str/capitalize))]])
              [:collected :created :curated :selling :sold]))
        [:div.total
         [total @tab @active-account]]]
       [:section.stats
        (when (not (contains? #{:selling :sold} @tab))
          [:div.rank
           [rank @tab @active-account]])]
       [:section.tiles
        [:div.panel
         [scrolling-container @tab {:active-account @active-account :form-data @form-data :prefix prefix}]]]])))

(defmethod page :route.memefolio/index []
  (let [active-tab (r/atom default-tab)]
    (fn []
      (let [{:keys [:query]} @(subscribe [::router-subs/active-page])
            prefix (cond (contains? #{:collected :created :curated} @active-tab)
                         :memes
                         (contains? #{:selling :sold} @active-tab)
                         :meme-auctions)
            order-by? (-> query :order-by nil? not)
            memes? (= prefix :memes)
            meme-auctions? (= prefix :meme-auctions)
            form-data (r/atom {:term (:term query)
                               :order-by (match [order-by? memes? meme-auctions?]
                                                [true _ _] (build-order-by prefix (:order-by query))
                                                [false true false] (build-order-by prefix :created-on)
                                                [false false true] (build-order-by prefix :started-on))
                               :order-dir (or (keyword (:order-dir query)) :desc)})]
        [app-layout/app-layout
         {:meta {:title "MemeFactory"
                 :description "Description"}}
         [:div.memefolio-page
          [tabbed-pane active-tab prefix form-data]]]))))
