(ns memefactory.ui.events
  (:require [cljsjs.buffer]
            [cljs.reader :as reader]
            [district.cljs-utils :as cljs-utils]
            [district.ui.logging.events :as logging]
            [memefactory.shared.utils :as shared-utils]
            [memefactory.ui.contract.registry-entry :as registry-entry]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]))

(def interceptors [re-frame/trim-v])

(defn- build-challenge-meta-string [{:keys [comment] :as data}]
  (-> {:comment comment}
      clj->js
      js/JSON.stringify))

;; Adds the challenge to ipfs and if successfull dispatches ::create-challenge
(re-frame/reg-event-fx
 ::add-challenge
 (fn [{:keys [db]} [_ {:keys [:reg-entry/address :comment] :as data}]]
   (let [challenge-meta (build-challenge-meta-string {:comment comment})
         buffer-data (js/buffer.Buffer.from challenge-meta)]
     (log/info "Uploading challenge meta" {:meta challenge-meta} ::add-challenge)
     {:ipfs/call {:func "add"
                  :args [buffer-data]
                  :on-success [::registry-entry/approve-and-create-challenge data]
                  :on-error [::logging/error "add-challenge ipfs call error" data ::add-challenge]}})))

(re-frame/reg-fx
 :file/write
 (fn [[filename content & [mime-type]]]
   (shared-utils/file-write filename content mime-type)))

(re-frame/reg-event-fx
 ::backup-vote-secrets
 [interceptors (re-frame/inject-cofx :store)]
 (fn [{:keys [:store]} [{:keys [:file/filename]}]]
   (let [filename "memefactory_vote_secrets.edn"
         votes (str (:votes store))]
     {:file/write [filename votes "text/plain"]})))

(re-frame/reg-event-fx
 ::import-vote-secrets
 [interceptors (re-frame/inject-cofx :store)]
 (fn [{:keys [:db :store]} [data-string]]
   (let [votes (reader/read-string data-string)]
     {:store (cljs-utils/merge-in store {:votes votes})
      :db (cljs-utils/merge-in db {:votes votes})})))
