(ns memefactory.ui.dank-registry.events
  (:require [re-frame.core :as re-frame]
            [district0x.re-frame.ipfs-fx]
            [cljsjs.buffer]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]
            [goog.string :as gstring]
            [print.foo :refer [look] :include-macros true]
            [bignumber.core :as bn]))

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

(defn build-challenge-meta-string [{:keys [reason] :as data}]
  (-> {:reason reason}
      clj->js
      js/JSON.stringify))

;; Adds the challenge to ipfs and if successfull dispatches ::create-challenge
(re-frame/reg-event-fx
 ::add-challenge
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :reason] :as data}]]
   (let [challenge-meta (build-challenge-meta-string {:reason reason})
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
         ;; TODO grab this from registry
         deposit "1000000000000000000000"
         extra-data (look (web3-eth/contract-get-data (look (contract-queries/instance db :meme address))
                                                      :create-challenge
                                                      (look active-account)
                                                      (look Hash)))]
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
 ::collect-reward
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :send-tx/id]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :claim-vote-reward
                                      :args [active-account]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/collect-reward id}
                                      :on-tx-success-n [[::logging/success [:meme/collect-reward]]
                                                        [::notification-events/show "Reward collected"]]
                                      :on-tx-hash-error [::logging/error [:meme/collect-reward]]
                                      :on-tx-error [::logging/error [:meme/collect-reward]]}]})))

(re-frame/reg-event-fx
 ::commit-vote
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :send-tx/id :vote/amount :vote/option]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)
         salt "salt"
         secret-hash (str option salt)] ;; TODO calculate secret hash here
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :commit-vote
                                      :args [active-account amount secret-hash]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/commit-vote id}
                                      :on-tx-success-n [[::logging/success [:meme/commit-vote]]
                                                        [::notification-events/show "Voted"]]
                                      :on-tx-hash-error [::logging/error [:meme/commit-vote]]
                                      :on-tx-error [::logging/error [:meme/commit-vote]]}]
      ;; TODO assoc secret hash here so we can submit it after
      ;; :db (assoc db )

      })))

(re-frame/reg-event-fx
 ::reveal-vote
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :send-tx/id :vote/option]} {:keys [Hash]}]]
   (let [active-account (account-queries/active-account db)
         salt "salt"] ;; TODO calculate secret hash here
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme address)
                                      :fn :reveal-vote
                                      :args [option salt] ;; TODO how option should be encoded
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme/commit-vote id}
                                      :on-tx-success-n [[::logging/success [:meme/commit-vote]]
                                                        [::notification-events/show "Voted"]]
                                      :on-tx-hash-error [::logging/error [:meme/commit-vote]]
                                      :on-tx-error [::logging/error [:meme/commit-vote]]}]})))




(re-frame/dispatch [::init-ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0"}])


