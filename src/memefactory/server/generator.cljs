(ns memefactory.server.generator
  (:require [bignumber.core :as bn]
            [cljs-ipfs-api.files :as ipfs-files]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs-web3.utils :refer [js->cljkk camel-case]]
            [district.cljs-utils :refer [rand-str]]
            [district.format :as format]
            [district.server.config :refer [config]]
            [district.server.smart-contracts :refer [smart-contracts contract-address contract-call instance wait-for-tx-receipt]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-auction :as meme-auction]
            [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
            [memefactory.server.contract.meme-factory :as meme-factory]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.contract.meme-token :as meme-token]
            [memefactory.server.contract.minime-token :as minime-token]
            [memefactory.server.contract.param-change :as param-change]
            [memefactory.server.contract.param-change-factory :as param-change-factory]
            [memefactory.server.contract.param-change-registry :as param-change-registry]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.macros :refer [promise->]]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [taoensso.timbre :as log]
            [mount.core :as mount :refer [defstate]]
            [print.foo :refer [look] :include-macros true]
            [district.shared.error-handling :refer [try-catch]]))

(def fs (js/require "fs"))

(defn get-scenarios [{:keys [:accounts :use-accounts :items-per-account :scenarios]}]
  (when (and (pos? use-accounts)
             (pos? items-per-account)
             (seq scenarios))
    (let [accounts-repeated (flatten (for [x accounts]
                                       (repeat items-per-account x)))
          scenarios (map #(if (keyword? %) {:scenario-type %} %) scenarios)
          scenarios-repeated (take (* use-accounts items-per-account) (cycle scenarios))]
      (partition 2 (interleave accounts-repeated scenarios-repeated)))))

(defn upload-meme [previous]
  (let [file "resources/dev/pepe.png"]
    (log/info "Uploading file" {:path file} ::upload-meme)
    (js/Promise.
     (fn [resolve reject]
       (.readFile fs
                  file
                  (fn [err data]
                    (if err
                      (reject err)
                      (ipfs-files/add
                       data
                       (fn [err {image-hash :Hash}]
                         (if err
                           (do
                             (log/error "ifps error" {:error err} ::upload-meme-meta)
                             (reject err))
                           (do
                             (log/info (str "Uploaded " file " received") {:image-hash image-hash} ::upload-meme)
                             (resolve (assoc previous :image-hash image-hash)))))))))))))

(defn upload-meme-meta [{:keys [:image-hash] :as previous}]
  (let [meta-info (format/clj->json {:title "PepeSmile"
                                     :image-hash image-hash
                                     :search-tags ["pepe" "frog" "dank"]
                                     :comment "did not like it"})]
    (log/info "Uploading meta" {:meta-info meta-info} ::upload-meme-meta)
    (js/Promise.
     (fn [resolve reject]
       (ipfs-files/add
        (js/Buffer.from meta-info)
        (fn [err {meta-hash :Hash}]
          (if err
            (do
              (log/error "ifps error" {:error err} ::upload-meme-meta)
              (reject err))
            (do
              (log/info "Uploaded meta received " {:meta-hash meta-hash} ::upload-meme-meta)
              (resolve (assoc previous :meta-hash meta-hash))))))))))

(defn get-meme-registry-db-values [previous]
  (let [meme-registry-db-keys [:max-total-supply :max-auction-duration :deposit :commit-period-duration :reveal-period-duration]]
    (promise-> (eternal-db/get-uint-values :meme-registry-db meme-registry-db-keys)
               #(let [meme-registry-db-values (zipmap meme-registry-db-keys (map bn/number %))]
                  (merge previous {:meme-registry-db-values meme-registry-db-values
                                                      :total-supply (inc (rand-int (:max-total-supply meme-registry-db-values)))})))))

;; TODO: destructure previous
(defn create-meme [previous account]
  (promise-> (meme-factory/approve-and-create-meme {:meta-hash (:meta-hash previous)
                                                    :total-supply (:total-supply previous)
                                                    :amount (get-in previous [:meme-registry-db-values :deposit])}
                                                   {:from account})
             #(wait-for-tx-receipt %)
             #(let [{{:keys [registry-entry creator]} :args} (registry/meme-constructed-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                    (:transaction-hash %))]
                (assoc previous :meme {:registry-entry registry-entry
                                       :creator creator}))))

(defn challenge-meme [previous account]
  (promise-> (registry-entry/approve-and-create-challenge (get-in previous [:meme :registry-entry])
                                                          {:meta-hash (:meta-hash previous)
                                                           :amount (get-in previous [:meme-registry-db-values :deposit])}
                                                          {:from account})
             #(wait-for-tx-receipt %)
             #(assoc previous :challenge-meme-tx (:transaction-hash %))))

(defn commit-vote [previous account]
  (promise-> (registry-entry/approve-and-commit-vote (get-in previous [:meme :registry-entry])
                                                     {:amount 2
                                                      :salt "abc"
                                                      :vote-option :vote.option/vote-for}
                                                     {:from account})
             #(wait-for-tx-receipt %)
             #(assoc previous :commit-vote-tx (:transaction-hash %))))

(defn increase-time [previous millis]
  (js/Promise.
   (fn [resolve reject]
     (web3-evm/increase-time! @web3 [millis]
                              (fn [error success]
                                (if error
                                  (reject error)
                                  (resolve previous)))))))

(defn reveal-vote [{{:keys [:registry-entry]} :meme :as previous} account]
  (promise-> (registry-entry/reveal-vote registry-entry
                                         {:vote-option :vote.option/vote-for
                                          :salt "abc"}
                                         {:from account})
             #(wait-for-tx-receipt %)
             #(assoc previous :reveal-vote-tx (:transaction-hash %))))

(defn claim-vote-reward [{{:keys [:registry-entry]} :meme :as previous} account]
  (promise-> (registry-entry/claim-vote-reward registry-entry {:from account})
             #(wait-for-tx-receipt %)
             #(assoc previous :claim-vote-reward-tx (:transaction-hash %))))


(defn mint-meme-tokens [{:keys [:total-supply] {:keys [:registry-entry]} :meme :as previous} account]
  (promise-> (meme/mint registry-entry (dec total-supply) {:from account})
             #(wait-for-tx-receipt %)
             #(let [{{:keys [:token-start-id :token-end-id]} :args} (registry/meme-minted-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                      (:transaction-hash %))]
                (assoc-in previous [:meme :token-ids] (range (bn/number token-start-id)
                                                             (inc (bn/number token-end-id)))))))

(defn start-auction [{{:keys [:token-ids]} :meme {:keys [:max-auction-duration]} :meme-registry-db-values :as previous} account]
  (promise-> (meme-token/transfer-multi-and-start-auction {:from account
                                                           :token-ids token-ids
                                                           :start-price (web3/to-wei 0.1 :ether)
                                                           :end-price (web3/to-wei 0.01 :ether)
                                                           :duration (+ 60 (rand-int (- max-auction-duration 60)))
                                                           :description "some auction"}
                                                          {:from account})
             #(wait-for-tx-receipt %)
             #(let [{{:keys [:meme-auction :token-id]} :args} (meme-auction-factory/meme-auction-started-event-in-tx (:transaction-hash %))]
                (assoc previous :meme-auction {:meme-auction meme-auction
                                               :token-id token-id}))))


(defn buy-auction [{{:keys [:meme-auction]} :meme-auction :as previous} account]
  (promise-> (meme-auction/buy meme-auction {:from account
                                             :value (web3/to-wei 0.1 :ether)})
             #(wait-for-tx-receipt %)
             #(assoc previous :buy-tx (:transaction-hash %))))

(defn generate-memes [{:keys [:accounts]}]
  (let [account (first accounts)
        previous {}]
    (promise-> (upload-meme previous)
               ;; #(.foo %)
               #(upload-meme-meta %)
               #(get-meme-registry-db-values %)
               #(create-meme % account)
               #(challenge-meme % account)
               #(commit-vote % account)
               #(increase-time % (inc (get-in % [:meme-registry-db-values :commit-period-duration])))
               #(reveal-vote % account)
               #(increase-time % (inc (get-in % [:meme-registry-db-values :reveal-period-duration])))
               #(claim-vote-reward % account)
               #(mint-meme-tokens % account)
               #(start-auction % account)
               #(buy-auction % account)
               #(log/info "Generate meme result" % ::generate-memes))))
