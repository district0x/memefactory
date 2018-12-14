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
    (log/info "Uploading" file ::upload-meme)
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

(defn upload-meme-meta [previous]
  (let [{:keys [image-hash]} previous
        meta-info (format/clj->json {:title "PepeSmile"
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

;; TODO


;; {:claim-vote-reward-tx
;;  "0x7cb644c9820b6e0e7c80e79fead83591bb16bc6813e2d805f3565423f2387e8f",
;;  :meta-hash "QmWip6bd1hZXqXMiwzgNkS8dvYMh7ZD9VcjcLSooyEqx1F",
;;  :commit-vote-tx
;;  "0x909799b1e171d18bbc122ea3643061a9c8f23066f1db78ab7f3caffafe6130b4",
;;  :meme-auction
;;  {:address "0xb6813bafeab350e3e07e98a7795729d1da2d980a",
;;   :token-id #object[BigNumber 187]},
;;  :image-hash "QmVXuQzg8yxN31fzwrWZ67dQBoThgM2sVWUybRyg1UZHQ1",
;;  :challenge-meme-tx
;;  "0x8e5f805d64c11a3c60c42abd4aac9a071a7a6c898fc8478390194bb5af9eb8d0",
;;  :reveal-vote-tx
;;  "0x04feb717bb960c738213b28204beb02a5184d3c0d072fd87cd10e5c45be86709",
;;  :total-supply 7,
;;  :meme
;;  {:registry-entry "0x2f9108eb1a6d327f7c5393ca761e6c5a7c67a4e1",
;;   :creator "0x4c3f13898913f15f12f902d6480178484063a6fb",
;;   :token-ids (187 188 189 190 191 192)},
;;  :meme-registry-db-values
;;  {:max-total-supply 10,
;;   :max-auction-duration 12096000,
;;   :deposit 1000000000000000000,
;;   :commit-period-duration 600,
;;   :reveal-period-duration 600}}

(defn buy-auction [{{:keys [:meme-auction]} :meme-auction :as previous} account]
  (promise-> (meme-auction/buy meme-auction {:from account
                                             :value (web3/to-wei 0.1 :ether)})
             #(wait-for-tx-receipt %)
             #(assoc previous :buy-tx (:transaction-hash %))

             ))

(defn generate-memes [{:keys [:accounts :memes/use-accounts :memes/items-per-account :memes/scenarios]}]
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

               #(log/info "End-chain result" % ::generate-memes)
               ))

  #_(let [[max-total-supply max-auction-duration deposit commit-period-duration reveal-period-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :max-auction-duration :deposit :commit-period-duration
                                                            :reveal-period-duration])
             (map bn/number))
        scenarios (get-scenarios {:accounts accounts
                                  :use-accounts use-accounts
                                  :items-per-account items-per-account
                                  :scenarios scenarios})]
    (log/info "Going to generate" {:scenarios scenarios :deposit deposit} ::generate-memes)
    (doseq [[account {:keys [:scenario-type]}] scenarios]
      (-> (upload-meme)
          (.then upload-meme-meta)
          (.then (fn [meta-hash]
                   (try-catch
                    (let [total-supply (inc (rand-int max-total-supply))
                          auction-duration (+ 60 (rand-int (- max-auction-duration 60)))]

                      (let [tx-hash (meme-factory/approve-and-create-meme (look {:meta-hash meta-hash
                                                                            :total-supply total-supply
                                                                            :amount deposit})
                                                                          {:from account})]
                        (log/info "Meme Created in TX " tx-hash)
                        (when-not (= :scenario/create scenario-type)
                          (log/info "Generating some challenges")
                          (let [r (registry/meme-constructed-event-in-tx [:meme-registry :meme-registry-fwd] tx-hash)
                                {{:keys [:registry-entry :creator]} :args} r]
                            (when-not registry-entry
                              (throw (js/Error. "Meme registry entry wasn't found")))

                            (registry-entry/approve-and-create-challenge (look registry-entry)
                                                                         (look {:meta-hash meta-hash
                                                                           :amount deposit})
                                                                         {:from account})

                            (when-not (= :scenario/challenge scenario-type)
                              (log/info "Commiting some votes")

                              (registry-entry/approve-and-commit-vote registry-entry
                                                                      {:amount 2
                                                                       :salt "abc"
                                                                       :vote-option :vote.option/vote-for}
                                                                      {:from creator})

                              (when-not (= :scenario/commit-vote scenario-type)
                                (log/info "Revealing some votes")
                                (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

                                (registry-entry/reveal-vote registry-entry
                                                            {:vote-option :vote.option/vote-for
                                                             :salt "abc"}
                                                            {:from creator})

                                (when-not (= :scenario/reveal-vote scenario-type)
                                  (log/info "Claiming some votes rewards")
                                  (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

                                  (registry-entry/claim-vote-reward registry-entry {:from creator})

                                  (when-not (= :scenario/claim-vote-reward scenario-type)
                                    (log/info "Minting memes")
                                    (let [tx-hash (meme/mint registry-entry (dec total-supply) {:from creator})
                                          mm-ev (registry/meme-minted-event-in-tx [:meme-registry :meme-registry-fwd] tx-hash)

                                          {:keys [token-start-id token-end-id]} (-> mm-ev
                                                                                    :args)
                                          token-ids (range (bn/number token-start-id) (inc (bn/number token-end-id)))]

                                      (when (empty? token-ids)
                                        (throw (js/Error. "Token IDs weren't found")))

                                      (let [tx-hash (meme-token/transfer-multi-and-start-auction {:from creator
                                                                                                  :token-ids token-ids
                                                                                                  :start-price (web3/to-wei 0.1 :ether)
                                                                                                  :end-price (web3/to-wei 0.01 :ether)
                                                                                                  :duration auction-duration
                                                                                                  :description "some auction"})
                                            {{:keys [:meme-auction]} :args} (meme-auction-factory/meme-auction-started-event-in-tx tx-hash)]

                                        (when-not meme-auction
                                          (throw (js/Error. "Meme Auction wasn't found")))

                                        #_(meme-auction/buy meme-auction {:from creator :value (web3/to-wei 0.1 :ether)}))))))))))))))))))


(defn generate-param-changes [{:keys [:accounts
                                      :param-changes/use-accounts
                                      :param-changes/items-per-account
                                      :param-changes/scenarios]}]
#_  (let [[deposit challenge-period-duration] (->> (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration])
                                                 (map bn/number))]
    (doseq [[account {:keys [:scenario-type :param-change-db]}] (get-scenarios {:accounts accounts
                                                                                :use-accounts use-accounts
                                                                                :items-per-account items-per-account
                                                                                :scenarios scenarios})]
      (let [tx-hash (param-change-factory/approve-and-create-param-change
                     {:db (contract-address (or param-change-db :meme-registry-db))
                      :key :deposit
                      :value (web3/to-wei 800 :ether)
                      :amount deposit}
                     {:from account})
            {:keys [:registry-entry]} (:args (param-change-registry/registry-entry-event-in-tx tx-hash))]

        (when-not (= scenario-type :scenario/create)

          (when-not registry-entry
            (throw (js/Error. "Param change registry entry wasn't found")))

          (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
          (param-change-registry/apply-param-change registry-entry {:from account}))))))
