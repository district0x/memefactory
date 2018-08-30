(ns memefactory.tests.smart-contracts.registry-entry-tests
  (:require [bignumber.core :as bn]
            [cljs-solidity-sha3.core :refer [solidity-sha3]]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [district.server.smart-contracts :refer [contract-call]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme-registry :as meme-registry]
            [memefactory.server.contract.minime-token :as minime-token]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [memefactory.shared.contract.registry-entry :refer [vote-option->num vote-options]]
            [memefactory.tests.smart-contracts.meme-tests :refer [create-meme]]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [print.foo :include-macros true :refer [look]]))

(use-fixtures
  :each {:before (test-utils/create-before-fixture {:use-n-account-as-cut-collector 2
                                                    :use-n-account-as-deposit-collector 3
                                                    :meme-auction-cut 10})
         :after test-utils/after-fixture})

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")

;;;;;;;;;;;;;;;;;;;
;; RegistryEntry ;;
;;;;;;;;;;;;;;;;;;;

(defn load-registry-entry [entry-address]
  (merge (registry-entry/load-registry-entry entry-address)
         (registry-entry/load-registry-entry-challenge entry-address)))

(deftest approve-and-create-meme-registry-test
  (let [[creator-addr] (web3-eth/accounts @web3)
        creator-init-balance (dank-token/balance-of creator-addr)
        [max-total-supply deposit] (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit])
                                        (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)]

    (testing "Can create RegistryEntry (as Meme) with valid parameters"
      (is registry-entry))

    (testing "Created RegistryEntry has properties initialised as they should be"
      (let [entry (load-registry-entry registry-entry)]
        (is (= (web3/from-wei deposit :ether) (str (:reg-entry/deposit entry))))
        (is (= creator-addr (:reg-entry/creator entry)))
        (is (= 1 (:reg-entry/version entry)))
        (is (bn/= (dank-token/balance-of creator-addr)
                  (bn/- creator-init-balance deposit)))
        (is (= (bn/number (dank-token/balance-of (:reg-entry/address entry)))
               deposit))))

    (testing "Construct method of cannot be called twice"
      (is (thrown? js/Error (contract-call :meme :construct creator-addr 1 sample-meta-hash-1 max-total-supply))))))

(deftest approve-and-create-challenge-test
  (let [[creator-addr challenger-addr] (web3-eth/accounts @web3)
        challenger-init-balance (dank-token/balance-of challenger-addr)
        [max-total-supply deposit challenge-period-duration
         commit-period-duration reveal-period-duration max-auction-duration
         vote-quorum challenge-dispensation]
        (->> (eternal-db/get-uint-values :meme-registry-db
                                         [:max-total-supply :deposit :challenge-period-duration
                                          :commit-period-duration :reveal-period-duration :max-auction-duration
                                          :vote-quorum :challenge-dispensation])
             (map bn/number))
        meme-entry-1 (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        challenge #(registry-entry/approve-and-create-challenge meme-entry-1
                                                                {:amount deposit
                                                                 :meta-hash sample-meta-hash-1}
                                                                {:from challenger-addr})
        {:keys [timestamp]} (-> (challenge)
                                meme-registry/registry-entry-event-in-tx
                                :args
                                (select-keys [:registry-entry :timestamp]))
        entry (load-registry-entry meme-entry-1)]

    (testing "Can create challenge under valid condidtions"
      (is entry))

    (testing "Check properties of created challenge:"
      (testing "RegistryToken deposit should be transferred from challenger to RegistryEntry contract"
        (is (= (bn/number (dank-token/balance-of challenger-addr))
               (- challenger-init-balance deposit)))
        (is (= (bn/number (dank-token/balance-of (:reg-entry/address entry)))
               (* 2 deposit)))) ;; the registry entry should contain creator and challenge deposit

      (is (= challenger-addr (:challenge/challenger entry)))
      (is (= vote-quorum (:challenge/vote-quorum entry)))   ;; needs to be returned by RegistryEntry.loadRegistryEntry
      (is (= (:challenge/commit-period-end entry)
             (+ (bn/number timestamp) commit-period-duration)))
      (is (= (:challenge/reveal-period-end entry)
             (+ (bn/number timestamp) commit-period-duration reveal-period-duration)))
      (is (= (str (bn// (bn/* (web3/to-big-number deposit) (- 100 challenge-dispensation))
                     100))
             (web3/to-wei (:challenge/reward-pool entry) :ether)))
      (is (= sample-meta-hash-1 (:challenge/meta-hash entry))))

    (testing "Challenge cannot be created if RegistryEntry was already challenged"
      (is (thrown? js/Error (challenge))))

    (testing "Challenge cannot be created outside challenge period"
      (let [registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-2)]
        (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
        (is (thrown? js/Error (-> registry-entry
                               (registry-entry/approve-and-create-challenge
                                {:amount deposit
                                 :meta-hash sample-meta-hash-2}))))))))

(deftest approve-and-commit-vote-test
  (let [[voter-addr creator-addr challenger-addr voter-addr2] (web3-eth/accounts @web3)
        voter-init-balance (dank-token/balance-of voter-addr)
        [max-total-supply deposit challenge-period-duration
         commit-period-duration reveal-period-duration max-auction-duration
         vote-quorum challenge-dispensation]
        (->> (eternal-db/get-uint-values :meme-registry-db
                                         [:max-total-supply :deposit :challenge-period-duration
                                          :commit-period-duration :reveal-period-duration :max-auction-duration
                                          :vote-quorum :challenge-dispensation])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        _ (registry-entry/approve-and-create-challenge registry-entry
                                                       {:amount deposit
                                                        :meta-hash sample-meta-hash-1}
                                                       {:from challenger-addr})
        {:keys [:reg-entry/address :challenge/commit-period-end]} (load-registry-entry registry-entry)
        vote-amount (dank-token/balance-of voter-addr)
        reg-balance-before-vote (dank-token/balance-of address)
        salt "abc"
        vote-option :vote.option/vote-for
        vote-tx (registry-entry/approve-and-commit-vote registry-entry
                                                        {:amount vote-amount
                                                         :salt salt
                                                         :vote-option vote-option}
                                                        {:from voter-addr})]

    (testing "Vote can be committed under valid conditions"
      (is vote-tx))

    (testing "Check properties of committed vote"
      (let [vote (registry-entry/load-vote registry-entry voter-addr)]
        (is (bn/= (dank-token/balance-of address)
                  (bn/+ reg-balance-before-vote vote-amount)))
        (is (= (:vote/secret-hash vote)
               (solidity-sha3 (vote-option->num vote-option) salt)))
        (is (bn/= (web3/to-big-number (:vote/amount vote))
                  vote-amount))))

    (testing "Vote cannot be committed outside vote commit period"
      (web3-evm/increase-time! @web3 [(inc commit-period-duration)])
      (is (thrown? js/Error (registry-entry/approve-and-commit-vote registry-entry
                                                                    {:amount vote-amount
                                                                     :salt salt
                                                                     :vote-option vote-option}
                                                                    {:from voter-addr2}))))))

(deftest approve-and-commit-vote-rejection-tests
  (let [[voter-addr creator-addr challenger-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration
         commit-period-duration reveal-period-duration max-auction-duration
         vote-quorum challenge-dispensation]
        (->> (eternal-db/get-uint-values :meme-registry-db
                                         [:max-total-supply :deposit :challenge-period-duration
                                          :commit-period-duration :reveal-period-duration :max-auction-duration
                                          :vote-quorum :challenge-dispensation])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        _ (registry-entry/approve-and-create-challenge registry-entry
                                                       {:amount deposit
                                                        :meta-hash sample-meta-hash-1}
                                                       {:from challenger-addr})
        {:keys [:reg-entry/address :challenge/commit-period-end]} (load-registry-entry registry-entry)
        first-vote-amount (-> (dank-token/balance-of voter-addr) (bn/number) (/ 2))
        salt "abc"
        _ (registry-entry/approve-and-commit-vote registry-entry
                                                  {:amount first-vote-amount
                                                   :salt salt
                                                   :vote-option :vote.option/vote-for}
                                                  {:from voter-addr})]

    (testing "Can't make second vote"
      (is (thrown? js/Error (registry-entry/approve-and-commit-vote registry-entry
                                                                    {:amount (dank-token/balance-of voter-addr)
                                                                     :salt salt
                                                                     :vote-option :vote.option/vote-against}
                                                                    {:from voter-addr}))))))

(deftest reveal-vote-test
  (let [[voter-addr creator-addr challenger-addr voter-addr2] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration
         commit-period-duration reveal-period-duration max-auction-duration
         vote-quorum challenge-dispensation]
        (->> (eternal-db/get-uint-values :meme-registry-db
                                         [:max-total-supply :deposit :challenge-period-duration
                                          :commit-period-duration :reveal-period-duration :max-auction-duration
                                          :vote-quorum :challenge-dispensation])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        _ (registry-entry/approve-and-create-challenge registry-entry
                                                       {:amount deposit
                                                        :meta-hash sample-meta-hash-1}
                                                       {:from challenger-addr})
        {:keys [:reg-entry/address :challenge/commit-period-end]} (load-registry-entry registry-entry)
        vote-amount (bn/number (dank-token/balance-of voter-addr))
        salt "abc"
        _ (registry-entry/approve-and-commit-vote registry-entry
                                                  {:amount vote-amount
                                                   :salt salt
                                                   :vote-option :vote.option/vote-for}
                                                  {:from voter-addr})
        _ (registry-entry/approve-and-commit-vote registry-entry
                                                  {:amount vote-amount
                                                   :salt salt
                                                   :vote-option :vote.option/vote-against}
                                                  {:from voter-addr2})]

    (testing "Vote transferred DANK from the voter to the registry entry"
      (is (zero? (bn/number (dank-token/balance-of voter-addr))))
      (is (zero? (bn/number (dank-token/balance-of voter-addr2)))))

    (testing "Vote cannot be revealed outside vote reveal period"
      (is (thrown? js/Error
                   (registry-entry/reveal-vote registry-entry
                                               {:vote-option :vote.option/vote-for
                                                :salt salt}
                                               {:from voter-addr}))))
    (web3-evm/increase-time! @web3 [(inc commit-period-duration)])

    (testing "Vote cannot be revealed with incorrect salt"
      (is (thrown? js/Error
                   (registry-entry/reveal-vote registry-entry
                                               {:vote-option :vote.option/vote-for
                                                :salt (str salt "x")}
                                               {:from voter-addr}))))

    (let [reveal-vote1 #(registry-entry/reveal-vote registry-entry
                                                    {:vote-option :vote.option/vote-for
                                                     :salt (str salt)}
                                                    {:from voter-addr})
          _ (registry-entry/reveal-vote registry-entry
                                        {:vote-option :vote.option/vote-against
                                         :salt (str salt)}
                                        {:from voter-addr2})
          reveal-tx (reveal-vote1)
          vote1 (registry-entry/load-vote registry-entry voter-addr)
          vote2 (registry-entry/load-vote registry-entry voter-addr2)]

      (testing "Vote can be revealed under valid conditions"
        (is reveal-tx))

      (testing "Check properties of revealed vote"
        (is (:vote/revealed-on vote1))
        (is (:vote/option vote1)))

      (testing "Both scenarios for vote options"
        (is (= (-> vote1 :vote/option vote-options) :vote.option/vote-for))
        (is (= (-> vote2 :vote/option vote-options) :vote.option/vote-against)))

      (testing "Revealing returned DANK token back to voters"
        (is (= vote-amount (bn/number (dank-token/balance-of voter-addr))))
        (is (= vote-amount (bn/number (dank-token/balance-of voter-addr2)))))

      (testing "Vote cannot be revealed twice"
        (is (thrown? js/Error (reveal-vote1)))))))


(deftest claim-vote-reward-test
  (let [[voter-addr creator-addr challenger-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration
         commit-period-duration reveal-period-duration max-auction-duration
         vote-quorum challenge-dispensation]
        (->> (eternal-db/get-uint-values :meme-registry-db
                                         [:max-total-supply :deposit :challenge-period-duration
                                          :commit-period-duration :reveal-period-duration :max-auction-duration
                                          :vote-quorum :challenge-dispensation])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        _ (registry-entry/approve-and-create-challenge registry-entry
                                                       {:amount deposit
                                                        :meta-hash sample-meta-hash-1}
                                                       {:from challenger-addr})
        {:keys [:reg-entry/address :challenge/commit-period-end]} (load-registry-entry registry-entry)
        vote-amount (bn/number (dank-token/balance-of voter-addr))
        salt "abc"
        _ (registry-entry/approve-and-commit-vote registry-entry
                                                  {:amount vote-amount
                                                   :salt salt
                                                   :vote-option :vote.option/vote-for}
                                                  {:from voter-addr})
        _ (web3-evm/increase-time! @web3 [(inc commit-period-duration)])
        _ (registry-entry/reveal-vote registry-entry
                                      {:vote-option :vote.option/vote-for
                                       :salt (str salt)}
                                      {:from voter-addr})]

    (testing "Cannot be called before reveal period is over"
      (is (thrown? js/Error (registry-entry/claim-vote-reward registry-entry {:from voter-addr}))))

    (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

    (let [balance-before-claim (dank-token/balance-of voter-addr)
          reward-claim #(registry-entry/claim-vote-reward registry-entry {:from voter-addr})
          reward-claim-tx (reward-claim)
          balance-after-claim (dank-token/balance-of voter-addr)
          entry (load-registry-entry registry-entry)
          timestamp (-> reward-claim-tx
                        meme-registry/registry-entry-event-in-tx
                        :args :timestamp
                        bn/number)
          vote (registry-entry/load-vote registry-entry voter-addr)]

      (testing "Vote reward can be claimed under valid condidtions"
        (is reward-claim-tx))

      (testing "Check properties of claimed vote"
        (is (= (:vote/claimed-reward-on vote) timestamp))
        (is (bn/> balance-after-claim balance-before-claim)))

      (testing "Cannot be called twice"
        (is (thrown? js/Error (reward-claim)))))))

(deftest claim-challenge-reward-test
  (try
    (let [[voter-addr creator-addr challenger-addr] (web3-eth/accounts @web3)
          [max-total-supply deposit challenge-period-duration
           commit-period-duration reveal-period-duration max-auction-duration
           vote-quorum challenge-dispensation]
          (->> (eternal-db/get-uint-values :meme-registry-db
                                           [:max-total-supply :deposit :challenge-period-duration
                                            :commit-period-duration :reveal-period-duration :max-auction-duration
                                            :vote-quorum :challenge-dispensation])
               (map bn/number))
          registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
          _ (registry-entry/approve-and-create-challenge registry-entry
                                                         {:amount deposit
                                                          :meta-hash sample-meta-hash-1}
                                                         {:from challenger-addr})
          {:keys [:reg-entry/address :challenge/commit-period-end]} (load-registry-entry registry-entry)
          vote-amount (bn/number (dank-token/balance-of voter-addr))
          salt "abc"
          _ (registry-entry/approve-and-commit-vote registry-entry
                                                    {:amount vote-amount
                                                     :salt salt
                                                     :vote-option :vote.option/vote-against}
                                                    {:from voter-addr})
          _ (web3-evm/increase-time! @web3 [(inc commit-period-duration)])
          _ (registry-entry/reveal-vote registry-entry
                                        {:vote-option :vote.option/vote-against
                                         :salt (str salt)}
                                        {:from voter-addr})
          balance-before-claim (dank-token/balance-of challenger-addr)]

      (testing "Cannot be called before reveal period is over"
        (is (thrown? js/Error (registry-entry/claim-challenge-reward registry-entry {}))))

      (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
      (web3-evm/mine! @web3)

      (let [reward-claim #(registry-entry/claim-challenge-reward registry-entry {})
            reward-claim-tx (reward-claim)
            balance-after-claim (dank-token/balance-of challenger-addr)
            entry (load-registry-entry registry-entry)
            timestamp (-> reward-claim-tx
                          meme-registry/registry-entry-event-in-tx
                          :args :timestamp
                          bn/number)]

        (testing "Challenge reward can be claimed under valid condidtions"
          (is reward-claim-tx))

        (testing "Check properties of claimed challenge"
          (is (= (:challenge/claimed-reward-on entry) timestamp))
          (is (bn/> balance-after-claim balance-before-claim)))

        (testing "Cannot be called twice"
          (is (thrown? js/Error (reward-claim))))))
    (catch js/Error e
      (println e)
      (println (.-stack e)))))

(deftest reclaim-vote-deposit-test
  (let [[voter-addr creator-addr challenger-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration
         commit-period-duration reveal-period-duration max-auction-duration
         vote-quorum challenge-dispensation]
        (->> (eternal-db/get-uint-values :meme-registry-db
                                         [:max-total-supply :deposit :challenge-period-duration
                                          :commit-period-duration :reveal-period-duration :max-auction-duration
                                          :vote-quorum :challenge-dispensation])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        _ (registry-entry/approve-and-create-challenge registry-entry
                                                       {:amount deposit
                                                        :meta-hash sample-meta-hash-1}
                                                       {:from challenger-addr})
        {:keys [:reg-entry/address :challenge/commit-period-end]} (load-registry-entry registry-entry)
        vote-amount (dank-token/balance-of voter-addr)
        salt "abc"
        _ (registry-entry/approve-and-commit-vote registry-entry
                                                  {:amount (bn/number vote-amount)
                                                   :salt salt
                                                   :vote-option :vote.option/vote-for}
                                                  {:from voter-addr})
        _ (web3-evm/increase-time! @web3 [(inc commit-period-duration)])]

    (web3-evm/increase-time! @web3 [(inc reveal-period-duration)])

    (let [balance-before-reclaim (dank-token/balance-of voter-addr)
          vote-deposit-reclaim #(registry-entry/reclaim-vote-deposit registry-entry {:from voter-addr})

          reward-claim-tx (vote-deposit-reclaim)
          balance-after-reclaim (dank-token/balance-of voter-addr)]

      (testing "Vote reward can be claimed under valid condidtions"
        (is reward-claim-tx))

      (testing "vote amount is reclaimed"
        (is (bn/= vote-amount (bn/- balance-after-reclaim balance-before-reclaim))))

      (testing "Cannot be called twice"
        (is (thrown? js/Error (vote-deposit-reclaim)))))))
