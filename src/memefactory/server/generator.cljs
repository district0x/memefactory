(ns memefactory.server.generator
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs-web3.utils :refer [js->cljkk camel-case]]
    [district.cljs-utils :refer [rand-str]]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-address contract-call instance]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.dank-token :as dank-token]
    [memefactory.server.contract.eternal-db :as eternal-db]
    [memefactory.server.contract.meme :as meme]
    [memefactory.server.contract.meme-factory :as meme-factory]
    [memefactory.server.contract.meme-registry :as meme-registry]
    [memefactory.server.contract.minime-token :as minime-token]
    [memefactory.server.contract.param-change :as param-change]
    [memefactory.server.contract.param-change-factory :as param-change-factory]
    [memefactory.server.contract.param-change-registry :as param-change-registry]
    [memefactory.server.contract.registry :as registry]
    [memefactory.server.contract.registry-entry :as registry-entry]
    [memefactory.server.deployer]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(defstate ^{:on-reload :noop} generator :start (start (merge (:generator @config)
                                                             (:generator (mount/args)))))


(defn get-screnarios [{:keys [:accounts :use-accounts :items-per-account :scenarios]}]
  (when (and (pos? use-accounts)
             (pos? items-per-account)
             (seq scenarios))
    (let [accounts-repeated (flatten (for [x accounts]
                                       (repeat items-per-account x)))
          scenarios (map #(if (keyword? %) {:scenario-type %} %) scenarios)
          scenarios-repeated (take (* use-accounts items-per-account) (cycle scenarios))]
      (partition 2 (interleave accounts-repeated scenarios-repeated)))))

(comment
  :scenario/create
  :scenario/challenge
  :scenario/commit-vote
  :scenario/reveal-vote
  :scenario/claim-vote-reward
  :scenario/buy
  :scenario/apply-param-change

  (get-screnarios {:use-accounts 2
                   :items-per-account 1
                   :scenarios [:scenario/create :scenario/buy]
                   :accounts ["0xccaca6119493350e670f740b89e028bad6b00041"
                              "0xcea88e5816cf24a137a0a4c925ab2ad6295b5a85"]}))




(defn generate-memes [{:keys [:accounts :memes/use-accounts :memes/items-per-account :memes/scenarios]}]
  (let [[max-total-supply max-start-price deposit commit-period-duration reveal-period-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :max-start-price :deposit :commit-period-duration
                                                            :reveal-period-duration])
          (map bn/number))]
    (doseq [[account {:keys [:scenario-type]}] (get-screnarios {:accounts accounts
                                                                :use-accounts use-accounts
                                                                :items-per-account items-per-account
                                                                :scenarios scenarios})]
      (let [name (rand-str 10)
            meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH"
            total-supply (inc (rand-int max-total-supply))
            start-price (inc (rand-int max-start-price))]

        (let [tx-hash (meme-factory/approve-and-create-meme {:name name
                                                             :meta-hash meta-hash
                                                             :total-supply total-supply
                                                             :start-price start-price
                                                             :amount deposit}
                                                            {:from account})]

          (when-not (= :scenario/create scenario-type)
            (let [{{:keys [:registryEntry]} :args} (meme-registry/registry-entry-event-in-tx tx-hash)]

              (registry-entry/approve-and-create-challenge registryEntry
                                                           {:meta-hash meta-hash
                                                            :amount deposit}
                                                           {:from account})

              (when-not (= :scenario/challenge scenario-type)
                (let [{:keys [:challenge/voting-token :reg-entry/creator]} (registry-entry/load-registry-entry registryEntry)
                      balance (minime-token/balance-of [:DANK voting-token] creator)]

                  (registry-entry/approve-and-commit-vote registryEntry
                                                          {:voting-token voting-token
                                                           :amount balance
                                                           :salt "abc"
                                                           :vote-option :vote.option/vote-for}
                                                          {:from creator})

                  (when-not (= :scenario/commit-vote scenario-type)
                    (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

                    (registry-entry/reveal-vote registryEntry
                                                {:vote-option :vote.option/vote-for
                                                 :salt "abc"}
                                                {:from creator})

                    (when-not (= :scenario/reveal-vote scenario-type)
                      (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

                      (registry-entry/claim-vote-reward registryEntry {:from creator})

                      (when-not (= :scenario/claim-vote-reward scenario-type)
                        (meme/buy registryEntry 1 {:from creator :value start-price})))))))))))))


(defn generate-param-changes [{:keys [:accounts
                                      :param-changes/use-accounts
                                      :param-changes/items-per-account
                                      :param-changes/scenarios]}]
  (let [[deposit challenge-period-duration]
        (->> (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration])
          (map bn/number))]
    (doseq [[account {:keys [:scenario-type :param-change-db]}]
            (get-screnarios {:accounts accounts
                             :use-accounts use-accounts
                             :items-per-account items-per-account
                             :scenarios scenarios})]

      (let [tx-hash (param-change-factory/approve-and-create-param-change
                      {:db (contract-address (or param-change-db :meme-registry-db))
                       :key :deposit
                       :value (web3/to-wei 800 :ether)
                       :amount deposit}
                      {:from account})

            registry-entry (:registryEntry (:args (param-change-registry/registry-entry-event-in-tx tx-hash)))]

        (when-not (= scenario-type :scenario/create)
          (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

          (param-change-registry/apply-param-change registry-entry {:from account}))))))


(defn start [opts]
  (let [opts (assoc opts :accounts (web3-eth/accounts @web3))]
    (generate-memes opts)
    (generate-param-changes opts)))


(comment
  (registry-entry/status @*registry-entry*))