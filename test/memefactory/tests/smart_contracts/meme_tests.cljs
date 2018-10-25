(ns memefactory.tests.smart-contracts.meme-tests
  (:require [bignumber.core :as bn]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-factory :as meme-factory]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.contract.meme-token :as meme-token]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [print.foo :include-macros true :refer [look]]))

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")

#_(use-fixtures
  :once {:before (test-utils/create-before-fixture {:use-n-account-as-cut-collector 2
                                                    :use-n-account-as-deposit-collector 3
                                                    :meme-auction-cut 10})
         :after test-utils/after-fixture})

;;;;;;;;;;;
;; Meme  ;;
;;;;;;;;;;;

(defn create-meme
  "Creates a meme and returns its registry entry"
  [& [creator-addr deposit total-supply meta-hash :as args]]
  (-> (meme-factory/approve-and-create-meme {:meta-hash meta-hash
                                             :total-supply total-supply
                                             :amount deposit}
                                            {:from creator-addr})
      look
      (registry/meme-constructed-event-in-tx [:meme-registry :meme-registry-fwd])
      look
      :args :registry-entry
      ))

#_(deftest approve-and-create-meme-test
  (let [[creator-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit] (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit])
                                        (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)]

    (testing "Meme can be created under valid conditions"
      (is registry-entry))

    (testing "Cannot create meme with higher supply than maxTotalSupply TCR param"
      (is (thrown? js/Error (create-meme creator-addr deposit (inc max-total-supply) sample-meta-hash-1))))

    (testing "Created Meme has properties initialised as they should be"
      (let [meme (meme/load-meme registry-entry)]
        (is (= (:meme/meta-hash meme) sample-meta-hash-1))
        (is (= (:meme/total-supply meme) max-total-supply))))))

#_(deftest transfer-deposit-test
  (let [[creator-addr _ _ collector-address] (web3-eth/accounts @web3)
        collector-initial-balance (bn/number (dank-token/balance-of collector-address))
        [max-total-supply deposit challenge-period-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit :challenge-period-duration])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)]

    (testing "Meme deposit can't be transferred if not whitelisted"
      ;; it will not be whitelisted because we are still in challenge period
      (is (thrown? js/Error
                   (meme/transfer-deposit registry-entry))))

    (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

    ;; all conditions shold be valid after challenge period
    (testing "Meme deposit can be transferred from whitelisted meme to depositCollector address"
      (meme/transfer-deposit registry-entry)
      (let [collector-final-balance (bn/number (dank-token/balance-of collector-address))]
        (is (> collector-final-balance collector-initial-balance))))))

#_(deftest mint-test
  (let [[creator-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit :challenge-period-duration])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)]

    (testing "Meme cant be minted if it's not whitelisted"
      ;; it will not be whitelisted because we are still in challenge period
      (is (thrown? js/Error (meme/mint registry-entry max-total-supply {}))))

    (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

    ;; Now meme should be whitelisted
    (let [mint-count (quot max-total-supply 2)]
     (testing "Meme can be minted under valid conditions"
       (meme/mint registry-entry mint-count {}))

     (let [meme (meme/load-meme registry-entry)]
       (testing "All properties should be good after minting collectibles"
         (is (= (:meme/total-minted meme) mint-count))

         (is (apply (partial = creator-addr)
                    (map (fn [token-id]
                           (meme-token/owner-of token-id))
                         (range (:meme/token-id-start meme)
                                (+ (:meme/token-id-start meme)
                                   (:meme/total-minted meme)))))))
       (testing "Mint should work with passed amount that's bigger than totalSupply"
         (meme/mint registry-entry (+ 3 max-total-supply) {})
         (let [meme (meme/load-meme registry-entry)]
           (is (= (:meme/total-minted meme) (:meme/total-supply meme) max-total-supply))))))))
