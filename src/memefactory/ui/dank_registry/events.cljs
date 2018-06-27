(ns memefactory.ui.dank-registry.events
  (:require [re-frame.core :as re-frame]
            [district0x.re-frame.ipfs-fx]
            [cljsjs.buffer]
            [district.ui.logging.events :as logging]
            [district.ui.notification.events :as notification-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as account-queries]
            [district.ui.web3-tx.events :as tx-events]))

(re-frame/reg-event-fx
 ::init-ipfs
 (fn [_ [_ config]]
   {:ipfs/init config}))                                                                     

(re-frame/reg-event-fx
 ::upload-meme
 (fn [_ [_ {:keys [file-info title search-tags issuance] :as data}]]
   (let [buffer-data (js/buffer.Buffer.from (:array-buffer file-info))]
     {:ipfs/call {:func "add"
                  :args [buffer-data]
                  :on-success [::on-upload-meme-success data]
                  :on-error ::error}})))

(re-frame/reg-event-fx
 ::on-upload-meme-success
 (fn [{:keys [db]} [_ {:keys [array-buffer title search-tags issuance] :as data} {:keys [Name Hash Size]}]]
   (prn "Meme image uploaded with hash " Hash " data " data)
   (let [active-account (account-queries/active-account db)]
     (prn "Active account " active-account)
     #_{:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-factory)
                                      :fn :create-meme
                                      :args [active-account
                                             Hash
                                             issuance]
                                      :tx-opts {:from active-account
                                                :gas 6000000}
                                      :tx-id {:meme-auction/buy id}
                                      :on-tx-success-n [[::logging/success [:meme-auction/buy]]
                                                        [::notification-events/show (gstring/format "Auction %s is now yours" address)]]
                                      :on-tx-hash-error [::logging/error [:meme-auction/buy]]
                                      :on-tx-error [::logging/error [:meme-auction/buy]]}]})))



(re-frame/dispatch [::init-ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0"}])


