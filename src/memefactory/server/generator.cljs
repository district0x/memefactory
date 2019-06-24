(ns memefactory.server.generator
  (:require
   [bignumber.core :as bn]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [cljs-web3.utils :refer [js->cljkk camel-case]]
   [clojure.set :as cset]
   [district.cljs-utils :refer [rand-str]]
   [district.format :as format]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts :refer [smart-contracts contract-address contract-call instance wait-for-tx-receipt]]
   [district.server.web3 :refer [web3]]
   [district.shared.async-helpers :refer [promise->]]
   [district.shared.error-handling :refer [try-catch]]
   [memefactory.server.contract.dank-token :as dank-token]
   [memefactory.server.contract.eternal-db :as eternal-db]
   [memefactory.server.contract.meme :as meme]
   [memefactory.server.contract.meme-auction :as meme-auction]
   [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
   [memefactory.server.contract.meme-factory :as meme-factory]
   [memefactory.server.contract.meme-token :as meme-token]
   [memefactory.server.contract.minime-token :as minime-token]
   [memefactory.server.contract.param-change :as param-change]
   [memefactory.server.contract.param-change-factory :as param-change-factory]
   [memefactory.server.contract.param-change-registry :as param-change-registry]
   [memefactory.server.contract.registry :as registry]
   [memefactory.server.contract.registry :as registry]
   [memefactory.server.contract.registry-entry :as registry-entry]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :refer [spy] :as log]
   ))

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

(defn has-all-keys? [m ks]
  (js/Promise.
   (fn [resolve reject]
     (if (apply = (map count [ks (select-keys m ks)]))
       (resolve true)
       (reject (str "Argument is missing mandatory key(s) " (cset/difference (set ks)
                                                                             (-> m keys set))))))))

(defn upload-meme! [previous
                    {:keys [:image-file]
                     ;; default value
                     :or {image-file "resources/dev/pepe.png"}
                     :as arguments}]
  (js/Promise.
   (fn [resolve reject]
     (if arguments
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
                             (resolve (assoc-in previous [:meme :image-hash] image-hash)))))))))
       (resolve previous)))))

(defn upload-meme-meta! [{{:keys [:image-hash]} :meme
                          :as previous}
                         {:keys [:title :search-tags]
                          ;; default values
                          :or {title "PepeSmile"
                               search-tags ["pepe" "frog" "dank"]}
                          :as arguments}]
  (let [meta-info (format/clj->json {:title title
                                     :image-hash image-hash
                                     :search-tags search-tags})]
    (js/Promise.
     (fn [resolve reject]
       (if arguments
         (ipfs-files/add
          (js/Buffer.from meta-info)
          (fn [err {meta-hash :Hash}]
            (if err
              (do
                (log/error "ifps error" {:error err} ::upload-meme-meta)
                (reject err))
              (do
                (log/info "Uploaded meta received " {:meta-hash meta-hash} ::upload-meme-meta)
                (resolve (assoc-in previous [:meme :meta-hash] meta-hash))))))
         (resolve previous))))))

(defn get-meme-registry-db-values [previous]
  (let [meme-registry-db-keys [:max-total-supply :max-auction-duration :deposit :commit-period-duration :reveal-period-duration]]
    (promise-> (eternal-db/get-uint-values :meme-registry-db meme-registry-db-keys)
               #(let [meme-registry-db-values (zipmap meme-registry-db-keys (map bn/number %))]
                  (merge previous {:meme-registry-db-values meme-registry-db-values})))))

(defn create-meme! [{{:keys [:meta-hash]} :meme
                     {:keys [:deposit :max-total-supply]} :meme-registry-db-values
                     :as previous}
                    {:keys [:total-supply :from-account]
                     ;; default : first account creates meme with random supply
                     :or {total-supply (inc (rand-int max-total-supply))
                          from-account 0}
                     :as arguments}]
  (if arguments
    (let [account (get-account from-account)]
      (promise-> (meme-factory/approve-and-create-meme {:meta-hash meta-hash
                                                        :total-supply total-supply
                                                        :amount deposit}
                                                       {:from account})
                 #(wait-for-tx-receipt %)
                 #(let [{{:keys [registry-entry creator]} :args} (registry/meme-constructed-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                        (:transaction-hash %))]
                    (assoc-in previous [:meme] (merge (:meme previous)
                                                      {:registry-entry registry-entry
                                                       :total-supply total-supply
                                                       :creator creator})))))
    (js/Promise.resolve previous)))

(defn upload-challenge-meta! [previous
                              {:keys [:comment]
                               ;; defualt value
                               :or {comment "did not like it"}
                               :as arguments}]
  (let [challenge-meta (format/clj->json {:comment comment})]
    (js/Promise.
     (fn [resolve reject]
       (if arguments
         (ipfs-files/add
          (js/Buffer.from challenge-meta)
          (fn [err {challenge-meta-hash :Hash}]
            (if err
              (do
                (log/error "ifps error" {:error err} ::upload-challenge-meta)
                (reject err))
              (do
                (log/info "Uploaded meta received " {:challenge-meta-hash challenge-meta-hash} ::upload-challenge-meta)
                (resolve (assoc-in previous [:challenge :challenge-meta-hash] challenge-meta-hash))))))
         (resolve previous))))))

(defn challenge-meme! [{{:keys [:challenge-meta-hash]} :challenge
                        {:keys [:registry-entry]} :meme
                        {:keys [:deposit]} :meme-registry-db-values
                        :as previous}
                       {:keys [:amount :from-account]
                        ;; default values : challenge from account 0
                        :or {amount deposit
                             from-account 0}
                        :as arguments}]
  (if arguments
    (let [challenger-address (get-account from-account)]
      (promise-> (registry-entry/approve-and-create-challenge (if (:registry-entry arguments)
                                                                (:registry-entry arguments)
                                                                registry-entry)
                                                              {:meta-hash challenge-meta-hash
                                                               :amount deposit}
                                                              {:from challenger-address})
                 #(wait-for-tx-receipt %)
                 #(-> previous
                      (assoc-in [:challenge :challenge-meme-tx] (:transaction-hash %))
                      (assoc-in [:challenge :challenger-address] challenger-address))))
    (js/Promise.resolve previous)))

(defn commit-votes! [{{:keys [:registry-entry]} :meme
                      :as previous}
                     args]
  (if-let [args (cond
                  ;; user supplied votes
                  (sequential? args) args
                  ;; default values for votes
                  args [{:option :vote.option/vote-for
                         :salt (rand-str 7)
                         :amount 1
                         :from-account 0}]
                  ;; skip this step
                  :else nil)]
    (promise-> (js/Promise.all
                (for [{:keys [:option :amount :salt :from-account]
                       :as vote} args]
                  (promise-> (has-all-keys? vote [:option :salt :amount :from-account])
                             #(registry-entry/approve-and-commit-vote registry-entry
                                                                      {:amount amount
                                                                       :salt salt
                                                                       :vote-option option}
                                                                      {:from (get-account from-account)})
                             #(wait-for-tx-receipt %)
                             #(js/Promise.resolve {:from-account from-account
                                                   :amount amount
                                                   :salt salt
                                                   :option option
                                                   :commit-vote-tx (:transaction-hash %)}))))
               #(assoc previous :votes (js->clj %)))
    (js/Promise.resolve previous)))

(defn reveal-votes! [{:keys [:votes]
                      {:keys [:registry-entry]} :meme
                      :as previous}
                     args]
  (if-let [args (cond
                  ;; user supplied votes
                  (sequential? args) args
                  ;; default: reveal all commited votes
                  args votes
                  ;; skip this step
                  :else nil)]
    (promise-> (js/Promise.all
                (for [{:keys [:option :salt :from-account]
                       :as vote} args]
                  (promise-> (has-all-keys? vote [:option :salt :from-account])
                             #(registry-entry/reveal-vote registry-entry
                                                          {:vote-option option
                                                           :salt salt}
                                                          {:from (get-account from-account)})
                             #(wait-for-tx-receipt %)
                             #(js/Promise.resolve (merge vote
                                                         {:reveal-vote-tx (:transaction-hash %)})))))
               ;; TODO : dont overwrite, assoc-in by index in :votes
               #(assoc previous :votes (js->clj %)))
    previous))

(defn set-meme-status! [{{:keys [:registry-entry]} :meme
                         :as previous}]
  (promise-> (registry-entry/status registry-entry)
             #(assoc-in previous [:meme :status] %)))

(defn claim-rewards! [{{:keys [:registry-entry]} :meme
                       :as previous}
                      args]
  (if args
    (promise-> (js/Promise.all
                (map (fn [{:keys [:from-account]}]
                       (promise-> (registry-entry/claim-rewards registry-entry {:from (get-account from-account)})
                                  #(wait-for-tx-receipt %)))
                     args))
               #(assoc-in previous [:rewards :claim-reward-txs] (map :transaction-hash %)))
    (js/Promise.resolve previous)))

(defn mint-meme-tokens! [{{:keys [:registry-entry :total-supply :creator]} :meme
                          :as previous}
                         {:keys [:amount :from-account]
                          ;; defaults: creator mints all tokens
                          :or {amount total-supply
                               from-account creator}
                          :as arguments}]
  (if arguments
    (promise-> (meme/mint registry-entry total-supply {:from (get-account from-account)})
               #(wait-for-tx-receipt %)
               #(let [{{:keys [:token-start-id :token-end-id]} :args} (registry/meme-minted-event-in-tx [:meme-registry :meme-registry-fwd]
                                                                                                        (:transaction-hash %))]
                  (assoc-in previous [:meme :minted-token-ids] (range (bn/number token-start-id)
                                                                      (inc (bn/number token-end-id))))))
    (js/Promise.resolve previous)))

(defn start-auctions!
  "Create auctions for `token-ids` (by default all)."
  [{{:keys [:minted-token-ids :creator]} :meme
    {:keys [:max-auction-duration]} :meme-registry-db-values
    :as previous}
   {:keys [:token-ids :start-price :end-price :duration :description :from-account]
    ;; default values : auction all tokens
    :or {token-ids minted-token-ids
         start-price 0.5
         end-price 0.1
         duration (+ 60 (rand-int (- max-auction-duration 60)))
         description "some auction"
         from-account 0}
    :as arguments}]
  (if arguments
    (let [account (get-account from-account)]
      (promise-> (meme-token/transfer-multi-and-start-auction {:from account
                                                               :token-ids token-ids
                                                               :start-price (web3/to-wei start-price :ether)
                                                               :end-price (web3/to-wei end-price :ether)
                                                               :duration duration
                                                               :description description}
                                                              {:from account})
                 #(wait-for-tx-receipt %)
                 #(map (fn [{{:keys [:meme-auction :token-id]} :args :as evt}]
                         {:meme-auction meme-auction
                          :token-id (bn/number token-id)})
                       ;; TODO : bug in smart-contracts/contract-events-in-tx, returns only last event
                       (meme-auction-factory/meme-auction-started-events-in-tx (:transaction-hash %)))
                 #(assoc previous :auctions (vec %))))
    (js/Promise.resolve previous)))

(defn buy-auctions! [{:keys [:auctions]
                      :as previous}
                     args]
  (if-let [args (cond
                  ;; user supplied args
                  (sequential? args) args
                  ;; default values : buy all meme-auctions
                  args (map
                        #(merge %
                                {:price 0.5
                                 :from-account 0})
                        auctions)
                  ;; skip this step
                  :else nil)]
    (promise-> (js/Promise.all
                (for [index (range (count auctions))
                      {:keys [:meme-auction :price :from-account]
                       ;; default values for single buy tx
                       :or {meme-auction (:meme-auction (nth auctions index))}
                       :as auction} args]
                  (promise-> (has-all-keys? auction [#_:meme-auction :price :from-account])
                             #(meme-auction/buy meme-auction {:from (get-account from-account)
                                                              :value (web3/to-wei price :ether)})
                             #(wait-for-tx-receipt %)
                             #(js/Promise.resolve (merge (nth auctions index)
                                                         {:index index
                                                          :buy-auction-tx (:transaction-hash %)})))))
               ;; TODO : dont overwrite, assoc-in by index in :auctions
               #(assoc previous :auctions (js->clj %)))
    (js/Promise.resolve previous)))

(defn generate-memes [{:keys [:create-meme :challenge-meme :commit-votes
                              :reveal-votes
                              :claim-rewards
                              :mint-meme-tokens :start-auctions :buy-auctions]
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

               #(if reveal-votes
                  (increase-time! % (inc (get-in % [:meme-registry-db-values :commit-period-duration])))
                  (js/Promise.resolve %))

               #(reveal-votes! % reveal-votes)

               #(if claim-rewards
                  (increase-time! % (inc (get-in % [:meme-registry-db-values :reveal-period-duration])))
                  (js/Promise.resolve %))

               #(set-meme-status! %)

               #(claim-rewards! % claim-rewards)

               #(mint-meme-tokens! % mint-meme-tokens)

               #(start-auctions! % start-auctions)

               #(buy-auctions! % buy-auctions)

               #(log/info "Generate meme result" % ::generate-memes)

               )))
