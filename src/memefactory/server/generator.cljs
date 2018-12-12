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
            [district.server.smart-contracts :refer [smart-contracts contract-address contract-call instance]]
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
                             (resolve (swap! previous assoc :image-hash image-hash)))))))))))))

(defn upload-meme-meta [previous]
  (let [{:keys [image-hash]} @previous
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
              (resolve (swap! previous assoc :meta-hash meta-hash))))))))))

#_(defn promisify [v]
  (js/Promise.resolve v))

;; TODO .catch
(defn generate-memes [{:keys [:accounts :memes/use-accounts :memes/items-per-account :memes/scenarios]}]
  (let [#_scenarios #_(get-scenarios {:accounts accounts
                                      :use-accounts use-accounts
                                      :items-per-account items-per-account
                                      :scenarios scenarios})
        account (first accounts)
        previous (atom {})
        meme-registry-db-keys [:max-total-supply :max-auction-duration :deposit :commit-period-duration :reveal-period-duration]]
    (.catch
     (promise-> (upload-meme previous)
                ;; #(.foo %)
                #(upload-meme-meta previous)
                (promise-> (eternal-db/get-uint-values :meme-registry-db meme-registry-db-keys)
                           #(let [meme-registry-db-values (zipmap meme-registry-db-keys
                                                                  (map bn/number %))]
                              (swap! previous merge {:meme-registry-db-values meme-registry-db-values
                                                     :total-supply (inc (rand-int (:max-total-supply meme-registry-db-values )))
                                                     ;; :amount (:deposit meme-registry-db-values)
                                                     })))

                #_#(meme-factory/approve-and-create-meme {:meta-hash (:meta-hash %)
                                                        :total-supply (:total-supply %)
                                                        :amount (get-in % [:meme-registry-db-values :deposit])}
                                                       {:from account})

                #_(.then #(prn "@resp" %))


      #(prn "@end-chain result" %)
      )
     (fn [err]
       (log/error "Promise rejected" {:error err} ::generate-memes)))

    )


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
