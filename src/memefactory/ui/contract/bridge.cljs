(ns memefactory.ui.contract.bridge
  (:require
    [akiroz.re-frame.storage :as storage]
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as cs]
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [cljs-web3-next.helpers :as web3-helpers]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.utils :refer [cljkk->js]]
    [cljs.spec.alpha :as s]
    [district.cljs-utils :as cljs-utils]
    [district.shared.async-helpers :refer [promise-> <?]]
    [district.ui.logging.events :as logging]
    [district.ui.notification.events :as notification-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as account-queries]
    [district.ui.web3.queries :as web3-queries]
    [district.ui.web3-tx.events :as tx-events]
    [goog.string :as gstring]
    [clojure.set :as set]
    [memefactory.shared.contract.registry-entry :refer [vote-options vote-option->num]]
    [re-frame.core :as re-frame]
    [memefactory.ui.utils :as utils]
    [taoensso.timbre :as log]
    [memefactory.ui.utils :as ui-utils]
    [district.format :as format])

  (:require-macros [reagent.ratom :refer [reaction]]))

; sha3(MessageSent(bytes))
(def event-sig "0x8c5261668696ce22758910d05bab8f186d6eb247ceac2af2e82c7dc17669b036")

(def interceptors [re-frame/trim-v])


(re-frame/reg-event-fx
 ::approve-and-bridge-dank-to-l2
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :to :amount] :as args}]]
   (let [tx-name (gstring/format "Transferring %s to L2 account %s" (ui-utils/format-dank amount) to)
         address (contract-queries/contract-address db :DANK-root-tunnel)
         active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :DANK-root-tunnel address)
                                                :deposit-from
                                                to
                                                to
                                                amount)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK-root)
                                      :fn :approve-and-call
                                      :args [address
                                             amount
                                             extra-data]
                                      :tx-opts {:from active-account}
                                      :tx-id {::approve-and-bridge-dank-to-l2 id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.bridge/index}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::approve-and-bridge-dank-to-l2]
                                                        [::notification-events/show
                                                         (gstring/format "Successfully transferred %s to L2 account %s" (ui-utils/format-dank amount) to)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::approve-and-bridge-dank-to-l2]}]})))


(re-frame/reg-event-fx
 ::bridge-meme-to-l2
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :token-ids] :as args}]]
   (let [active-account (account-queries/active-account db)
         tx-name (gstring/format "Transferring Memes to L2 account %s" active-account)
         to (contract-queries/contract-address db :meme-token-root-tunnel)
         [fn args] (if (= 1 (count token-ids))
                          [:safe-transfer-from [active-account to (first token-ids)]]
                          [:safe-transfer-from-multi [active-account to token-ids nil]])]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-token-root)
                                      :fn fn
                                      :args args
                                      :tx-opts {:from active-account}
                                      :tx-id {::bridge-meme-to-l2 id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.bridge/index}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::bridge-meme-to-l2]
                                                        [::notification-events/show
                                                         (gstring/format "Successfully transferred Memes to L2 account %s" active-account)]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::bridge-meme-to-l2]}]})))


(re-frame/reg-event-fx
 ::start-withdraw-dank
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :amount] :as args}]]
   (let [tx-name (gstring/format "Withdraw %s to L1" (ui-utils/format-dank amount))
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK-child-tunnel)
                                      :fn :withdraw
                                      :args [amount]
                                      :tx-opts {:from active-account}
                                      :tx-id {::start-withdraw-dank id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.bridge/index}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::start-withdraw-dank]
                                                        [::notification-events/show
                                                         (gstring/format "Successfully withdraw %s to L1" (ui-utils/format-dank amount))]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::start-withdraw-dank]}]})))


(re-frame/reg-event-fx
 ::start-withdraw-meme-token
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id :token-ids] :as args}]]
   (let [tx-name "Withdraw Meme-token to L1"
         active-account (account-queries/active-account db)
         [fn arguments] (if (= 1 (count token-ids))
              [:withdraw (first token-ids)]
              [:withdraw-batch token-ids])]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-token-child-tunnel)
                                      :fn fn
                                      :args [arguments]
                                      :tx-opts {:from active-account}
                                      :tx-id {::start-withdraw-meme-token id}
                                      :tx-log {:name tx-name
                                               :related-href {:name :route.bridge/index}}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::start-withdraw-meme-token]
                                                        [::notification-events/show "Successfully withdraw Meme-token to L1"]]
                                      :on-tx-error [::logging/error (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :args args}
                                                    ::start-withdraw-meme-token]}]})))


(re-frame/reg-fx
  ::call-exit-manager
  (fn [{:keys [:web3 :withdraw-tx :network :version version :on-success :on-error :on-complete :method] :as params}]
    (let [options {:network network :version version :parent-provider (aget web3 "currentProvider")}
          pos (.call (goog.object/get js/Matic "MaticPOSClient") js/Matic (cljkk->js options))
          method (name (cs/->camelCase method))]
      (-> (.. pos -posRootChainManager -exitManager)
          (js-invoke method withdraw-tx event-sig)
          (.then (fn [result]
                    (re-frame/dispatch (conj (vec on-success) result))))
          (.catch (fn [ex]
                    (re-frame/dispatch (conj (vec on-error) ex))))
          (.finally #(when on-complete (re-frame/dispatch on-complete)))))))


(re-frame/reg-event-fx
  ::build-exit-payload
  [interceptors]
  (fn [{:keys [:db]} [{:keys [:on-success :withdraw-tx :tx-id testnet?] :as args}]]
    (let [[network version] (if testnet? ["testnet" "mumbai"] ["mainnet" "v1"])]
      {::call-exit-manager {:network network
                            :version version
                            :method :build-payload-for-exit
                            :web3 (web3-queries/web3 db)
                            :withdraw-tx withdraw-tx
                            :on-success on-success
                            :on-error [::logging/error "build payload error"
                                       {:args args}
                                       ::build-exit-payload]
                            :on-complete [::payload-build tx-id]}
       :db (assoc-in db [:bridge :building-payload? tx-id] true)})))


(re-frame/reg-event-fx
  ::payload-build
  [interceptors]
  (fn [{:keys [:db]} [tx-id]]
    {:db (assoc-in db [:bridge :building-payload? tx-id] false)}))


(re-frame/reg-sub
  ::building-payload?
  (fn [db [_ tx-id]]
    (get-in db [:bridge :building-payload? tx-id])))


(defn exit-token [db root-tunnel-contract {:keys [:send-tx/id] :as args} exit-payload]
  (let [tx-name "Claim withdraw transaction"
        active-account (account-queries/active-account db)]
    {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db root-tunnel-contract)
                                     :fn :receive-message
                                     :args [exit-payload]
                                     :tx-opts {:from active-account}
                                     :tx-id {::exit-token id}
                                     :tx-log {:name tx-name
                                              :related-href {:name :route.bridge/index}}
                                     :on-tx-success-n [[::logging/info (str tx-name " tx success") :exit-token]
                                                       [::notification-events/show
                                                        "Successfully claimed withdraw transaction"]]
                                     :on-tx-error [::logging/error (str tx-name " tx error")
                                                   {:user {:id active-account}
                                                    :args args}
                                                   :exit-token]}]}))


(re-frame/reg-event-fx
 ::exit-dank
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id] :as args} exit-payload]]
   (exit-token db :DANK-root-tunnel args exit-payload)))


(re-frame/reg-event-fx
 ::exit-meme-token
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:send-tx/id] :as args} exit-payload]]
   (exit-token db :meme-token-root-tunnel args exit-payload)))


(re-frame/reg-event-fx
  ::add-withdraw
  [interceptors]
  (fn [{:keys [:db]} [withdraw-tx exited?]]
    (when-not exited?
      {:db (assoc-in db [:bridge :filter-withdraws] (conj (get-in db [:bridge :filter-withdraws]) withdraw-tx))})))


(re-frame/reg-event-fx
  ::check-exited
  [interceptors]
  (fn [{:keys [:db]} [{:keys [:withdraw-tx :tunnel-contract]} exit-hash]]
    {:web3/call {:web3 (web3-queries/web3 db)
                 :fns [{:instance (contract-queries/instance db tunnel-contract)
                        :fn :processed-exits
                        :args [exit-hash]
                        :on-success [::add-withdraw withdraw-tx]
                        }]}}))


(re-frame/reg-event-fx
  ::build-exit-hash
  [interceptors]
  (fn [{:keys [:db]} [{:keys [:withdraw-tx :tunnel-contract testnet?] :as args}]]
    (let [[network version] (if testnet? ["testnet" "mumbai"] ["mainnet" "v1"])]
      {::call-exit-manager {:network network
                            :version version
                            :method :get-exit-hash
                            :web3 (web3-queries/web3 db)
                            :withdraw-tx withdraw-tx
                            :on-success [::check-exited {:withdraw-tx withdraw-tx :tunnel-contract tunnel-contract}]
                            :on-error [::logging/error "build exit hash error"
                                       {:args args}
                                       ::build-exit-hash]}})))


(re-frame/reg-event-fx
  ::build-exit-hashes
  [interceptors]
  (fn [{:keys [:db]} [{:keys [:withdraw-txs :tunnel-contract testnet?]}]]
    {:dispatch-n (mapv #(vec [::build-exit-hash {:withdraw-tx % :tunnel-contract tunnel-contract :testnet? testnet?}]) withdraw-txs)
     :db (assoc-in db [:bridge :filter-withdraws] (sorted-set))}))


(re-frame/reg-sub-raw
  ::filter-withdraws
  (fn [db [_ withdraw-txs tunnel-contract testnet?]]
    (re-frame/dispatch [::build-exit-hashes {:withdraw-txs withdraw-txs
                                             :tunnel-contract tunnel-contract
                                             :testnet? testnet?}])
    (reaction (get-in @db [:bridge :filter-withdraws]))))


(re-frame/reg-sub-raw
  ::meme-token-ids
  (fn [db [_ account]]
    (re-frame/dispatch [::lookup-meme-tokens account])
    (reaction (get-in @db [:memefactory.ui.contract.bridge.meme-token-ids (keyword account)]))))


(re-frame/reg-event-fx
  ::lookup-meme-tokens
  interceptors
  (fn [{:keys [:db]} [account]]
    (let [instance (contract-queries/instance db :meme-token-root)]
      {:web3/call
       {:web3 (web3-queries/web3 db)
        :fns [{:instance instance
               :fn :balance-of
               :args [account]
               :on-success [::lookup-meme-tokens-ids account]
               :on-error [::logging/error "Error calling Meme token root" account ::lookup-meme-tokens]}]}
       :db (assoc-in db [:memefactory.ui.contract.bridge.meme-token-ids (keyword account)] (sorted-set))})))


(re-frame/reg-event-fx
  ::lookup-meme-tokens-ids
  interceptors
  (fn [{:keys [:db]} [account token-count]]
    (let [instance (contract-queries/instance db :meme-token-root)
          fns (for [token-index (range token-count)]
                {:instance instance
                 :fn :token-of-owner-by-index
                 :args [account token-index]
                 :on-success [::assoc-tokens-ids account]
                 :on-error [::logging/error "Error calling Meme token root" account token-index ::lookup-meme-tokens-ids]})]
      (when (not-empty fns)
        {:web3/call
         {:web3 (web3-queries/web3 db)
          :fns fns}}))))


(defn assoc-token-id [db account token-id]
  (let [ks [:memefactory.ui.contract.bridge.meme-token-ids (keyword account)]]
    (assoc-in db ks (into (sorted-set) (conj (get-in db ks) token-id)))))


(re-frame/reg-event-fx
  ::assoc-tokens-ids
  interceptors
  (fn [{:keys [:db]} [account token-id]]
    {:db (assoc-token-id db account (bn/fixed token-id))}))
