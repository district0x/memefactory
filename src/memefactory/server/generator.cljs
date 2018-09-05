(ns memefactory.server.generator
  (:require
    [bignumber.core :as bn]
    [cljs-ipfs-api.files :as ipfs-files]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs-web3.utils :refer [js->cljkk camel-case]]
    [district.cljs-utils :refer [rand-str]]
    [district.format :as format]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-address contract-call instance]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.dank-token :as dank-token]
    [memefactory.server.contract.eternal-db :as eternal-db]
    [memefactory.server.contract.meme :as meme]
    [memefactory.server.contract.meme-auction :as meme-auction]
    [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
    [memefactory.server.contract.meme-factory :as meme-factory]
    [memefactory.server.contract.meme-registry :as meme-registry]
    [memefactory.server.contract.meme-token :as meme-token]
    [memefactory.server.contract.minime-token :as minime-token]
    [memefactory.server.contract.param-change :as param-change]
    [memefactory.server.contract.param-change-factory :as param-change-factory]
    [memefactory.server.contract.param-change-registry :as param-change-registry]
    [memefactory.server.contract.registry :as registry]
    [memefactory.server.contract.registry-entry :as registry-entry]
    [memefactory.server.deployer]
    [taoensso.timbre :as log]
    [mount.core :as mount :refer [defstate]]
    [print.foo :refer [look] :include-macros true]
    [memefactory.server.macros :refer [try-catch]]))

(def fs (js/require "fs"))

(declare start)
(defstate ^{:on-reload :noop} generator :start (start (merge (:generator @config)
                                                             (:generator (mount/args)))))

(defn get-scenarios [{:keys [:accounts :use-accounts :items-per-account :scenarios]}]
  (when (and (pos? use-accounts)
             (pos? items-per-account)
             (seq scenarios))
    (let [accounts-repeated (flatten (for [x accounts]
                                       (repeat items-per-account x)))
          scenarios (map #(if (keyword? %) {:scenario-type %} %) scenarios)
          scenarios-repeated (take (* use-accounts items-per-account) (cycle scenarios))]
      (partition 2 (interleave accounts-repeated scenarios-repeated)))))

(defn upload-meme []
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
                           (reject err)
                           (do
                             (log/info (str "Uploaded " file " received") {:image-hash image-hash} ::upload-meme)
                             (resolve image-hash))))))))))))

(defn upload-meme-meta [image-hash]
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
            (log/error "ifps error" {:error err} ::upload-meme-meta)
            (do
              (log/info "Uploaded meta received " {:meta-hash meta-hash} ::upload-meme-meta)
              (resolve meta-hash)))))))))

(defn generate-memes [{:keys [:accounts :memes/use-accounts :memes/items-per-account :memes/scenarios]}]
  (let [[max-total-supply max-auction-duration deposit commit-period-duration reveal-period-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :max-auction-duration :deposit :commit-period-duration
                                                            :reveal-period-duration])
             (map bn/number))
        scenarios (get-scenarios {:accounts accounts
                                  :use-accounts use-accounts
                                  :items-per-account items-per-account
                                  :scenarios scenarios})]
    (log/info "Going to generate" scenarios ::generate-memes)
    (doseq [[account {:keys [:scenario-type]}] scenarios]
      (-> (upload-meme)
          (.then upload-meme-meta)
          (.then (fn [meta-hash]
                   (try-catch
                    (let [total-supply (inc (rand-int max-total-supply))
                          auction-duration (+ 60 (rand-int (- max-auction-duration 60)))]

                      (let [tx-hash (meme-factory/approve-and-create-meme {:meta-hash meta-hash
                                                                           :total-supply total-supply
                                                                           :amount deposit}
                                                                          {:from account})]


                        (when-not (= :scenario/create scenario-type)
                          (let [{{:keys [:registry-entry]} :args} (meme-registry/registry-entry-event-in-tx tx-hash)]

                            (when-not registry-entry
                              (throw (js/Error. "Registry Entry wasn't found")))

                            (registry-entry/approve-and-create-challenge registry-entry
                                                                         {:meta-hash meta-hash
                                                                          :amount deposit}
                                                                         {:from account})

                            (when-not (= :scenario/challenge scenario-type)
                              (let [{:keys [:reg-entry/creator]} (registry-entry/load-registry-entry registry-entry)
                                    balance (dank-token/balance-of creator)]

                                (registry-entry/approve-and-commit-vote registry-entry
                                                                        {:amount balance
                                                                         :salt "abc"
                                                                         :vote-option :vote.option/vote-for}
                                                                        {:from creator})

                                (when-not (= :scenario/commit-vote scenario-type)

                                  (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

                                  (registry-entry/reveal-vote registry-entry
                                                              {:vote-option :vote.option/vote-for
                                                               :salt "abc"}
                                                              {:from creator})

                                  (when-not (= :scenario/reveal-vote scenario-type)
                                    (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

                                    (registry-entry/claim-vote-reward registry-entry {:from creator})

                                    (when-not (= :scenario/claim-vote-reward scenario-type)
                                      (let [tx-hash (meme/mint registry-entry (dec total-supply) {:from creator})
                                            [_ first-token-id last-token-id] (-> (meme-registry/registry-entry-event-in-tx tx-hash)
                                                                                 :args
                                                                                 :data
                                                                                 (->> (map bn/number)))
                                            token-ids (range first-token-id (inc last-token-id))]

                                        (when (empty? token-ids)
                                          (throw (js/Error. "Token IDs weren't found")))

                                        (let [tx-hash (meme-token/transfer-multi-and-start-auction {:from creator
                                                                                                    :token-ids token-ids
                                                                                                    :start-price (web3/to-wei 0.1 :ether)
                                                                                                    :end-price (web3/to-wei 0.01 :ether)
                                                                                                    :duration auction-duration
                                                                                                    :description "some auction"})
                                              {{:keys [:meme-auction]} :args} (meme-auction-factory/meme-auction-event-in-tx tx-hash)]

                                          (when-not meme-auction
                                            (throw (js/Error. "Meme Auction wasn't found")))

                                          (meme-auction/buy meme-auction {:from creator :value (web3/to-wei 0.1 :ether)})))))))))))))))))))


(defn generate-param-changes [{:keys [:accounts
                                      :param-changes/use-accounts
                                      :param-changes/items-per-account
                                      :param-changes/scenarios]}]
  (let [[deposit challenge-period-duration] (->> (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration])
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
            (throw (js/Error. "Registry Entry wasn't found")))

          (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

          (param-change-registry/apply-param-change registry-entry {:from account}))))))

(defn start [opts]
  (let [opts (assoc opts :accounts (web3-eth/accounts @web3))]
    (generate-memes opts)
    (generate-param-changes opts)))
