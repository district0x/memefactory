(ns memefactory.server.generator
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.cljs-utils :refer [rand-str]]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-address contract-call]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.eternal-db :as eternal-db]
    [memefactory.server.contract.meme-factory :as meme-factory]
    [memefactory.server.contract.meme-registry :as meme-registry]
    [memefactory.server.contract.dank-token :as dank-token]
    [memefactory.server.contract.registry-entry :as registry-entry]
    [memefactory.server.deployer]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(defstate ^{:on-reload :noop} generator :start (start (merge (:generator @config)
                                                             (:generator (mount/args)))))


(defn start [{:keys [:use-accounts :memes-per-account]}]
  (let [accounts (web3-eth/accounts @web3)
        [max-total-supply max-start-price deposit]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :max-start-price :deposit])
          (map bn/number))]
    (dotimes [address-index use-accounts]
      (dotimes [_ memes-per-account]
        (let [owner (nth accounts address-index)
              name (rand-str 10)
              image-hash (rand-str 32)
              meta-hash (rand-str 32)
              total-supply (inc (rand-int max-total-supply))
              start-price (inc (rand-int max-start-price))]

          (dank-token/approve {:spender (contract-address :meme-factory) :amount deposit}
                              {:from owner})

          (let [tx-hash (meme-factory/create-meme {:creator owner
                                                   :name name
                                                   :image-hash image-hash
                                                   :meta-hash meta-hash
                                                   :total-supply total-supply
                                                   :start-price start-price}
                                                  {:from owner})]

            (let [{{:keys [:registryEntry]} :args} (print.foo/look (meme-registry/registry-entry-event-in-tx tx-hash))]
              #_(dank-token/approve {:spender registryEntry :amount deposit} {:from owner})
              #_(registry-entry/create-challenge registryEntry
                                                 {:meta-hash (rand-str 32)}
                                                 {:from owner})

              #_ (contract-call [:meme registryEntry]
                             :receive-approval
                             owner
                             owner
                             owner
                             "0x0"
                             {:gas 200000 :from owner})

              #_(contract-call [:meme registryEntry] :create-challenge (rand-str 32) {:gas 200000 :from owner})

              (print.foo/look registryEntry)
              (print.foo/look (web3-eth/get-code @web3 (contract-address :meme)))
              (print.foo/look (web3-eth/get-code @web3 registryEntry))
              (print.foo/look (web3-eth/get-code @web3 (contract-address :meme-registry-fwd)))
              (registry-entry/approve-and-create-challenge registryEntry
                                                           {:meta-hash (rand-str 32)
                                                            :amount deposit}
                                                           {:from owner})))


          #_(let [tx-hash (meme-factory/approve-and-create-meme {:creator owner
                                                                 :name name
                                                                 :image-hash image-hash
                                                                 :meta-hash meta-hash
                                                                 :total-supply total-supply
                                                                 :start-price start-price
                                                                 :amount deposit}
                                                                {:from owner})]

              (let [{{:keys [:registryEntry]} :args} (meme-registry/registry-entry-event-in-tx tx-hash)]
                #_(dank-token/approve {:spender registryEntry :amount deposit} {:from owner})
                #_(registry-entry/create-challenge registryEntry
                                                   {:meta-hash (rand-str 32)}
                                                   {:from owner})

                (registry-entry/approve-and-create-challenge registryEntry
                                                             {:meta-hash (rand-str 32)
                                                              :amount deposit}
                                                             {:from owner})))

          )))))


(comment
  (contract-address :meme)
  (web3-eth/get-code @web3 (contract-address :meme-factory))


  (memefactory.server.contract.dank-token/approve-and-call
    {:spender 0x268dc55833f165e49ee229cab015fb6399f54d9a
     :amount 1
     :extra-data ""}
    {:gas 1200000
     :from (first (web3-eth/accounts @web3))})

  (web3-evm/increase-time! @web3 [(time/in-seconds (time/minutes 2))])
  (registry-entry/approve-and-create-challenge "0xf65488560dc93a0233186dc6f1ec8a1078405ac3"
                                               {:meta-hash (rand-str 32)
                                                :amount (bignumber.core/number (eternal-db/get-uint-value :meme-registry-db :deposit))}
                                               {:from (first (web3-eth/accounts @web3))})

  (registry-entry/status [:meme "0x268dc55833f165e49ee229cab015fb6399f54d9a"])
  (registry-entry/registry [:meme "0x268dc55833f165e49ee229cab015fb6399f54d9a"]))