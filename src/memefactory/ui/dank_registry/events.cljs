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

(defn is-video? [file-info]
  (= "video/mp4" (:type file-info)))


(defn build-meme-meta-string [{:keys [title search-tags issuance comment file-info]} image-or-video-hash thumbnail-hash]
  (-> (merge {:name title
       :description (string/trim (or comment ""))
       :external_url "https://memefactory.io/"
       :image (str "ipfs://" (if (is-video? file-info)
                               thumbnail-hash
                               image-or-video-hash))
       :attributes {
                    :search-tags search-tags
                    :issuance issuance}}
         (when (is-video? file-info) {:animation_url (str "ipfs://" image-or-video-hash)}))
      clj->js
      js/JSON.stringify))

(re-frame/reg-event-fx
 ::upload-meme
 (fn [_ [_ {:keys [:form-data] :as data}]]
   (if (is-video? (:file-info form-data))
     {:dispatch [::upload-thumbnail data]}
     {:dispatch [::upload-meme-image data nil]})))


(re-frame/reg-event-fx
  ::upload-thumbnail
  (fn [_ [_ {:keys [:form-data] :as data}]]
    (log/info "Uploading video thumbnail" {:file (:video-thumbnail form-data)} ::upload-thumbnail)
    {:ipfs/call {:func "add"
                 :args [(:video-thumbnail form-data)]
                 :opts {:wrap-with-directory true}
                 :on-success [::upload-meme-image data]
                 :on-error [::logging/error "upload-thumbnail ipfs call error" {:data form-data}
                            ::upload-meme]}}))


(defn get-thumbnail-hash [ipfs-thumbnail-response]
  (let [resp (utils/parse-ipfs-response ipfs-thumbnail-response)
        thumbnail-hash (str (-> resp last :Hash) "/" (-> resp first :Name))]
    thumbnail-hash))

(re-frame/reg-event-fx
 ::upload-meme-image
 (fn [_ [_ {:keys [:form-data :deposit] :as data} ipfs-thumbnail-response]]
   (let [thumbnail-hash (when ipfs-thumbnail-response
                          (log/info "Video thumbnail uploaded" {:ipfs-response ipfs-thumbnail-response} ::upload-meme-image)
                          (get-thumbnail-hash ipfs-thumbnail-response))]
     (log/info "Uploading meme image" {:file (:file-info form-data)} ::upload-meme)
     {:ipfs/call {:func "add"
                  :args [(:file (:file-info form-data))]
                  :opts {:wrap-with-directory true}
                  :on-success [::upload-meme-meta (merge data {:thumbnail-hash thumbnail-hash})]
                  :on-error [::logging/error "upload-meme ipfs call error" {:data form-data}
                             ::upload-meme]}})))

(re-frame/reg-event-fx
 ::upload-meme-meta
 (fn [{:keys [db]} [_ {:keys [:form-data :thumbnail-hash] :as data} ipfs-response]]
   (log/info "Meme image uploaded" {:ipfs-response ipfs-response} ::upload-meme-meta)
   (try-catch
    (let [resp (utils/parse-ipfs-response ipfs-response)
          image-or-video-hash (str (-> resp last :Hash) "/" (-> resp first :Name))
          meme-meta (build-meme-meta-string form-data image-or-video-hash thumbnail-hash)
          buffer-data (js/buffer.Buffer.from meme-meta)]
      (log/info "Uploading meme meta" {:meme-meta meme-meta} ::upload-meme-meta)
      {:ipfs/call {:func "add"
                   :args [buffer-data]
                   :on-success [::meme-factory/approve-and-create-meme data]
                   :on-error [::logging/error "upload-meme-meta ipfs call error"
                              {:data form-data
                               :meme-meta meme-meta}
                              ::upload-meme-meta]}}))))
