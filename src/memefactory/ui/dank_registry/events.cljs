(ns memefactory.ui.dank-registry.events
  (:require
   [clojure.string :as string]
   [district.ui.logging.events :as logging]
   [memefactory.ui.contract.meme-factory :as meme-factory]
   [memefactory.ui.utils :as utils]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [district.shared.error-handling :refer [try-catch]]
   [taoensso.timbre :as log]))

(defn build-meme-meta-string [{:keys [title search-tags issuance comment]} image-hash]
  (-> {:name title
       :description (string/trim (or comment ""))
       :external_url "https://memefactory.io/"
       :image (str "ipfs://" image-hash)
       :attributes {
                    :search-tags search-tags
                    :issuance issuance}}
      clj->js
      js/JSON.stringify))

(re-frame/reg-event-fx
 ::upload-meme
 (fn [_ [_ {:keys [file-info] :as data} deposit]]
   (log/info "Uploading meme image" {:file file-info} ::upload-meme)
   {:ipfs/call {:func "add"
                :args [(:file file-info)]
                :opts {:wrap-with-directory true}
                :on-success [::upload-meme-meta data deposit]
                :on-error [::logging/error "upload-meme ipfs call error" {:data data
                                                                          :deposit deposit}
                           ::upload-meme]}}))

(re-frame/reg-event-fx
 ::upload-meme-meta
 (fn [{:keys [db]} [_ data deposit ipfs-response]]
   (log/info "Meme image uploaded" {:ipfs-response ipfs-response} ::upload-meme-meta)
   (try-catch
    (let [resp (utils/parse-ipfs-response ipfs-response)
          image-hash (str (-> resp last :Hash) "/" (-> resp first :Name))
          meme-meta (build-meme-meta-string data image-hash)
          buffer-data (js/buffer.Buffer.from meme-meta)]
      (log/info "Uploading meme meta" {:meme-meta meme-meta} ::upload-meme-meta)
      {:ipfs/call {:func "add"
                   :args [buffer-data]
                   :on-success [::meme-factory/approve-and-create-meme data deposit]
                   :on-error [::logging/error "upload-meme-meta ipfs call error"
                              {:data data
                               :deposit deposit
                               :meme-meta meme-meta}
                              ::upload-meme-meta]}}))))
