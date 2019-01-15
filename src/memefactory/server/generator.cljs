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

(defn increase-time! [previous millis]
  (js/Promise.
   (fn [resolve reject]
     (web3-evm/increase-time! @web3 [millis]
                              (fn [error success]
                                (if error
                                  (reject error)
                                  (resolve previous)))))))

(defn get-account [account]
  (if (string? account)
    account
    (nth (web3-eth/accounts @web3) account)))

(defn upload-meme! [previous {:keys [:image-file]
                              :or {image-file "resources/dev/pepe.png"}
                              :as create-meme}]
  (when create-meme
    (log/info "Uploading file" {:path image-file} ::upload-meme)
    (js/Promise.
     (fn [resolve reject]
       (.readFile fs
                  image-file
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
                             (log/info (str "Uploaded " image-file " received") {:image-hash image-hash} ::upload-meme)
                             (resolve (assoc previous :image-hash image-hash)))))))))))))

(defn upload-meme-meta! [{:keys [:image-hash] :as previous} {:keys [:title :search-tags]
                                                             :or {title "PepeSmile"
                                                                  search-tags ["pepe" "frog" "dank"]}
                                                             :as create-meme}]
  (when create-meme
    (let [meta-info (format/clj->json {:title title
                                       :image-hash image-hash
                                       :search-tags search-tags})]
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
                (resolve (assoc previous :meta-hash meta-hash)))))))))))

(defn get-meme-registry-db-values [previous]
  (let [meme-registry-db-keys [:max-total-supply :max-auction-duration :deposit :commit-period-duration :reveal-period-duration]]
    (promise-> (eternal-db/get-uint-values :meme-registry-db meme-registry-db-keys)
               #(let [meme-registry-db-values (zipmap meme-registry-db-keys (map bn/number %))]
                  (merge previous {:meme-registry-db-values meme-registry-db-values})))))

(defn create-meme! [{{:keys [:meme-registry-db-values :deposit]} :meme-registry-db-values
                     :as previous} {:keys [:supply :from-account]
                                    :or {from-account 0}
                                    :as create-meme}]
  (when create-meme
    (let [total-supply (or supply
                           (inc (rand-int (get-in previous [:meme-registry-db-values :max-total-supply]))))
          account (get-account from-account)]
      (promise-> (meme-factory/approve-and-create-meme {:meta-hash (:meta-hash previous)
                                                        :total-supply total-supply
                                                        :amount deposit}
                                                       {:from account})
                 #(wait-for-tx-receipt %)
                 #(let [{{:keys [registry-entry creator]} :args} (registry/meme-constructed-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                        (:transaction-hash %))]
                    (assoc previous :meme {:registry-entry registry-entry
                                           :creator creator}))))))

(defn upload-challenge-meta! [{:keys [:meme] :as previous} {:keys [:comment]
                                                            :or {comment "did not like it"}
                                                            :as challenge-meme}]
  (when (and meme challenge-meme)
    (let [challenge-meta (format/clj->json {:comment comment})]
      (log/info "Uploading meta" {:challenge-meta challenge-meta} ::upload-challenge-meta)
      (js/Promise.
       (fn [resolve reject]
         (ipfs-files/add
          (js/Buffer.from challenge-meta)
          (fn [err {challenge-meta-hash :Hash}]
            (if err
              (do
                (log/error "ifps error" {:error err} ::upload-challenge-meta)
                (reject err))
              (do
                (log/info "Uploaded meta received " {:challenge-meta-hash challenge-meta-hash} ::upload-challenge-meta)
                (resolve (assoc previous :challenge-meta-hash challenge-meta-hash)))))))))))

(defn challenge-meme! [{{:keys [:registry-entry]} :meme :as previous} {:keys [:from-account]
                                                                       :or {from-account 0}
                                                                       :as challenge-meme}]
  (when (and registry-entry challenge-meme)
    (promise-> (registry-entry/approve-and-create-challenge (get-in previous [:meme :registry-entry])
                                                            {:meta-hash (:challenge-meta-hash previous)
                                                             :amount (get-in previous [:meme-registry-db-values :deposit])}
                                                            {:from (get-account from-account)})
               #(wait-for-tx-receipt %)
               #(assoc previous :challenge-meme-tx (:transaction-hash %)))))

(defn commit-votes! [{{:keys [:registry-entry]} :meme
                      :as previous} commit-votes]
  (promise-> (js/Promise.all
              (for [{:keys [:option :amount :from-account] :as vote} commit-votes]

                (do
                  (prn "VOTE" {:registry-entry registry-entry
                        :vote-option option
                        :salt "abc"
                        :from (get-account from-account)
                        :amount amount
                        })

                  (promise-> (registry-entry/approve-and-commit-vote registry-entry
                                                                     {:amount amount
                                                                      :salt "abc"
                                                                      :vote-option option}
                                                                     {:from (get-account from-account)})
                             #(wait-for-tx-receipt %)))))
             #(assoc previous :commit-vote-txs (map :transaction-hash %))))

;; TODO : options
;; TODO : for all votes
;; TODO : reverts (wrong hash)
#_(defn reveal-votes! [{{:keys [:registry-entry]} :meme :as previous} account]
  (promise-> (registry-entry/reveal-vote registry-entry
                                         {:vote-option :vote.option/vote-for
                                          :salt "abc"}
                                         {:from (get-account 0)})
             #(wait-for-tx-receipt %)
             #(assoc previous :reveal-vote-tx (:transaction-hash %))))

(defn reveal-votes! [{:keys [:commit-vote-txs] {:keys [:registry-entry]} :meme :as previous} reveal-votes]

  ;; (prn commit-vote-txs)

  (promise-> (js/Promise.all
              (for [{:keys [:option :amount :from-account] :as vote} reveal-votes]

                (do
                  (prn "VOTE" {:registry-entry registry-entry
                        :vote-option option
                        :salt "abc"
                        :from (get-account from-account)})

                  (promise-> (registry-entry/reveal-vote registry-entry
                                                         {:vote-option option
                                                          :salt "abc"}
                                                         {:from (get-account from-account)})
                             #(wait-for-tx-receipt %)))))
             #(assoc previous :reveal-vote-txs (map :transaction-hash %))))

;; TODO : options
(defn claim-vote-reward! [{{:keys [:registry-entry]} :meme :as previous} account]
  (promise-> (registry-entry/claim-vote-reward registry-entry {:from account})
             #(wait-for-tx-receipt %)
             #(assoc previous :claim-vote-reward-tx (:transaction-hash %))))

;; TODO : options
(defn mint-meme-tokens! [{:keys [:total-supply] {:keys [:registry-entry]} :meme :as previous} account]
  (promise-> (meme/mint registry-entry (dec total-supply) {:from account})
             #(wait-for-tx-receipt %)
             #(let [{{:keys [:token-start-id :token-end-id]} :args} (registry/meme-minted-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                      (:transaction-hash %))]
                (assoc-in previous [:meme :token-ids] (range (bn/number token-start-id)
                                                             (inc (bn/number token-end-id)))))))
;; TODO : options
(defn start-auction! [{{:keys [:token-ids]} :meme {:keys [:max-auction-duration]} :meme-registry-db-values :as previous} account]
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

;; TODO : options
(defn buy-auction! [{{:keys [:meme-auction]} :meme-auction :as previous} account]
  (promise-> (meme-auction/buy meme-auction {:from account
                                             :value (web3/to-wei 0.1 :ether)})
             #(wait-for-tx-receipt %)
             #(assoc previous :buy-tx (:transaction-hash %))))

(defn generate-memes [{:keys [:create-meme :challenge-meme :commit-votes
                              :reveal-votes :mint-meme-tokens :start-auction
                              :buy-meme] :as scenarios}]
  (let [account (first (web3-eth/accounts @web3))
        previous {}]
    (promise-> (upload-meme! previous create-meme)
               #(upload-meme-meta! % create-meme)
               #(get-meme-registry-db-values %)
               #(create-meme! % create-meme)

               #(upload-challenge-meta! % challenge-meme)
               #(challenge-meme! % challenge-meme)

               #(commit-votes! % commit-votes)

               #(increase-time! % (inc (get-in % [:meme-registry-db-values :commit-period-duration])))
               #(reveal-votes! % reveal-votes)
               ;; #(increase-time! % (inc (get-in % [:meme-registry-db-values :reveal-period-duration])))

               ;; #(claim-vote-reward! % account)

               ;; #(mint-meme-tokens! % account)

               ;; #(start-auction! % account)

               ;; #(buy-auction! % account)

               #(log/info "Generate meme result" % ::generate-memes)

               )))
