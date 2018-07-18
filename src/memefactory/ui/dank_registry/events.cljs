(ns memefactory.ui.dank-registry.events
  (:require [re-frame.core :as re-frame]
            [district0x.re-frame.ipfs-fx]
            [cljsjs.buffer]
            [cljs-web3.core :as web3]
            [cljs-solidity-sha3.core :refer [solidity-sha3]]
            [district.cljs-utils :as dutils]
            [cljs-web3.eth :as web3-eth]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [goog.string :as gstring]
            [print.foo :refer [look] :include-macros true]
            [bignumber.core :as bn]
            [memefactory.shared.contract.registry-entry :as reg-entry]
            [akiroz.re-frame.storage :refer [reg-co-fx!]]))

(reg-co-fx! :my-app         ;; local storage key
            {:fx :store     ;; re-frame fx ID
             :cofx :store}) ;; re-frame cofx ID

(re-frame/reg-event-fx
 ::init-ipfs
 (fn [_ [_ config]]
   {:ipfs/init config}))                                                                     

(re-frame/reg-event-fx
 ::upload-meme
 (fn [_ [_ {:keys [file-info] :as data} deposit]]
   (let [buffer-data (js/buffer.Buffer.from (:array-buffer file-info))]
     {:ipfs/call {:func "add"
                  :args [buffer-data]
                  :on-success [::upload-meme-meta data deposit]
                  :on-error ::error}})))

(defn build-meme-meta-string [{:keys [title search-tags issuance] :as data} image-hash]
  (-> {:title title
       :search-tags search-tags
       :issuance issuance
       :image-hash image-hash}
      clj->js
      js/JSON.stringify))

(re-frame/reg-event-fx
 ::upload-meme-meta
 (fn [{:keys [db]} [_ data deposit {:keys [Name Hash Size]}]]
   (prn "Meme image uploaded with hash " Hash " data " data)
   (let [meme-meta (build-meme-meta-string data Hash)
         buffer-data (js/buffer.Buffer.from meme-meta)]
     (prn "Uploading meme meta " meme-meta)
     {:ipfs/call {:func "add"
                  :args [buffer-data]
                  :on-success [::create-meme data deposit]
                  :on-error ::error}})))

(re-frame/reg-event-fx
 ::create-meme
 (fn [{:keys [db]} [_ data deposit {:keys [Name Hash Size]}]]
   (prn "Meme meta uploaded with hash " Hash)
   (let [tx-id (str (random-uuid))
         active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme-factory)
                                                :create-meme
                                                active-account
                                                Hash
                                                (bn/number (:issuance data)))]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args (look [(contract-queries/contract-address db :meme-factory)
                                                   deposit
                                                   extra-data])
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/create-meme tx-id}
                                      :on-tx-success-n [[::logging/success [:meme/create-meme]]
                                                        [::notification-events/show (gstring/format "Meme created with meta hash %s" Hash)]]
                                      :on-tx-hash-error [::logging/error [:meme/create-meme]]
                                      :on-tx-error [::logging/error [:meme/create-meme]]}]})))

(defn build-challenge-meta-string [{:keys [comment] :as data}]
  (-> {:comment comment}
      clj->js
      js/JSON.stringify))

;; Adds the challenge to ipfs and if successfull dispatches ::create-challenge
(re-frame/reg-event-fx
 ::add-challenge
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :comment] :as data}]]
   (let [challenge-meta (build-challenge-meta-string {:comment comment})
         buffer-data (js/buffer.Buffer.from challenge-meta)]
     (prn "Uploading challenge  meta " challenge-meta)
     {:ipfs/call {:func "add"
                  :args [buffer-data]
                  :on-success [::create-challenge data]
                  :on-error ::error}})))

(re-frame/reg-event-fx
 ::create-challenge
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :send-tx/id :deposit]} {:keys [Hash]}]]
   (prn "Challenge meta created with hash " Hash)
   (let [active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme address)
                                                :create-challenge
                                                active-account
                                                Hash)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args [address
                                             deposit
                                             extra-data]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/create-challenge id} 
                                      :on-tx-success-n [[::logging/success [:meme/create-challenge]]
                                                        [::notification-events/show (gstring/format "Challenge created for %s with metahash %s"
                                                                                                    address Hash)]]
                                      :on-tx-hash-error [::logging/error [:meme/create-challenge]]
                                      :on-tx-error [::logging/error [:meme/create-challenge]]}]})))

(re-frame/reg-event-fx
 ::collect-all-rewards
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :send-tx/id]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :claim-all-rewards
                                      :args [active-account]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/collect-all-rewards id}
                                      :on-tx-success-n [[::logging/success [:meme/collect-all-rewards]]
                                                        [::notification-events/show "All rewards collected"]]
                                      :on-tx-hash-error [::logging/error [:meme/collect-all-rewards]]
                                      :on-tx-error [::logging/error [:meme/collect-all-rewards]]}]})))
 
(re-frame/reg-event-fx
 ::commit-vote
  [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]} [_ {:keys [:reg-entry/address :send-tx/id :vote/amount :vote/option]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)
         salt (dutils/rand-str 5)
         secret-hash (solidity-sha3 (reg-entry/vote-option->num option) salt)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :meme address)
                                                :commit-vote
                                                active-account
                                                amount
                                                secret-hash)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args [address
                                             amount
                                             extra-data]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/commit-vote id}
                                      :on-tx-success-n [[::logging/success [:meme/commit-vote]]
                                                        [::notification-events/show "Voted"]]
                                      :on-tx-hash-error [::logging/error [:meme/commit-vote]]
                                      :on-tx-error [::logging/error [:meme/commit-vote]]}]
      :store (assoc-in store [:votes active-account address] {:option option :salt salt})})))

(re-frame/reg-event-fx
 ::reveal-vote
 [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]} [_ {:keys [:reg-entry/address :send-tx/id]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)
         {:keys [option salt]} (get-in store [:votes active-account address])]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :reveal-vote  
                                      :args (look [(reg-entry/vote-option->num option) salt])
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/reveal-vote id}
                                      :on-tx-success-n [[::logging/success [:meme/reveal-vote]]
                                                        [::notification-events/show "Voted"]]
                                      :on-tx-hash-error [::logging/error [:meme/reveal-vote]]
                                      :on-tx-error [::logging/error [:meme/reveal-vote]]}]})))




(re-frame/dispatch [::init-ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0"}])


