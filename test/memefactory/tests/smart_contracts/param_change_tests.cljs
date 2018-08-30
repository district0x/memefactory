(ns memefactory.tests.smart-contracts.param-change-tests
  (:require [bignumber.core :as bn]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [district.server.smart-contracts :refer [contract-address]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.param-change :as param-change]
            [memefactory.server.contract.param-change-factory :as param-change-factory]
            [memefactory.server.contract.param-change-registry :as param-change-registry]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [print.foo :include-macros true :refer [look]]))

(use-fixtures
  :each {:before (test-utils/create-before-fixture {:use-n-account-as-cut-collector 2
                                                    :use-n-account-as-deposit-collector 3
                                                    :meme-auction-cut 10})
         :after test-utils/after-fixture})

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")

;;;;;;;;;;;;;;;;;
;; ParamChange ;;
;;;;;;;;;;;;;;;;;

(deftest approve-and-create-param-change-test
  (let [[creator-addr challenger-addr] (web3-eth/accounts @web3)
        [deposit challenge-period-duration]
        (->> (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration])
             (map bn/number))
        tx-hash (param-change-factory/approve-and-create-param-change
                 {:db (contract-address :meme-registry-db)
                  :key :deposit
                  :value (web3/to-wei 800 :ether)
                  :amount deposit}
                 {:from creator-addr})
        registry-entry (-> (param-change-registry/registry-entry-event-in-tx tx-hash)
                           :args
                           :registry-entry)
        change-entry (param-change/load-param-change registry-entry)]
    
    (testing "Can create param change registry"
      (is tx-hash))
    
    (testing "Properties should be good after creating param change"
      (is (= (:param-change/db change-entry) (contract-address :meme-registry-db)))
      (is (= (:param-change/key change-entry) "deposit")) 
      (is (= (bn/number (:param-change/value-type change-entry)) 0)) 
      (is (= (:param-change/value change-entry) (js/parseInt (web3/to-wei 800 :ether)))))
    
    (testing "Can create challenge under valid condidtions"
      (is (registry-entry/approve-and-create-challenge registry-entry
                                                       {:amount deposit}
                                                       {:from challenger-addr})))))

(deftest apply-param-change-test
  (let [[creator-addr challenger-addr] (web3-eth/accounts @web3)
        [deposit challenge-period-duration]
        (->> (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration])
             (map bn/number))
     
        registry-entry (-> (param-change-factory/approve-and-create-param-change
                            {:db (contract-address :meme-registry-db)
                             :key :deposit
                             :value (web3/to-wei 800 :ether)
                             :amount deposit}
                            {:from creator-addr})
                           param-change-registry/registry-entry-event-in-tx
                           :args
                           :registry-entry)
        other-registry-entry (-> (param-change-factory/approve-and-create-param-change
                                  {:db (contract-address :meme-registry-db)
                                   :key :deposit
                                   :value (web3/to-wei 500 :ether)
                                   :amount deposit}
                                  {:from creator-addr})
                                 param-change-registry/registry-entry-event-in-tx
                                 :args
                                 :registry-entry)
        change-entry (param-change/load-param-change registry-entry)]
    
    (testing "Cannot be applied when not whitelisted"
      ;; it will not be whitelisted because we are still in challenge period
      (is (thrown? js/Error
                   (param-change-registry/apply-param-change registry-entry {:from creator-addr}))))

    (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

    (testing "Param change can be applied under valid conditions"
      (param-change-registry/apply-param-change registry-entry {:from creator-addr}))
    
    (testing "Properties after applying param change should be good"
      (let [[new-deposit-val] (->> (eternal-db/get-uint-values :meme-registry-db [:deposit])
                                   (map bn/number))]
        (is (= new-deposit-val (js/parseInt (web3/to-wei 800 :ether))))))

    (testing "Param change cannot be applied twice"
      (is (thrown? js/Error
                   (param-change-registry/apply-param-change registry-entry {:from creator-addr}))))
    
    (testing "Param change cannot be applied if original value is not current value"
      (is (thrown? js/Error
                   (param-change-registry/apply-param-change other-registry-entry {:from creator-addr}))))

    (testing "Invalid Param change cannot be created"
      (is (thrown? js/Error
                   (param-change-factory/approve-and-create-param-change
                    {:db (contract-address :meme-registry-db)
                     :key :challenge-dispensation
                     :value 101
                     :amount deposit}
                    {:from creator-addr}))))))

