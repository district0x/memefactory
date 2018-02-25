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

(defonce *registry-entry* (atom nil))

(defn generate-memes [{:keys [:memes/use-accounts :memes/items-per-account]}]
  (when (and (pos? use-accounts) (pos? items-per-account))
    (let [accounts (web3-eth/accounts @web3)
          [max-total-supply max-start-price deposit commit-period-duration reveal-period-duration]
          (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :max-start-price :deposit :commit-period-duration
                                                              :reveal-period-duration])
            (map bn/number))]
      (dotimes [address-index use-accounts]
        (dotimes [_ items-per-account]
          (let [creator (nth accounts address-index)
                name (rand-str 10)
                image-hash (rand-str 32)
                meta-hash (rand-str 32)
                total-supply (inc (rand-int max-total-supply))
                start-price (inc (rand-int max-start-price))]

            (let [tx-hash (meme-factory/approve-and-create-meme {:name name
                                                                 :image-hash image-hash
                                                                 :meta-hash meta-hash
                                                                 :total-supply total-supply
                                                                 :start-price start-price
                                                                 :amount deposit}
                                                                {:from creator})]

              (let [{{:keys [:registryEntry]} :args} (meme-registry/registry-entry-event-in-tx tx-hash)]

                (registry-entry/approve-and-create-challenge registryEntry
                                                             {:meta-hash (rand-str 32)
                                                              :amount deposit}
                                                             {:from creator})

                (let [{:keys [:challenge/voting-token :reg-entry/creator]} (registry-entry/load-registry-entry registryEntry)
                      balance (minime-token/balance-of [:DANK voting-token] creator)]

                  (registry-entry/approve-and-commit-vote registryEntry
                                                          {:voting-token voting-token
                                                           :amount balance
                                                           :salt "abc"
                                                           :vote-option :vote-option/vote-for}
                                                          {:from creator})

                  (web3-evm/increase-time! @web3 [(inc commit-period-duration)])
                  (web3-evm/mine! @web3)

                  (registry-entry/reveal-vote registryEntry
                                              {:vote-option :vote-option/vote-for
                                               :salt "abc"}
                                              {:from creator})

                  (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])
                  (web3-evm/mine! @web3)

                  (registry-entry/claim-voter-reward registryEntry {:from creator})

                  (meme/buy registryEntry 1 {:from creator :value start-price})

                  (print.foo/look (registry-entry/load-registry-entry registryEntry))
                  (print.foo/look (meme/load-meme registryEntry))

                  (reset! *registry-entry* registryEntry))))))))))


(defn generate-param-changes [{:keys [:param-changes/use-accounts :param-changes/items-per-account]}]
  (when (and (pos? use-accounts) (pos? items-per-account))
    (let [accounts (web3-eth/accounts @web3)
          [deposit challenge-period-duration]
          (->> (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration])
            (map bn/number))]
      (dotimes [address-index use-accounts]
        (dotimes [_ items-per-account]
          (let [creator (nth accounts address-index)
                tx-hash1 (param-change-factory/approve-and-create-param-change {:db (contract-address :meme-registry-db)
                                                                                :key :deposit
                                                                                :value (web3/to-wei 800 :ether)
                                                                                :amount deposit}
                                                                               {:from creator})

                tx-hash2 (param-change-factory/approve-and-create-param-change {:db (contract-address :param-change-registry-db)
                                                                                :key :challenge-dispensation
                                                                                :value 60
                                                                                :amount deposit}
                                                                               {:from creator})
                registry-entry1 (:registryEntry (:args (param-change-registry/registry-entry-event-in-tx tx-hash1)))
                registry-entry2 (:registryEntry (:args (param-change-registry/registry-entry-event-in-tx tx-hash2)))]

            (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
            (web3-evm/mine! @web3)

            (param-change-registry/apply-param-change registry-entry1 {:from creator})
            (param-change-registry/apply-param-change registry-entry2 {:from creator})

            (print.foo/look (param-change/load-param-change registry-entry1))
            (print.foo/look (param-change/load-param-change registry-entry2))

            ))))))


(defn start [opts]
  (generate-memes opts)
  (generate-param-changes opts))


(comment
  (registry-entry/status @*registry-entry*))