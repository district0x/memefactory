(ns memefactory.ui.contract.param-change
  (:require [district.ui.logging.events :as logging]
            [clojure.string :as str]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [district.ui.web3-tx.events :as tx-events]
            [district.ui.notification.events :as notification-events]
            [district.ui.web3-accounts.queries :as account-queries]
            [memefactory.ui.utils :as utils]
            [cljs-web3.eth :as web3-eth]
            [district.ui.smart-contracts.queries :as contract-queries]
            [print.foo :refer [look] :include-macros true]
            [district.graphql-utils :as gql-utils]))

(defn build-param-change-meta-string [{:keys [reason]}]
  (-> {:reason (str/trim (or reason ""))}
      clj->js
      js/JSON.stringify))


(re-frame/reg-event-fx
 ::upload-and-add-param-change
 (fn [{:keys [db]} [_ {:keys [:deposit :reason :key :value :send-tx/id] :as data}]]
   (let [pc-meta (build-param-change-meta-string data)
         buffer-data (js/buffer.Buffer.from pc-meta)]
     (log/info "Uploading param-change meta" pc-meta ::upload-and-add-param-change)
     {:ipfs/call {:func "add"
                  :args [buffer-data]
                  :on-success [::create-param-change data]
                  :on-error [::logging/error "upload-and-add-param-change ipfs call error"
                             {:data data
                              :deposit deposit
                              :meta pc-meta}
                             ::upload-and-add-param-change]}})))

(re-frame/reg-event-fx
 ::create-param-change
 (fn [{:keys [db]} [_ {:keys [:deposit :reason :key :value :send-tx/id] :as data} ipfs-response]]
   (log/info "Param change meta uploaded with hash" {:ipfs-response ipfs-response} ::create-param-change)
   (let [resp (utils/parse-ipfs-response ipfs-response)
         meta-hash (-> resp last :Hash)
         tx-id id
         tx-name "Parameter change submitted"
         active-account (account-queries/active-account db)
         extra-data (web3-eth/contract-get-data (contract-queries/instance db :param-change-factory)
                                                :create-param-change
                                                active-account
                                                (contract-queries/contract-address db :meme-registry-db)
                                                (gql-utils/kw->gql-name key)
                                                value
                                                meta-hash)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :DANK)
                                      :fn :approve-and-call
                                      :args [(contract-queries/contract-address db :param-change-factory)
                                             deposit
                                             extra-data]
                                      :tx-opts {:from active-account}
                                      :tx-id {:meme/create-param-change tx-id}
                                      :tx-log {:name tx-name}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::create-param-change]
                                                        [::notification-events/show "Parameter change was successfully submitted" ]
                                                        [:memefactory.ui.config/load-memefactory-db-params]
                                                        [::create-param-change-success]]
                                      :on-tx-error [::logging/error
                                                    (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :deposit deposit
                                                     :data data}
                                                    ::create-param-change]}]})))

(re-frame/reg-event-fx ::create-param-change-success (fn []))

(re-frame/reg-event-fx
 ::apply-param-change
 (fn [{:keys [db]} [_ {:keys [:send-tx/id :reg-entry/address] :as data}]]
   (let [tx-id id
         tx-name "Parameter change applied"
         active-account (account-queries/active-account db)]
     {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db
                                                                           :param-change-registry
                                                                           (contract-queries/contract-address db :param-change-registry-fwd))
                                      :fn :apply-param-change
                                      :args [address]
                                      :tx-opts {:from active-account}
                                      :tx-id {:param-change/apply-change tx-id}
                                      :tx-log {:name tx-name}
                                      :on-tx-success-n [[::logging/info (str tx-name " tx success") ::create-param-change]
                                                        [::notification-events/show "Parameter change was successfully applied" ]
                                                        [:memefactory.ui.config/load-memefactory-db-params]]
                                      :on-tx-error [::logging/error
                                                    (str tx-name " tx error")
                                                    {:user {:id active-account}
                                                     :data data}
                                                    ::apply-param-change]}]})))
