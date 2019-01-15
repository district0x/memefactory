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

(defn upload-meme! [previous
                    {:keys [:image-file]
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
                             (resolve (assoc-in previous [:meme :image-hash] image-hash)))))))))))))

(defn upload-meme-meta! [{{:keys [:image-hash]} :meme :as previous}
                         {:keys [:title :search-tags]
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
                (resolve (assoc-in previous [:meme :meta-hash] meta-hash)))))))))))

(defn get-meme-registry-db-values [previous]
  (let [meme-registry-db-keys [:max-total-supply :max-auction-duration :deposit :commit-period-duration :reveal-period-duration]]
    (promise-> (eternal-db/get-uint-values :meme-registry-db meme-registry-db-keys)
               #(let [meme-registry-db-values (zipmap meme-registry-db-keys (map bn/number %))]
                  (merge previous {:meme-registry-db-values meme-registry-db-values})))))

(defn create-meme! [{{:keys [:meta-hash]} :meme
                     {:keys [:deposit :max-total-supply]} :meme-registry-db-values
                     :as previous}
                    {:keys [:total-supply :from-account]
                     :or {total-supply (inc (rand-int max-total-supply))
                          from-account 0}
                     :as create-meme}]
  (when create-meme
    (let [account (get-account from-account)]
      (promise-> (meme-factory/approve-and-create-meme {:meta-hash meta-hash
                                                        :total-supply total-supply
                                                        :amount deposit}
                                                       {:from account})
                 #(wait-for-tx-receipt %)
                 #(let [{{:keys [registry-entry creator]} :args} (registry/meme-constructed-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                        (:transaction-hash %))]
                    (-> previous
                        (assoc-in [:meme :registry-entry] registry-entry)
                        (assoc-in [:meme :total-supply] total-supply)
                        (assoc-in [:meme :creator] creator)))))))

(defn upload-challenge-meta! [{:keys [:meme] :as previous}
                              {:keys [:comment]
                               :or {comment "did not like it"}
                               :as challenge-meme}]
  (if challenge-meme
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
                (resolve (assoc-in previous [:challenge :challenge-meta-hash] challenge-meta-hash)))))))))
    previous))

(defn challenge-meme! [{{:keys [:challenge-meta-hash]} :challenge
                        {:keys [:registry-entry]} :meme
                        {:keys [:deposit]} :meme-registry-db-values
                        :as previous}
                       {:keys [:amount :from-account]
                        :or {amount deposit
                             from-account 0}
                        :as challenge-meme}]
  (if challenge-meme
    (promise-> (registry-entry/approve-and-create-challenge registry-entry
                                                            {:meta-hash challenge-meta-hash
                                                             :amount deposit}
                                                            {:from (get-account from-account)})
               #(wait-for-tx-receipt %)
               #(assoc-in previous [:challenge :challenge-meme-tx] (:transaction-hash %)))
    previous))

;; TODO : handle votes is true
(defn commit-votes! [{{:keys [:registry-entry]} :meme
                      :as previous}
                     votes]
  (if votes
    (promise-> (js/Promise.all
                (for [{:keys [:option :amount :from-account] :as vote} votes]
                  (promise-> (registry-entry/approve-and-commit-vote registry-entry
                                                                     {:amount amount
                                                                      :salt "abc"
                                                                      :vote-option option}
                                                                     {:from (get-account from-account)})
                             #(wait-for-tx-receipt %))))
               #(assoc previous :commit-vote-txs (map :transaction-hash %)))
    previous))

(defn reveal-votes! [{:keys [:commit-vote-txs] {:keys [:registry-entry]} :meme :as previous}
                     votes]
  (if votes
    (promise-> (js/Promise.all
                (for [{:keys [:option :amount :from-account] :as vote} votes]

                  (promise-> (registry-entry/reveal-vote registry-entry
                                                         {:vote-option option
                                                          :salt "abc"}
                                                         {:from (get-account from-account)})
                             #(wait-for-tx-receipt %))))
               #(assoc previous :reveal-vote-txs (map :transaction-hash %)))
    previous))

(defn claim-vote-rewards! [{{:keys [:registry-entry]} :meme :as previous}
                           {:keys [:from-account] :as claims}]
  (if claims
    (promise-> (js/Promise.all
                (for [{:keys [:from-account] :as claim} claims]
                  (promise-> (registry-entry/claim-vote-reward registry-entry {:from (get-account from-account)})
                             #(wait-for-tx-receipt %))))
               #(assoc previous :claim-vote-reward-txs (map :transaction-hash %)))
    previous))

(defn mint-meme-tokens! [{{:keys [:registry-entry :total-supply]} :meme :as previous}
                         {:keys [:amount :from-account]
                          :or {amount total-supply
                               from-account 0}
                          :as mint-meme-tokens}]
  (if mint-meme-tokens
    (promise-> (meme/mint registry-entry total-supply {:from (get-account from-account)})
               #(wait-for-tx-receipt %)
               #(let [{{:keys [:token-start-id :token-end-id]} :args} (registry/meme-minted-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                        (:transaction-hash %))]
                  (assoc-in previous [:meme :token-ids] (range (bn/number token-start-id)
                                                               (inc (bn/number token-end-id))))))
    previous))

(defn start-auctions! [{:keys [:meme] {:keys [:max-auction-duration]} :meme-registry-db-values :as previous}
                       auctions]
  (if auctions
    (promise-> (js/Promise.all
                (for [{:keys [:token-ids
                              :start-price
                              :end-price
                              :duration
                              :description
                              :from-account]
                       :or {token-ids (look (:token-ids meme))
                            start-price 0.5
                            end-price 0.1
                            duration (+ 60 (rand-int (- max-auction-duration 60)))
                            description "some auction"
                            from-account 0}
                       :as auction} auctions]
                  (let [account (get-account from-account)]
                    (promise-> (meme-token/transfer-multi-and-start-auction {:from account
                                                                             :token-ids token-ids
                                                                             :start-price (web3/to-wei start-price :ether)
                                                                             :end-price (web3/to-wei end-price :ether)
                                                                             :duration duration
                                                                             :description "some auction"}
                                                                            {:from account})
                               #(wait-for-tx-receipt %)
                               #(apply (fn [{{:keys [:meme-auction :token-id]} :args :as evt}]
                                         {:meme-auction meme-auction
                                          :token-id (bn/number token-id)})
                                       ;; TODO : bug in smart-contracts/contract-events-in-tx, returns only last event
                                       (meme-auction-factory/meme-auction-started-events-in-tx (:transaction-hash %)))))))
               #(assoc previous :meme-auctions (js->clj %)))
    previous))

(defn buy-auctions! [{:keys [:meme-auctions] :as previous}
                     auctions]
  (if auctions
    (promise-> (js/Promise.all
                (for [index (range (count auctions))
                      {:keys [:meme-auction :price :from-account]
                       :or {meme-auction (:meme-auction (nth meme-auctions index))
                            price 0.5
                            from-account 0}
                       :as auction} auctions]
                  (promise-> (meme-auction/buy meme-auction {:from (get-account from-account)
                                                             :value (web3/to-wei price :ether)})
                             #(wait-for-tx-receipt %))))
               #(assoc previous :buy-auction-txs (map :transaction-hash %)))
    previous))

(defn generate-memes [{:keys [:create-meme :challenge-meme :commit-votes
                              :reveal-votes :claim-vote-rewards :mint-meme-tokens
                              :start-auctions :buy-auctions]
                       :as scenarios}]
  (let [account (first (web3-eth/accounts @web3))
        previous {}]
    (promise-> (get-meme-registry-db-values previous)
               #(upload-meme! % create-meme)
               #(upload-meme-meta! % create-meme)
               #(create-meme! % create-meme)

               #(upload-challenge-meta! % challenge-meme)
               #(challenge-meme! % challenge-meme)

               #(commit-votes! % commit-votes)

               #(increase-time! % (inc (get-in % [:meme-registry-db-values :commit-period-duration])))
               #(reveal-votes! % reveal-votes)
               #(increase-time! % (inc (get-in % [:meme-registry-db-values :reveal-period-duration])))

               #(claim-vote-rewards! % claim-vote-rewards)

               #(mint-meme-tokens! % mint-meme-tokens)

               #(start-auctions! % start-auctions)

               #(buy-auctions! % buy-auctions)

               #(log/info "Generate meme result" % ::generate-memes)

               )))
