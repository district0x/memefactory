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
    [memefactory.server.contract.meme-factory :as meme-factory]
    [memefactory.server.contract.meme-registry :as meme-registry]
    [memefactory.server.contract.minime-token :as minime-token]
    [memefactory.server.contract.registry-entry :as registry-entry]
    [memefactory.server.deployer]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(defstate ^{:on-reload :noop} generator :start (start (merge (:generator @config)
                                                             (:generator (mount/args)))))

(defonce *registry-entry* (atom nil))

(defn start [{:keys [:use-accounts :memes-per-account]}]
  (let [accounts (web3-eth/accounts @web3)
        [max-total-supply max-start-price deposit commit-period-duration reveal-period-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :max-start-price :deposit :commit-period-duration
                                                            :reveal-period-duration])
          (map bn/number))]
    (dotimes [address-index use-accounts]
      (dotimes [_ memes-per-account]
        (let [owner (nth accounts address-index)
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
                                                              {:from owner})]

            (let [{{:keys [:registryEntry]} :args} (meme-registry/registry-entry-event-in-tx tx-hash)]

              (registry-entry/approve-and-create-challenge registryEntry
                                                           {:meta-hash (rand-str 32)
                                                            :amount deposit}
                                                           {:from owner})

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

                (print.foo/look (registry-entry/voter registryEntry creator))
                (print.foo/look (registry-entry/load-registry-entry registryEntry)))

              (reset! *registry-entry* registryEntry))))
        ))))



(comment
  (registry-entry/status @*registry-entry*))