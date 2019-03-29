(ns memefactory.ui.dank-registry.events
  (:require
   [district.ui.logging.events :as logging]
   [district.ui.notification.events :as notification-events]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [district0x.re-frame.ipfs-fx :as ipfs-fx]
   [memefactory.ui.contract.meme-factory :as meme-factory]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   [clojure.string :as str]
   ))

(re-frame/reg-event-fx
 ::upload-meme
 (fn [_ [_ {:keys [file-info] :as data} deposit]]
   (log/info "Uploading meme file" {:file file-info} ::upload-meme)
   {:ipfs/call {:func "add"
                :args [(:file file-info)]
                :on-success [::upload-meme-meta data deposit]
                :on-error [::logging/error "upload-meme ipfs call error" {:data data
                                                                          :deposit deposit}
                           ::upload-meme]}}))

(defn build-meme-meta-string [{:keys [title search-tags issuance comment] :as data} image-hash]
  (-> {:title title
       :comment (str/trim (or comment ""))
       :search-tags search-tags
       :issuance issuance
       :image-hash image-hash}
      clj->js
      js/JSON.stringify))

(re-frame/reg-event-fx
 ::upload-meme-meta
 (fn [{:keys [db]} [_ data deposit {:keys [Name Hash Size] :as meta}]]
   (log/info "Meme image uploaded with hash" {:hash Hash} ::upload-meme-meta)
   (let [meme-meta (build-meme-meta-string data Hash)
         buffer-data (js/buffer.Buffer.from meme-meta)]
     (log/info "Uploading meme meta" meta ::upload-meme-meta)
     {:ipfs/call {:func "add"
                  :args [buffer-data]
                  :on-success [::meme-factory/approve-and-create-meme data deposit]
                  :on-error [::logging/error "upload-meme-meta ipfs call error"
                             {:data data
                              :deposit deposit
                              :meta meta}
                             ::upload-meme-meta]}})))
