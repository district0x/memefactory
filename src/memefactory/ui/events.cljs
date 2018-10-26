(ns memefactory.ui.events
  (:require
   [cljsjs.buffer]
   [district.ui.logging.events :as logging]
   [memefactory.ui.contract.registry-entry :as registry-entry]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   ))

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
