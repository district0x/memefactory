(ns memefactory.ui.memefolio.page
  (:require

   [cljs-web3.core :as web3]
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
   [district.ui.graphql.utils :as graphql-ui-utils]
   [district.ui.router.subs :as router-subs]
   [district.ui.server-config.subs :as config-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.app-layout :as app-layout]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.search :as search]
   [memefactory.ui.components.tiles :as tiles]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :as ratom]

            ))

(def default-tab :sold)

(def scroll-interval 5)

(defmulti panel (fn [tab & opts] tab))

(defmulti rank (fn [tab & opts] tab))

(defmulti total (fn [tab & opts] tab))

(defn resolve-image [image-hash]
  (let [gateway @(subscribe [::config-subs/config :ipfs :gateway])]
    (str (format/ensure-trailing-slash gateway) image-hash)))

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

(defn collected-tile-front [{:keys [:meme/image-hash]}]
  [:img {:src (resolve-image image-hash)}])

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

(defmethod panel :collected [_ state]  
  [:div.tiles
   (doall (map (fn [{:keys [:reg-entry/address :reg-entry/status :meme/image-hash :meme/number
                            :meme/title :meme/total-supply :meme/owned-meme-tokens] :as meme}]
                 (when address
                   (let [token-ids (map :meme-token/token-id owned-meme-tokens)
                         token-count (count token-ids)]
                     ^{:key address} [:div
                                      [tiles/flippable-tile {:id address
                                                             :front [collected-tile-front {:meme/image-hash image-hash}]
                                                             :back [collected-tile-back {:meme/number number
                                                                                         :meme/title title
                                                                                         :meme/owned-meme-tokens owned-meme-tokens
                                                                                         :meme-auction/token-count token-count
                                                                                         :meme-auction/token-ids token-ids}]}]
                                      [:div [:b (str "#" number " " title)]]
                                      (when (and token-count total-supply)
                                        [:div [:span (str "Owning " token-count " out of " total-supply)]])])))
               state))])

(defmethod rank :collected [_ active-account]
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
    (if (:graphql/loading? @query)
      [:div.loading "Loading..."]
      (let [{:keys [:user/collector-rank :user/total-collected-memes :user/total-collected-token-ids
                    :user/largest-buy]} (:user @query)
            {:keys [:meme-auction/bought-for :meme-auction/meme-token]} largest-buy
            {:keys [:meme-token/number :meme-token/meme]} meme-token
            {:keys [:meme/title]} meme
            style {:background-color "yellow"
                   :display "inline-block"}]
        [:div.stats {:style {:display "block"}}
         [:div.rank {:style {:background-color "orange"
                             :display "inline-block"}}
          (str "RANK: " collector-rank)]
         [:div.unique-memes {:style style}
          [:b "Unique Memes: "]
          (str total-collected-memes "/" (-> @query :search-memes :total-count))]
         [:div.unique-memes {:style style}
          [:b "Total Cards: "]
          [:span (str total-collected-token-ids "/" (-> @query :search-meme-tokens :total-count))]]
         [:div.largest-buy {:style style}
          [:b "Largest buy: "]
          [:span (str (format/format-eth (web3/from-wei bought-for :ether))
                      "(#" number " " title ")")]]]))))

(defmethod total :collected [_ active-account]
  (let [query (subscribe [::gql/query {:queries [[:search-memes {:owner active-account}
                                                  [:total-count]]]}])]
    [:div "Total " (when (not (:graphql/loading? @query))
                     (get-in @query [:search-memes :total-count]))]))

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

(defmethod panel :created [_ state]
  [:div.tiles
   (doall (map (fn [{:keys [:reg-entry/address :meme/image-hash :meme/number
                            :meme/title :meme/total-supply :meme/total-minted
                            :reg-entry/status] :as meme}]
                 (when address
                   (let [status (graphql-utils/gql-name->kw status)]
                     ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                    :height 280
                                                                    :display "block"}}
                                      [:img {:src (resolve-image image-hash)}]
                                      [:div [:b (str "#" number " " title)]]
                                      [:div [:span (str total-minted "/" total-supply" Issued")]]
                                      [:div
                                       (case status
                                         :reg-entry.status/whitelisted [:label [:b "In Registry"]]
                                         :reg-entry.status/blacklisted [:label [:b "Rejected"]]
                                         [:label [:b "Challenged"]])]

                                      (when (= status :reg-entry.status/whitelisted)
                                        [issue-form {:meme/title title
                                                     :reg-entry/address address
                                                     :max-amount (- total-supply total-minted)}])])))
               state))])

(defmethod rank :created [_ active-account]
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
        [:div.rank {:style {:white-space "nowrap"}}
         [:div.rank {:style {:background-color "orange"
                             :display "inline-block"}}
          (str "RANK: " creator-rank)]
         [:div.earned {:style style}
          [:b "Earned: "]
          (format/format-eth (web3/from-wei creator-total-earned :ether))]
         [:div.success-rate {:style style}
          [:b "Success Rate: "]
          (str total-created-memes-whitelisted "/" total-created-memes " ("
               (format/format-percentage total-created-memes-whitelisted total-created-memes) ")")]
         [:div.best-sale {:style style}
          [:b "Best Single Card Sale: "]
          (str (-> (shared-utils/calculate-meme-auction-price largest-sale (-> (.getTime @now) time/time-units :seconds))
                   (web3/from-wei :ether)
                   format/format-eth)
               " (#" number " " title ")")]]))))

(defmethod total :created [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:creator active-account}
                                      [:total-count]]]}])]
    [:div "Total " (when (not (:graphql/loading? @query))
                     (get-in @query [:search-memes :total-count]))]))

(defmethod panel :curated [_ state]
  [:div.tiles
   (doall
    (map (fn [{:keys [:reg-entry/address :meme/image-hash :meme/number
                      :meme/title :challenge/vote] :as meme}]
           (when address
             (let [{:keys [:vote/option]} vote]
               ^{:key address} [:div.meme-card-front {:style {:width 200
                                                              :height 280
                                                              :display "block"}}
                                [:img {:src (resolve-image image-hash)}]
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
                                    [:i.icon.thumbs.down.outline]])]])))
         state))])

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
    (if (:graphql/loading? @query)
      [:div.loading "Loading..."]
      (let [{:keys [:user/curator-rank :user/total-created-challenges :user/total-created-challenges-success
                    :user/challenger-total-earned :user/total-participated-votes :user/total-participated-votes-success
                    :user/voter-total-earned]} (:user @query)
            style {:background-color "yellow"
                   :display "inline-block"}]
        [:div.stats {:style {:display "block"}}
         [:div.rank {:style {:background-color "orange"
                             :display "inline-block"}}
          (str "RANK: " curator-rank)]
         [:div.curator {:style style}
          [:div.challenges
           "CHALLENGES:"
           [:div {:style style} [:b "Success Rate:"] total-created-challenges-success "/" total-created-challenges
            " (" (format/format-percentage total-created-challenges-success total-created-challenges)  ")"]
           [:div {:style style} [:b "Earned:"] (str (web3/from-wei challenger-total-earned :ether) " DANK")]]
          [:div.votes "VOTES:"
           [:div {:style style} [:b "Success Rate:"] total-participated-votes "/" total-participated-votes-success
            " (" (format/format-percentage total-participated-votes-success total-participated-votes)  ")"]
           [:div {:style style} [:b "Earned:"] (str (web3/from-wei voter-total-earned :ether) " DANK")]]
          [:div.total-earnings "TOTAL-EARNINGS:"
           [:div {:style style} (str (web3/from-wei (+ challenger-total-earned voter-total-earned) :ether) " DANK")]]]]))))

(defmethod total :curated [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-memes {:curator active-account}
                                      [:total-count]]]}])]
    [:div "Total " (when (not (:graphql/loading? @query))
                     (get-in @query [:search-memes :total-count]))]))

(defmethod panel :selling [_ state]
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
         state))])

(defmethod total :selling [_ active-account]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/active]}
                                      [:total-count]]]}])]
    [:div "Total " (when (not (:graphql/loading? @query))
                     (get-in @query [:search-meme-auctions :total-count]))]))

(defmethod panel :sold [_ state]  
  [:div.tiles
   (doall
    (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
           (when address
             (let [{:keys [:meme-token/number :meme-token/meme]} meme-token
                   {:keys [:meme/title :meme/image-hash :meme/total-minted]} meme
                   now (subscribe [:district.ui.now.subs/now])
                   price (shared-utils/calculate-meme-auction-price meme-auction (:seconds (time/time-units (.getTime @now))))]
               ^{:key address} [:div.meme-card-front {:style {:width 500
                                                              :height 380
                                                              :display "block"}}                                
                                [:img {:src (resolve-image image-hash)}]
                                [:div.title [:b (str "#" number " " title)]]
                                [:div.number-minted (str number "/" total-minted)]
                                [:div.price (format/format-eth (web3/from-wei price :ether))]])))
         state))])

(defmethod total :sold [_ active-account form-data]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller active-account :statuses [:meme-auction.status/done]}
                                      [:total-count]]]}])]
    [:div "Total " (when (not (:graphql/loading? @query))
                     (get-in @query [:search-meme-auctions :total-count]))]))

(defn- build-order-by  [prefix order-by]
  (keyword (str (cljs.core/name prefix) ".order-by") order-by))

(defn build-query [tab {:keys [:active-account :form-data :prefix :first :after] :as opts}]

  #_(prn "@build-query" tab opts)
  
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
                              [:meme/title
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
                           [:meme/title
                            :meme/image-hash
                            :meme/meta-hash
                            :meme/total-minted]]]]]]]]])))

(defn scrolling-container [tab {:keys [:form-data :active-account :prefix]}]
  (letfn [(safe-mapcat [f queries] (loop [queries queries
                                          result []]
                                     (if (empty? queries)
                                       result
                                       (recur (rest queries)
                                              (let [not-contains? (complement contains?)
                                                    new (filter #(not-contains? (set result) %)
                                                                (f (first queries)))]
                                                (into result new))))))]
    (let [query (subscribe [::gql/query {:queries (build-query tab {:active-account active-account
                                                                    :prefix prefix
                                                                    :form-data form-data
                                                                    :after 0
                                                                    :first scroll-interval})}
                            {:id tab
                             ;; :disable-fetch? true
                             }])
          k (case prefix
              :memes :search-memes
              :meme-auctions :search-meme-auctions)
          state (safe-mapcat (fn [q] (get-in q [k :items])) @query)]

      (prn "re-render" tab (map #(get-in % [(case prefix
                                              :memes :reg-entry/address
                                              :meme-auctions :meme-auction/address)]) state))
      
      [:div.scroll-area
       [panel tab state]
       [infinite-scroll {:load-fn (fn []
                                    (when-not (:graphql/loading? @query)
                                      (let [{:keys [:has-next-page :end-cursor]} (k (last @query))]

                                        #_(prn "has-next / from" has-next-page end-cursor)

                                        (when (or has-next-page (empty? state))
                                                                                    
                                          (dispatch [::gql-events/query
                                                     {:query {:queries (build-query tab {:active-account active-account
                                                                                         :prefix prefix
                                                                                         :form-data form-data
                                                                                         :first scroll-interval
                                                                                         :after end-cursor})}
                                                         :id tab}])))))}]])))

(defn tabbed-pane [tab prefix form-data]
  (let [active-account (subscribe [::accounts-subs/active-account])
        tags (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        #_re-search #_(fn []
                    (dispatch [::gql-events/query
                               {:query {:queries (build-query tab {:active-account @active-account
                                                                   :form-data @form-data
                                                                   :prefix prefix
                                                                   :after 0
                                                                   :first scroll-interval})}}]))]

    (fn [tab prefix form-data]
      (let [        re-search (fn []
                    (dispatch [::gql-events/query
                               {:query {:queries (build-query tab {:active-account @active-account
                                                                   :form-data @form-data
                                                                   :prefix prefix
                                                                   :after 0
                                                                   :first scroll-interval})}}]))]
        [:div.tabbed-pane {:style {:display "grid"
                                   :grid-template-areas
                                   "'search search search search search search'
                                  'tab tab tab tab tab total'
                                  'rank rank rank rank rank rank'
                                  'panel panel panel panel panel panel'"}}

         [:div {:style {:grid-area "search"}}
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

         [:div.tabs {:style {:grid-area "tab"}}
          (map (fn [tab-id]
                 ^{:key tab-id} [:div {:style {:display "inline-block"}}
                                 [:a {:on-click (fn [evt]
                                                  (reset! tab tab-id))}
                                  (-> tab-id
                                      cljs.core/name
                                      (str/capitalize))]])
               [:collected :created :curated :selling :sold])]

         [:div.total {:style {:grid-area "total"}}
          [total @tab @active-account]]

         (when (not (contains? #{:selling :sold} @tab))
           [:div.rank {:style {:grid-area "rank"}}
            [rank @tab @active-account]])

         [:div.panel {:style {:grid-area "panel"}}
          [scrolling-container @tab {:active-account @active-account :form-data @form-data :prefix prefix}]]]))))


(defmethod page :route.memefolio/index []
  (let [tab (r/atom default-tab)]
    (fn []
      (let [{:keys [:query]} @(subscribe [::router-subs/active-page])
            prefix (cond (contains? #{:collected :created :curated} @tab)
                         :memes
                         (contains? #{:selling :sold} @tab)
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
         [tabbed-pane tab prefix form-data]]))))
