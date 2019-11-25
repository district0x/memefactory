(ns memefactory.tests.smart-contracts.registry-entry-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.evm :as web3-evm]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.core.async :refer [go <!]]
            [cljs.test :as test :refer-macros [deftest is testing async]]
            [clojure.string :as string]
            [district.server.web3 :refer [web3]]
            [district.shared.async-helpers :refer [<?]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [memefactory.shared.contract.registry-entry :refer [vote-option->num vote-options]]
            [memefactory.tests.smart-contracts.meme-tests :refer [create-meme]]
            [memefactory.tests.smart-contracts.utils :refer [tx-reverted?]]))

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")

;;;;;;;;;;;;;;;;;;;
;; RegistryEntry ;;
;;;;;;;;;;;;;;;;;;;

(deftest approve-and-create-meme-registry-test
  (async done
         (go
           (let [[creator-addr] (map string/lower-case (<! (web3-eth/accounts @web3)))
                 creator-init-balance (<? (dank-token/balance-of creator-addr))
                 [max-total-supply deposit] (->> (<? (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit]))
                                                 (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))]

             (testing "Can create RegistryEntry (as Meme) with valid parameters"
               (is registry-entry))

             (testing "Created RegistryEntry has properties initialised as they should be"
               (let [entry (<! (registry-entry/load-registry-entry registry-entry))]
                 (is (= deposit (:reg-entry/deposit entry)))
                 (is (= creator-addr (:reg-entry/creator entry)))
                 (is (= 1 (:reg-entry/version entry)))
                 #_(is (bn/= (bn/number (<? (dank-token/balance-of creator-addr)))
                             (bn/- creator-init-balance deposit)))))

             (testing "Construct method of cannot be called twice"
               (is (tx-reverted? (<! (meme/construct registry-entry {:creator/address creator-addr :total-supply max-total-supply :meta-hash  sample-meta-hash-1}))))))
           (done))))

(deftest approve-and-create-challenge-test
  (async done
         (go
           (let [[creator-addr challenger-addr] (map string/lower-case (<! (web3-eth/accounts @web3)))
                 challenger-init-balance (bn/number (<? (dank-token/balance-of challenger-addr)))
                 [max-total-supply deposit challenge-period-duration
                  commit-period-duration reveal-period-duration max-auction-duration
                  challenge-dispensation]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                      [:max-total-supply :deposit :challenge-period-duration
                                                       :commit-period-duration :reveal-period-duration :max-auction-duration
                                                       :challenge-dispensation]))
                      (map bn/number))
                 meme-entry-1 (string/lower-case (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)))
                 challenge-fn #(registry-entry/approve-and-create-challenge meme-entry-1
                                                                            {:amount deposit
                                                                             :meta-hash sample-meta-hash-1}
                                                                            {:from challenger-addr})
                 challenge (<! (challenge-fn))
                 entry (<! (registry-entry/load-registry-entry meme-entry-1))]

             (testing "Can create challenge under valid condidtions"
               (is entry))

             (testing "Check properties of created challenge:"
               (testing "RegistryToken deposit should be transferred from challenger to RegistryEntry contract"
                 (is (= (bn/number (<? (dank-token/balance-of challenger-addr)))
                        (- challenger-init-balance deposit)))
                 (is (= (bn/number (<? (dank-token/balance-of (:reg-entry/address entry))))
                        (* 2 deposit))))

               (is (= challenger-addr (:challenge/challenger entry)))

               ;; We can't test time related thing with ganache because of
               ;; https://github.com/trufflesuite/ganache-core/issues/111

               #_(is (= (:challenge/commit-period-end entry)
                        (+ (bn/number timestamp) commit-period-duration)))

               #_(is (= (:challenge/reveal-period-end entry)
                        (+ (bn/number timestamp) commit-period-duration reveal-period-duration)))

               (is (= (:challenge/reward-pool entry)
                      ( / (bn/* (bn/number deposit) (- 100 challenge-dispensation)) 100)))

               (is (= sample-meta-hash-1 (:challenge/meta-hash entry))))

             (testing "Challenge cannot be created if RegistryEntry was already challenged"
               (is (tx-reverted? (<? (challenge-fn)))))

             (testing "Challenge cannot be created outside challenge period"
               (let [registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-2))]
                 (web3-evm/increase-time @web3 (inc challenge-period-duration))
                 (is (tx-reverted? (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                                    {:amount deposit
                                                                                     :meta-hash sample-meta-hash-2}
                                                                                    {:from challenger-addr}))))))
             (done)))))

(deftest approve-and-commit-vote-test
  (async done
         (go
           (let [[voter-addr creator-addr challenger-addr voter-addr2] (<! (web3-eth/accounts @web3))
                 voter-init-balance (<? (dank-token/balance-of voter-addr))
                 [max-total-supply deposit challenge-period-duration
                  commit-period-duration reveal-period-duration max-auction-duration
                  challenge-dispensation]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                      [:max-total-supply :deposit :challenge-period-duration
                                                       :commit-period-duration :reveal-period-duration :max-auction-duration
                                                       :challenge-dispensation]))
                      (map bn/number))
                 reg-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (<! (registry-entry/approve-and-create-challenge reg-entry
                                                                    {:amount deposit
                                                                     :meta-hash sample-meta-hash-1}
                                                                    {:from challenger-addr}))

                 {:keys [:reg-entry/address :challenge/commit-period-end]} (<! (registry-entry/load-registry-entry reg-entry))
                 vote-amount 10
                 reg-balance-before-vote (<! (dank-token/balance-of address))
                 salt "abc"
                 vote-option :vote.option/vote-for
                 vote-tx (<? (registry-entry/approve-and-commit-vote address
                                                                     {:amount vote-amount
                                                                      :salt salt
                                                                      :vote-option vote-option}
                                                                     {:from voter-addr}))]

             (testing "Vote can be committed under valid conditions"
               (is vote-tx))

             (testing "Check properties of committed vote"
               (let [vote (<! (registry-entry/load-registry-entry-vote address voter-addr))]

                 (is (= (bn/number (<? (dank-token/balance-of address)))
                        (+ (bn/number reg-balance-before-vote) vote-amount)))

                 (is (= (:vote/secret-hash vote)
                        (web3-utils/solidity-sha3 @web3 (vote-option->num vote-option) salt)))

                 (is (= (bn/number (:vote/amount vote))
                        vote-amount))))

             (testing "Vote cannot be committed outside vote commit period"
               (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))

               (is (tx-reverted? (<? (registry-entry/approve-and-commit-vote address
                                                                             {:amount vote-amount
                                                                              :salt salt
                                                                              :vote-option vote-option}
                                                                             {:from voter-addr2})))))
             (done)))))

(deftest approve-and-commit-vote-rejection-tests
  (async done
         (go
           (let [[voter-addr creator-addr challenger-addr] (<! (web3-eth/accounts @web3))
                 [max-total-supply deposit challenge-period-duration
                  commit-period-duration reveal-period-duration max-auction-duration
                  challenge-dispensation]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                      [:max-total-supply :deposit :challenge-period-duration
                                                       :commit-period-duration :reveal-period-duration :max-auction-duration
                                                       :challenge-dispensation]))
                      (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:amount deposit
                                                                     :meta-hash sample-meta-hash-1}
                                                                    {:from challenger-addr}))
                 {:keys [:reg-entry/address :challenge/commit-period-end]} (<! (registry-entry/load-registry-entry registry-entry))
                 first-vote-amount 10
                 salt "abc"
                 _ (<? (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount first-vote-amount
                                                                :salt salt
                                                                :vote-option :vote.option/vote-for}
                                                               {:from voter-addr}))
                 voter-balance (<? (dank-token/balance-of voter-addr))]
             (testing "Can't make second vote"
               (is (tx-reverted? (<? (registry-entry/approve-and-commit-vote registry-entry
                                                                             {:amount voter-balance
                                                                              :salt salt
                                                                              :vote-option :vote.option/vote-against}
                                                                             {:from voter-addr}))))))
           (done))))

(deftest reveal-vote-test
  (async done
         (go
           (let [[voter-addr creator-addr challenger-addr voter-addr2] (<! (web3-eth/accounts @web3))
                 [max-total-supply deposit challenge-period-duration
                  commit-period-duration reveal-period-duration max-auction-duration
                  challenge-dispensation]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                      [:max-total-supply :deposit :challenge-period-duration
                                                       :commit-period-duration :reveal-period-duration :max-auction-duration
                                                       :challenge-dispensation]))
                      (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:amount deposit
                                                                     :meta-hash sample-meta-hash-1}
                                                                    {:from challenger-addr}))
                 {:keys [:reg-entry/address :challenge/commit-period-end]} (<! (registry-entry/load-registry-entry registry-entry))
                 voter-addr1-init-balance (<? (dank-token/balance-of voter-addr))
                 voter-addr2-init-balance (<? (dank-token/balance-of voter-addr2))
                 vote-amount 10
                 salt "abc"
                 _ (<? (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount vote-amount
                                                                :salt salt
                                                                :vote-option :vote.option/vote-for}
                                                               {:from voter-addr}))
                 _ (<? (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount vote-amount
                                                                :salt salt
                                                                :vote-option :vote.option/vote-against}
                                                               {:from voter-addr2}))]

             (testing "Vote transferred DANK from the voter to the registry entry"
               (is (= (bn/number (<? (dank-token/balance-of voter-addr))) (- voter-addr1-init-balance vote-amount)))
               (is (= (bn/number (<? (dank-token/balance-of voter-addr2))) (- voter-addr2-init-balance vote-amount))))

             (testing "Vote cannot be revealed outside vote reveal period"
               (is (tx-reverted? (<? (registry-entry/reveal-vote registry-entry
                                                                 {:address voter-addr
                                                                  :vote-option :vote.option/vote-for
                                                                  :salt salt}
                                                                 {:from voter-addr})))))

             (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))

             (testing "Vote cannot be revealed with incorrect salt"
               (is (tx-reverted? (<? (registry-entry/reveal-vote registry-entry
                                                                 {:address voter-addr
                                                                  :vote-option :vote.option/vote-for
                                                                  :salt (str salt "x")}
                                                                 {:from voter-addr})))))

             (let [reveal-vote1 #(registry-entry/reveal-vote registry-entry
                                                             {:address voter-addr
                                                              :vote-option :vote.option/vote-for
                                                              :salt (str salt)}
                                                             {:from voter-addr})
                   _ (<? (registry-entry/reveal-vote registry-entry
                                                     {:address voter-addr2
                                                      :vote-option :vote.option/vote-against
                                                      :salt (str salt)}
                                                     {:from voter-addr2}))
                   reveal-tx (<? (reveal-vote1))

                   vote1 (<! (registry-entry/load-registry-entry-vote registry-entry voter-addr))
                   vote2 (<! (registry-entry/load-registry-entry-vote registry-entry voter-addr2))]

               (testing "Vote can be revealed under valid conditions"
                 (is reveal-tx))

               (testing "Check properties of revealed vote"
                 (is (:vote/revealed-on vote1))
                 (is (:vote/option vote1)))

               (testing "Both scenarios for vote options"
                 (is (= (-> vote1 :vote/option vote-options) :vote.option/vote-for))
                 (is (= (-> vote2 :vote/option vote-options) :vote.option/vote-against)))

               (testing "Revealing returned DANK token back to voters"
                 (is (bn/= (bn/number (<? (dank-token/balance-of voter-addr)))
                           (+ vote-amount (bn/number voter-addr1-init-balance))))
                 (is (bn/= (bn/number (<? (dank-token/balance-of voter-addr2)))
                           (+ vote-amount (bn/number voter-addr2-init-balance)))))

               (testing "Vote cannot be revealed twice"
                 (is (tx-reverted? (<? (reveal-vote1))))))
             (done)))))

(deftest claim-rewards
  (async done
         (go
           (let [[voter-addr creator-addr challenger-addr] (<! (web3-eth/accounts @web3))
                 [max-total-supply deposit challenge-period-duration
                  commit-period-duration reveal-period-duration max-auction-duration
                  challenge-dispensation]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                      [:max-total-supply :deposit :challenge-period-duration
                                                       :commit-period-duration :reveal-period-duration :max-auction-duration
                                                       :challenge-dispensation]))
                      (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:amount deposit
                                                                     :meta-hash sample-meta-hash-1}
                                                                    {:from challenger-addr}))

                 {:keys [:reg-entry/address :challenge/commit-period-end]} (<! (registry-entry/load-registry-entry registry-entry))
                 vote-amount 10
                 salt "abc"
                 _ (<? (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount vote-amount
                                                                :salt salt
                                                                :vote-option :vote.option/vote-for}
                                                               {:from voter-addr}))
                 _ (<! (web3-evm/increase-time @web3 (inc commit-period-duration)))
                 _ (<? (registry-entry/reveal-vote registry-entry
                                                   {:address voter-addr
                                                    :vote-option :vote.option/vote-for
                                                    :salt (str salt)}
                                                   {:from voter-addr}))]

             (<! (web3-evm/increase-time @web3 (inc reveal-period-duration)))

             (let [balance-before-claim (<? (dank-token/balance-of voter-addr))
                   reward-claim-tx (<! (registry-entry/claim-rewards registry-entry {:from voter-addr}))
                   balance-after-claim (<? (dank-token/balance-of voter-addr))
                   entry (<! (registry-entry/load-registry-entry registry-entry))
                   vote (<! (registry-entry/load-registry-entry-vote registry-entry voter-addr))]

               (testing "Vote reward can be claimed under valid condidtions"
                 (is reward-claim-tx))

               (testing "Check properties of claimed vote"
                 (is (bn/> balance-after-claim balance-before-claim))))
             (done)))))

(deftest reclaim-vote-amount-test
  (async done
         (go
           (let [[voter-addr creator-addr challenger-addr] (<! (web3-eth/accounts @web3))
                 [max-total-supply deposit challenge-period-duration
                  commit-period-duration reveal-period-duration max-auction-duration
                  challenge-dispensation]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                      [:max-total-supply :deposit :challenge-period-duration
                                                       :commit-period-duration :reveal-period-duration :max-auction-duration
                                                       :challenge-dispensation]))
                      (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                    {:amount deposit
                                                                     :meta-hash sample-meta-hash-1}
                                                                    {:from challenger-addr}))

                 {:keys [:reg-entry/address :challenge/commit-period-end]} (<! (registry-entry/load-registry-entry registry-entry))
                 vote-amount 10
                 salt "abc"
                 _ (<? (registry-entry/approve-and-commit-vote registry-entry
                                                               {:amount #_bn/number vote-amount
                                                                :salt salt
                                                                :vote-option :vote.option/vote-for}
                                                               {:from voter-addr}))]

             ;; after reveal period
             (<! (web3-evm/increase-time @web3 (inc (+ commit-period-duration reveal-period-duration))))

             (let [balance-before-reclaim (<? (dank-token/balance-of voter-addr))
                   reclaim-vote-deposit #(registry-entry/reclaim-vote-amount registry-entry {:from voter-addr})
                   reward-claim-tx (<? (reclaim-vote-deposit))
                   balance-after-reclaim (<? (dank-token/balance-of voter-addr))]

               (testing "Vote reward can be claimed under valid condidtions"
                 (is reward-claim-tx))

               (testing "Vote amount is reclaimed"
                 (is (= (js/BigInt vote-amount)
                        (- (js/BigInt balance-after-reclaim) (js/BigInt balance-before-reclaim)))))

               (testing "Cannot be called twice"
                 (is (tx-reverted? (<? (reclaim-vote-deposit))))))
             (done)))))
