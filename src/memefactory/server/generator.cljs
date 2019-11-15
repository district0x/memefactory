(ns memefactory.server.generator
  (:require [bignumber.core :as bn]
            [cljs-ipfs-api.files :as ipfs-files]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs-web3.utils :as web3-utils]
            [clojure.set :refer [difference]]
            [district.cljs-utils :refer [rand-str]]
            [district.format :as format]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [district.shared.async-helpers :refer [safe-go <?]]
            [cljs.core.async :as async]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-auction :as meme-auction]
            [memefactory.server.contract.meme-factory :as meme-factory]
            [memefactory.server.contract.meme-token :as meme-token]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [taoensso.timbre :refer [spy] :as log]))

(def fs (js/require "fs"))

(defn get-meme-registry-db-values []
  (safe-go
   (let [meme-registry-db-keys [:max-total-supply :max-auction-duration :deposit :commit-period-duration :reveal-period-duration]
         meme-registry-db-values (<? (eternal-db/get-uint-values :meme-registry-db meme-registry-db-keys))]
     (zipmap meme-registry-db-keys meme-registry-db-values))))

(defn upload-meme-image! [{:keys [:image-file]
                           :or {image-file "resources/dev/pepe.png"}
                           :as arguments}]
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
                         (resolve {:image-hash image-hash}))))))))))

(defn upload-meme-meta! [{:keys [:title :search-tags :image-hash]
                          :or {title "PepeSmile"
                               search-tags ["pepe" "frog" "dank"]}
                          :as arguments}]
  (let [meta-info (format/clj->json {:title title
                                     :image-hash image-hash
                                     :search-tags search-tags})]
    (js/Promise.
     (fn [resolve reject]
       (ipfs-files/add
        (js/Buffer.from meta-info)
        (fn [err {meta-hash :Hash}]
          (if err
            (do
              (log/error "ifps error" {:error err} ::upload-meme-meta)
              (reject err))
            (resolve {:meta-hash meta-hash}))))))))

(defn create-meme! [{:keys [:total-supply :accounts :from-account :meta-hash :deposit :max-total-supply]
                     :or {from-account 0}
                     :as arguments}]
  (safe-go
   (let [{:keys [:transaction-hash] :as tx-receipt} (<? (meme-factory/approve-and-create-meme {:meta-hash meta-hash
                                                                                               :total-supply (or total-supply
                                                                                                                 (inc (rand-int max-total-supply)))
                                                                                               :amount deposit}
                                                                                              {:from (if (string? from-account)
                                                                                                       from-account
                                                                                                       (nth accounts from-account))}))]
     (<? (smart-contracts/contract-event-in-tx [:meme-registry :meme-registry-fwd] :MemeConstructedEvent tx-receipt)))))

(defn upload-challenge-meta! [{:keys [:comment]
                               :or {comment "did not like it"}
                               :as arguments}]
  (let [challenge-meta (format/clj->json {:comment comment})]
    (js/Promise.
     (fn [resolve reject]
       (ipfs-files/add
        (js/Buffer.from challenge-meta)
        (fn [err {challenge-meta-hash :Hash}]
          (if err
            (do
              (log/error "ifps error" {:error err} ::upload-challenge-meta)
              (reject err))
            (resolve {:challenge-meta-hash challenge-meta-hash}))))))))

(defn challenge-meme! [{:keys [:challenge-meta-hash
                               :deposit
                               :amount
                               :from-account
                               :accounts]
                        :or {from-account 0}
                        :as arguments}]
  (registry-entry/approve-and-create-challenge (:registry-entry arguments)
                                               {:meta-hash challenge-meta-hash
                                                :amount deposit}
                                               {:from (nth accounts from-account)}))

(defn commit-votes! [{:keys [:accounts :registry-entry :commit-votes]
                      :or {commit-votes [{:option :vote.option/vote-for
                                          :salt (rand-str 7)
                                          :amount 1
                                          :from-account 0}]}
                      :as arguments}]
  (js/Promise.all
   (for [{:keys [:option :amount :salt :from-account]
          :as vote} commit-votes]
     (registry-entry/approve-and-commit-vote registry-entry
                                             {:amount amount
                                              :salt salt
                                              :vote-option option}
                                             {:from (nth accounts from-account)}))))

(defn reveal-votes! [{:keys [:accounts :registry-entry :reveal-votes :commit-votes]
                      :as args}]
  (let [votes (if (sequential? reveal-votes)
                reveal-votes
                commit-votes)]
    (js/Promise.all
     (for [{:keys [:option :salt :from-account]
            :as vote} votes]
       (registry-entry/reveal-vote registry-entry
                                   {:vote-option option
                                    :salt salt}
                                   {:from (nth accounts from-account)})))))

(defn get-meme-status [{:keys [:registry-entry]}]
  (registry-entry/status registry-entry))

(defn claim-rewards! [{:keys [:accounts :registry-entry :claim-vote-rewards] :as args}]
  (js/Promise.all
   (for [{:keys [:from-account]} claim-vote-rewards]
     (registry-entry/claim-rewards registry-entry {:from (nth accounts from-account)}))))

(defn mint-meme-tokens! [{:keys [:accounts :amount :from-account :registry-entry :total-supply :creator]}]
  (safe-go
   (let [tx-receipt (<? (meme/mint registry-entry (or amount total-supply) {:from (if from-account
                                                                                    (nth accounts from-account)
                                                                                    creator)}))
         {:keys [:token-start-id :token-end-id]} (<? (smart-contracts/contract-event-in-tx [:meme-registry :meme-registry-fwd] :MemeMintedEvent tx-receipt))]
     (range (bn/number token-start-id) (inc (bn/number token-end-id))))))

(defn start-auctions!
  [{:keys [:accounts :minted-meme-tokens :creator :max-auction-duration :token-ids :start-price :end-price :duration :description :from-account]
    :or {start-price 0.5
         end-price 0.1
         description "some auction"
         from-account 0}}]
  (safe-go
   (let [account (nth accounts from-account)
         tx-receipt (<? (meme-token/transfer-multi-and-start-auction {:from account
                                                                      :token-ids (or token-ids minted-meme-tokens)
                                                                      :start-price (web3-utils/to-wei @web3 start-price :ether)
                                                                      :end-price (web3-utils/to-wei @web3 end-price :ether)
                                                                      :duration (+ 60 (rand-int (- (bn/number max-auction-duration) 60)))
                                                                      :description description}
                                                                     {:from account}))]
     (<? (smart-contracts/contract-event-in-tx [:meme-auction-factory :meme-auction-factory-fwd] :MemeAuctionStartedEvent tx-receipt)))))

(defn buy-auctions! [{:keys [:accounts :meme-auctions :buy-auctions]}]
  (js/Promise.all
   (for [index (range (count meme-auctions))
         {:keys [:meme-auction :price :from-account]
          :or {meme-auction (:meme-auction (nth meme-auctions index))}} buy-auctions]
     (meme-auction/buy meme-auction {:from (nth accounts from-account)
                                     :value (web3-utils/to-wei @web3 price :ether)}))))

(defn pause [millis]
  (js/Promise. (fn [resolve reject]
                 (js/setTimeout #(resolve millis)
                                millis))))

(defn generate-memes [{:keys [:create-meme :challenge-meme :commit-votes
                              :reveal-votes :claim-vote-rewards :mint-meme-tokens
                              :start-auctions :buy-auctions]
                       :as scenarios}]
  (safe-go
   (let [accounts {:accounts (<? (web3-eth/accounts @web3))}
         {:keys [:commit-period-duration :reveal-period-duration] :as meme-db-values} (<? (get-meme-registry-db-values))
         meme-image-hash (<? (upload-meme-image! create-meme))
         meme-meta-hash (<? (upload-meme-meta! (merge meme-image-hash create-meme)))
         meme (<? (create-meme! (merge accounts meme-db-values meme-meta-hash create-meme)))
         challenge-meta (<? (upload-challenge-meta! (merge accounts meme-db-values meme challenge-meme)))
         challenge-meme-tx (<? (challenge-meme! (merge accounts challenge-meta meme challenge-meme)))
         commit-votes-txs (<? (commit-votes! (merge accounts meme {:commit-votes commit-votes})))
         to-reveal-time-increase (<? (web3-evm/increase-time @web3 (inc (bn/number commit-period-duration))))
         reveal-votes-txs (<? (reveal-votes! (merge accounts meme (:reveal-votes reveal-votes) {:commit-votes commit-votes})))
         to-claim-time-increase (<? (web3-evm/increase-time @web3 (bn/number (inc (bn/number reveal-period-duration)))))
         block (<? (web3-evm/mine-block @web3))
         status (<? (get-meme-status meme))
         claim-rewards-txs (<? (claim-rewards! (merge accounts meme {:claim-vote-rewards claim-vote-rewards})))
         meme-tokens (<? (mint-meme-tokens! (merge accounts meme mint-meme-tokens)))
         meme-auctions [(<? (start-auctions! (merge accounts meme-db-values meme {:minted-meme-tokens meme-tokens} start-auctions)))]
         ;; TODO : weird bug, tx is sent but never executed
         ;; buy-auction-txs (<? (buy-auctions! (merge accounts {:meme-auctions meme-auctions :buy-auctions buy-auctions})))
         ]
     (log/debug "generate-memes results" {:meme-db-values meme-db-values
                                          :meme-image-hash meme-image-hash
                                          :meme-meta-hash meme-meta-hash
                                          :meme meme
                                          :challenge-meta challenge-meta
                                          :challenge-meme-tx (:transaction-hash challenge-meme-tx)
                                          :commit-votes-txs (map :transaction-hash commit-votes-txs)
                                          :to-reveal-time-increase to-reveal-time-increase
                                          :reveal-votes-txs (map :transaction-hash reveal-votes-txs)
                                          :to-claim-time-increase to-claim-time-increase
                                          :status status
                                          :claim-rewards-txs (map :transaction-hash claim-rewards-txs)
                                          :meme-tokens meme-tokens
                                          :meme-auctions meme-auctions
                                          ;; :buy-auction-txs (map :transaction-hash buy-auction-txs)
                                          }))))
