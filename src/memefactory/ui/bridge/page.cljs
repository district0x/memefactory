(ns memefactory.ui.bridge.page
  (:require
    [cljs-web3.core :as web3]
    [clojure.string :as str]
    [district.parsers :as parsers]
    [district.ui.component.form.input :refer [index-by-type file-drag-input with-label chip-input text-input int-input pending-button select-input checkbox-input]]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.events :as gql-events]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as router-subs]
    [district.ui.web3-account-balances.subs :as balance-subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.ui.web3-chain.subs :as chain-subs]
    [district.ui.web3-tx-id.subs :as tx-id-subs]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [memefactory.ui.components.buttons :refer [chain-check-pending-button switch-chain-button]]
    [memefactory.ui.components.general :refer [nav-anchor dank-with-logo]]
    [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
    [memefactory.ui.components.panels :refer [no-items-found]]
    [memefactory.ui.components.spinner :as spinner]
    [memefactory.ui.components.tiles :as tiles]
    [memefactory.ui.config :refer [config-map]]
    [memefactory.ui.contract.bridge :as bridge-contracts]
    [memefactory.ui.utils :as ui-utils]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.ratom :refer [reaction]]
    [taoensso.timbre :as log :refer [spy]]
    [district.format :as format])
  (:require-macros [memefactory.shared.utils :refer [get-environment]]))

(defn format-token [token-id]
  (str "0x..." (str/join (take-last 5 (web3/to-hex token-id)))))

(defn format-tokens [tokens]
  (str/join ", " (map format-token (str/split tokens ","))))

(def default-tab :deposit-dank)

(def page-size 6)

(defn l1-chain? []
  (= @(subscribe [::chain-subs/chain]) (get-in config-map [:web3-chain :l1 :chain-id])))

(defn deposit-dank []
  (let [account-balance (subscribe [::balance-subs/active-account-balance :DANK-root])
        form-data (r/atom {:amount 1})
        errors (reaction {:local (let [{:keys [amount]} @form-data
                                       max-amount (or (/ @account-balance 1e18) 1)]
                                   (cond-> {:amount {:hint (str "Max " (format/format-number max-amount {:max-fraction-digits 0}))}}
                                           (not (try
                                                  (let [amount (parsers/parse-int amount)]
                                                    (and (< 0 amount) (<= amount max-amount)))
                                                  (catch js/Error e nil)))
                                           (assoc-in [:amount :error] (str "Amount should be a number between 1 and " (format/format-number max-amount {:max-fraction-digits 0})))))})
        critical-errors (reaction (index-by-type @errors :error))
        active-account (subscribe [::accounts-subs/active-account])
        tx-id (str @active-account "bridge-dank-to-l2" (random-uuid))
        bridge-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::bridge-contracts/approve-and-bridge-dank-to-l2 tx-id}])
        bridge-tx-success? (subscribe [::tx-id-subs/tx-success? {::bridge-contracts/approve-and-bridge-dank-to-l2 tx-id}])]
    (fn []
      [:div.bridge-box
       [:section.bridge-header
        [:div.icon]
        [:h2.title (str "Transfer DANK ETH → Polygon")]
        [:h3.title "Bring your DANK tokens from Ethereum to Polygon network"]
        [switch-chain-button {:net :l1}]]
       [:p "You can bring the DANK tokens you have on Ethereum Mainnet to Polygon. Select the amount of tokens you want to transfer. Once the transaction succeed, switch your wallet to Polygon Mainnet where you'll receive your DANK in a few minutes."]
       [:div.form-panel
        [with-label "Amount"
         [text-input {:form-data form-data
                      :errors errors
                      :id :amount
                      :dom-id :amount
                      :type :number
                      :min 1}]
         {:form-data form-data
          :id :amount
          :for :amount}]
        [:div.submit
         [chain-check-pending-button {:for-chain (get-in config-map [:web3-chain :l1 :chain-id])
                                      :pending? @bridge-tx-pending?
                                      :pending-text "Transferring tokens"
                                      :disabled (or (not (empty? @critical-errors)) @bridge-tx-pending? @bridge-tx-success? (not @active-account))
                                      :class (when-not @bridge-tx-success? "bridge-dank-to-l2")
                                      :on-click (fn [e]
                                                  (.stopPropagation e)
                                                  (dispatch [::bridge-contracts/approve-and-bridge-dank-to-l2 {:send-tx/id tx-id
                                                                                                               :to @active-account
                                                                                                               :amount (min @account-balance ; deal with rounding
                                                                                                                            (* (:amount @form-data) 1e18))}]))}
          (if @bridge-tx-success? "Tokens Transferred" "Transfer Tokens")]
         ]
        (when (< @account-balance 1)
          [:div.not-enough-dank "You don't have DANK tokens on Ethereum"])
        ]
       [:div.footer "Make sure your wallet is connected to Ethereum Mainnet network"]])))


(defn deposit-meme-token []
  (let [active-account (subscribe [::accounts-subs/active-account])
        form-token-data (r/atom {})
        tx-id (str @active-account "bridge-meme-token-to-l2" (random-uuid))
        bridge-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::bridge-contracts/bridge-meme-to-l2 tx-id}])
        bridge-tx-success? (subscribe [::tx-id-subs/tx-success? {::bridge-contracts/bridge-meme-to-l2 tx-id}])
        ]
    (fn []
      (let [state (if (l1-chain?) @(subscribe [::bridge-contracts/meme-token-ids @active-account]) [])
            token-ids (keys (filter (fn[[_ v]] (true? v)) @form-token-data))
            errors (cond-> []
                           (> (count token-ids) 20) (conj "Cannot bridge more than 20 tokens at once"))]
      [:div.bridge-page
       [:div.bridge-box
        [:section.bridge-header
         [:div.icon]
         [:h2.title (str "Transfer Meme NFTs ETH → Polygon")]
         [:h3.title "Bring your Meme tokens from Ethereum to Polygon network"]
         [switch-chain-button {:net :l1}]]
        [:p "You can bring the Meme NFT you have on Ethereum Mainnet to Polygon. Select the tokens you want to transfer. Once the transaction succeed, switch your wallet to Polygon Mainnet where you'll receive your Meme NFTs in a few minutes."]
        [:div.form-panel
         (doall (map (fn [token-id]
                       (let [token-title (format-token token-id)
                             cb-id token-id]
                         [:div.single-check {:key token-id}
                          [checkbox-input {:form-data form-token-data
                                           :id cb-id
                                           :value token-id}]
                          [:label {:on-click #(swap! form-token-data update cb-id not)}
                           token-title]])) state))
         [:div.submit-errors
          (doall
            (for [e errors]
              [:div.error {:key e} "*" e]))]
         [:div.submit
          [chain-check-pending-button {:for-chain (get-in config-map [:web3-chain :l1 :chain-id])
                                       :pending? @bridge-tx-pending?
                                       :pending-text "Transferring tokens"
                                       :disabled (or (not (empty? errors)) @bridge-tx-pending? @bridge-tx-success? (not @active-account) (empty? token-ids))
                                       :class (when-not @bridge-tx-success? "bridge-meme-to-l2")
                                       :on-click (fn [e]
                                                   (.stopPropagation e)
                                                   (dispatch [::bridge-contracts/bridge-meme-to-l2 {:send-tx/id tx-id
                                                                                                    :token-ids token-ids}]))}
           (if @bridge-tx-success? "Tokens Transferred" "Transfer Tokens")]]]
        [:div.footer "Make sure your wallet is connected to Ethereum Mainnet network"]]]))))


(defn start-withdraw-dank []
  (let [account-balance (subscribe [::balance-subs/active-account-balance :DANK])
        form-data (r/atom {:amount 1})
        errors (reaction {:local (let [{:keys [amount]} @form-data
                                       max-amount (or (/ @account-balance 1e18) 1)]
                                   (cond-> {:amount {:hint (str "Max " (format/format-number max-amount {:max-fraction-digits 0}))}}
                                           (not (try
                                                  (let [amount (parsers/parse-int amount)]
                                                    (and (< 0 amount) (<= amount max-amount)))
                                                  (catch js/Error e nil)))
                                           (assoc-in [:amount :error] (str "Amount should be a number between 1 and " (format/format-number max-amount {:max-fraction-digits 0})))))})
        critical-errors (reaction (index-by-type @errors :error))
        active-account (subscribe [::accounts-subs/active-account])
        tx-id (str @active-account "start-dank-withdraw" (random-uuid))
        bridge-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::bridge-contracts/start-withdraw-dank tx-id}])
        bridge-tx-success? (subscribe [::tx-id-subs/tx-success? {::bridge-contracts/start-withdraw-dank tx-id}])
        ]
    (fn []
      [:div.bridge-box
       [:section.bridge-header
        [:div.icon]
        [:h2.title (str "1. Start Withdraw DANK Polygon → ETH")]
        [:h3.title "Withdraw DANK tokens from Polygon to be claimed on Ethereum network"]
        [switch-chain-button]]
       [:p "You can send the DANK tokens you have on Polygon to Ethereum. Select the amount of tokens you want to transfer and trigger the button to start the withdrawal process"]
       [:div.form-panel
        [with-label "Amount"
         [text-input {:form-data form-data
                      :errors errors
                      :id :amount
                      :dom-id :amount
                      :type :number
                      :min 1}]
         {:form-data form-data
          :id :amount
          :for :amount}]
        [:div.submit
         [chain-check-pending-button {:pending? @bridge-tx-pending?
                                      :pending-text "Withdrawing tokens"
                                      :disabled (or (not (empty? @critical-errors)) @bridge-tx-pending? @bridge-tx-success? (not @active-account))
                                      :class (when-not @bridge-tx-success? "start-withdraw-dank")
                                      :on-click (fn [e]
                                                  (.stopPropagation e)
                                                  (dispatch [::bridge-contracts/start-withdraw-dank {:send-tx/id tx-id
                                                                                                     :amount (min @account-balance ; deal with rounding
                                                                                                                  (* (:amount @form-data) 1e18))}]))}
          (if @bridge-tx-success? "Tokens withdrew" "Withdraw Tokens")]
         ]
        (when (< @account-balance 1)
          [:div.not-enough-dank "You don't have DANK tokens to bridge"])
        ]])))


(defn withdraw-form [form-data options tx-id tx-event]
  (let [active-account (subscribe [::accounts-subs/active-account])
        bridge-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::bridge-contracts/exit-token tx-id}])
        bridge-tx-success? (subscribe [::tx-id-subs/tx-success? {::bridge-contracts/exit-token tx-id}])
        building-payload? (subscribe [::bridge-contracts/building-payload? tx-id])]
    (when (and (empty? (:withdraw-tx @form-data)) (not-empty options))
      (swap! form-data (fn [] {:withdraw-tx (:key (first options))})))
    [:div.form-panel
     [select-input {:form-data form-data
                   :id :withdraw-tx
                   :group-class :options
                   :disabled (or @bridge-tx-pending? @bridge-tx-success? @building-payload? (not (l1-chain?)))
                   :options options}]
     [:div.submit
      [chain-check-pending-button {:for-chain (get-in config-map [:web3-chain :l1 :chain-id])
                                   :pending? @bridge-tx-pending?
                                   :pending-text "Claiming tokens"
                                   :disabled (or @bridge-tx-pending? @bridge-tx-success? @building-payload? (empty? (:withdraw-tx @form-data)) (not @active-account))
                                   :class (when-not @bridge-tx-success? "claim-withdraw-meme")
                                   :on-click (fn [e]
                                               (.stopPropagation e)
                                               (println tx-id)
                                               (dispatch
                                                 [::bridge-contracts/build-exit-payload {:on-success [tx-event {:send-tx/id tx-id}]
                                                                                         :withdraw-tx (:withdraw-tx @form-data)
                                                                                         :tx-id tx-id
                                                                                         :testnet? (not= (get-environment) "prod")}]))}
       (if @bridge-tx-success? "Tokens claimed" "Claim Tokens")]]
     (when @building-payload?
       [:div.message
        [:p "preparing transaction payload..."]])
     (when (and (l1-chain?) (empty? options))
       [:div.no-pending-withdraw "There are not pending withdraws or they have not being confirmed yet"])]))


(defn claim-withdraw-dank []
  (let [tx-id (str "claim-withdraw-dank" (random-uuid))]
    (fn []
      (let [active-account (subscribe [::accounts-subs/active-account])
            form-data (r/atom {:withdraw-tx ""})
            withdraw-txs (subscribe [::gql/query {:queries [[:search-withdraw-dank {:receiver @active-account}
                                                             [[:items [:withdraw-dank/tx :withdraw-dank/amount :withdraw-dank/receiver]]]]]}])
            items (->> @withdraw-txs :search-withdraw-dank :items)
            pending-withdraw-txs (if (l1-chain?) (set @(subscribe [::bridge-contracts/filter-withdraws (mapv :withdraw-dank/tx items) (not= (get-environment) "prod")])) [])
            options (map #(merge {} {:key (:withdraw-dank/tx %) :value (str "Receive: " (/ (:withdraw-dank/amount %) 1e18) " DANK")})
                         (filter #(contains? pending-withdraw-txs (:withdraw-dank/tx %)) items))
            ]
        [:div.bridge-box
         [:section.bridge-header
          [:h2.title (str "2. Complete withdrawing DANK Polygon → ETH")]
          [:h3.title "Claim the tokens you have previously withdraw from Polygon to Ethereum network"]
          [switch-chain-button {:net :l1}]]
         [:p "Once the Polygon transaction receipt is confirmed on Ethereum (up to 3 hours), switch your wallet to Ethereum Mainnet and complete the withdrawal to claim the DANK you transferred"]
         [withdraw-form form-data options tx-id ::bridge-contracts/exit-dank]
         [:div.footer "Make sure your wallet is connected to Ethereum Mainnet network"]]))))


(defn build-meme-query [{:keys [:active-account :first :after] :as opts}]
  [[:search-memes (merge {:owner active-account
                          :first first}
                         (when after
                           {:after (str after)})
                         {:order-by (ui-utils/build-order-by :memes :created-on)
                          :order-dir :desc})
    [:total-count
     :end-cursor
     :has-next-page
     [:items (remove nil? [:reg-entry/address
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
                           [:meme/owned-meme-tokens {:owner active-account}
                            [:meme-token/token-id]]])]]]]
  )

(defn start-withdraw-meme-token []
  (let [active-account (subscribe [::accounts-subs/active-account])
        form-token-data (r/atom {})
        tx-id (str @active-account "start-meme-token-withdraw" (random-uuid))
        bridge-tx-pending? (subscribe [::tx-id-subs/tx-pending? {::bridge-contracts/start-withdraw-meme-token tx-id}])
        bridge-tx-success? (subscribe [::tx-id-subs/tx-success? {::bridge-contracts/start-withdraw-meme-token tx-id}])]
    (fn []
      (let [query (build-meme-query {:active-account @active-account
                                     :first page-size})
            query-id {:panel :start-withdraw-meme-token :active-account @active-account}
            query-sub (subscribe [::gql/query {:queries query} {:id query-id}])
            last-sub (last @query-sub)
            end-cursor (-> last-sub :search-memes :end-cursor)
            re-search (fn [after]
                        (dispatch [::gql-events/query
                                   {:query {:queries (build-meme-query {:active-account @active-account
                                                                        :first page-size
                                                                        :after after})}
                                    :id query-id}]))
            has-more? (-> last-sub :search-memes :has-next-page)
            loading-first? (:graphql/loading? (first @query-sub))
            loading? (:graphql/loading? last-sub)
            state (mapcat (fn [q] (get-in q [:search-memes :items])) @query-sub)
            token-ids (keys (filter (fn[[_ v]] (true? v)) @form-token-data))
            errors (cond-> []
                          (> (count token-ids) 20) (conj "Cannot bridge more than 20 tokens at once"))]
      [:div.bridge-page
       [:div.bridge-box
        [:section.bridge-header
         [:div.icon]
         [:h2.title (str "1. Withdraw Meme tokens Polygon → ETH")]
         [:h3.title "Withdraw Meme NFT from Polygon to be claimed on Ethereum network"]
         [switch-chain-button]]
        [:p "You can send your Meme NFTs from Polygon to Ethereum. Select the tokens you want to transfer and trigger the button to start the withdrawal process"]
         (if (and (empty? state)
                  (not loading-first?))
           [no-items-found]
           [infinite-scroll {:class "tiles"
                             :loading? loading?
                             :loading-spinner-delegate (fn []
                                                         [:div.spinner-container [spinner/spin]])
                             :has-more? has-more?
                             :load-fn #(re-search end-cursor)}
           (when-not loading-first?
             (doall (map (fn [{:keys [:reg-entry/address :meme/title :meme/number :meme/number :meme/owned-meme-tokens :meme/image-hash :meme/total-supply] :as meme}]
                           (let [token-count (count owned-meme-tokens)]
                           [:div.compact-tile {:key address}
                            [tiles/flippable-tile {:front [tiles/meme-image image-hash
                                                           {:class "collected-tile-front"}]
                                                   :back [tiles/meme-back-tile meme]
                                                   }]
                             [:div.title (str "#" number " " title)]
                              (when (and token-count total-supply)
                                [:div.number-minted (str "Owning " token-count " out of " total-supply)])
                             [:div.tokens "Select NFTs"
                            (doall (map (fn [{:keys [:meme-token/token-id] :as meme-token}]
                                          (let [token-title (format-token token-id)
                                                cb-id token-id]
                                            [:div.single-check {:key token-id}
                                            [checkbox-input {:form-data form-token-data
                                                             :id cb-id
                                                             :value token-id}]
                                             [:label {:on-click #(swap! form-token-data update cb-id not)}
                                              token-title]]))
                                   owned-meme-tokens))
                            ]])) state)))])
        [:div.form-panel
         [:div.submit-errors
          (doall
            (for [e errors]
              [:div.error {:key e} "*" e]))]
         [:div.submit
          [chain-check-pending-button {:pending? @bridge-tx-pending?
                                       :pending-text "Withdrawing tokens"
                                       :disabled (or (not (empty? errors)) @bridge-tx-pending? @bridge-tx-success? (not @active-account) (empty? token-ids))
                                       :class (when-not @bridge-tx-success? "start-withdraw-meme")
                                       :on-click (fn [e]
                                                   (.stopPropagation e)
                                                   (dispatch [::bridge-contracts/start-withdraw-meme-token {:send-tx/id tx-id
                                                                                                            :token-ids token-ids}])
                                                   )}
           (if @bridge-tx-success? "Tokens withdrew" "Withdraw Tokens")]]]
        [:div.footer "Note: your NFTs will not be tradeable through this site until transfer back to Polygon"]]]))))


(defn claim-withdraw-meme-token []
  (let [tx-id (str "claim-withdraw-meme-token" (random-uuid))]
    (fn []
      (let [active-account (subscribe [::accounts-subs/active-account])
            form-data (r/atom {:withdraw-tx ""})
            withdraw-txs (subscribe [::gql/query {:queries [[:search-withdraw-meme {:receiver @active-account}
                                                             [[:items [:withdraw-meme/tx :withdraw-meme/tokens :withdraw-meme/receiver]]]]]}])
            items (->> @withdraw-txs :search-withdraw-meme :items)
            pending-withdraw-txs (if (l1-chain?) (set @(subscribe [::bridge-contracts/filter-withdraws (mapv :withdraw-meme/tx items) (not= (get-environment) "prod")])) [])
            options (map #(merge {} {:key (:withdraw-meme/tx %) :value (str "Receive: " (format-tokens (:withdraw-meme/tokens %)))})
                         (filter #(contains? pending-withdraw-txs (:withdraw-meme/tx %)) items))
            ]
        [:div.bridge-page
         [:div.bridge-box
          [:section.bridge-header
           [:h2.title (str "2. Complete withdrawing Meme tokens Polygon → ETH")]
           [:h3.title "Claim the meme tokens you have previously withdraw from Polygon to Ethereum network"]
           [switch-chain-button {:net :l1}]]
          [:p "Once the Polygon transaction receipt is confirmed on Ethereum (up to 3 hours), switch your wallet to Ethereum Mainnet and complete the withdrawal to claim the Meme NFT you transferred"]
          [withdraw-form form-data options tx-id ::bridge-contracts/exit-meme-token]
          [:div.footer "Make sure your wallet is connected to Ethereum Mainnet network"]]]))))


(defn withdraw-dank []
  [:div.withdraw
    [start-withdraw-dank]
    [claim-withdraw-dank]])

(defn withdraw-meme-token []
  [:div.withdraw
    [start-withdraw-meme-token]
    [claim-withdraw-meme-token]])


(def tab-names
  {:deposit-dank {:name "DANK ETH → Polygon" :panel deposit-dank }
   :withdraw-dank {:name "DANK Polygon → ETH" :panel withdraw-dank }
   :deposit-meme {:name "Meme ETH → Polygon" :panel deposit-meme-token }
   :withdraw-meme {:name "Meme Polygon → ETH" :panel withdraw-meme-token }})


(defn bridge-panels [{:keys [:tab]}]
  [app-layout
   {:meta {:title       "MemeFactory - Bridge Tokens"
           :description "Bridge tokens from/to ethereum mainnet. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.bridge-page
     [:div.tabbed-pane
      [:section.tabs
       (doall
         (map (fn [tab-id]
                ^{:key tab-id} [:div.tab
                                {:class (when (= tab
                                                 tab-id) "selected")}

                                [nav-anchor {:route :route.bridge/index
                                             :query {:tab tab-id}}
                                 (-> tab-names tab-id :name)
                                 ]])
              (keys tab-names)))]
      [:div.panel
       {:class (name tab)}
       (let [panel (-> tab-names tab :panel)]
         [panel])]]]])


(defmethod page :route.bridge/index []
  (let [{:keys [:query]} @(subscribe [::router-subs/active-page])
        active-tab (or (keyword (:tab query)) default-tab)]
  [bridge-panels {:tab active-tab}]))
