(ns memefactory.tests.smart-contracts.registry-tests
  (:require [bignumber.core :as bn]
            [cljs-web3.eth :as web3-eth]
            [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [district.server.smart-contracts :refer [contract-address contract-call contract]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.mutable-forwarder :as mutable-forwarder]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [memefactory.tests.smart-contracts.meme-tests :as meme-tests]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [print.foo :refer [look] :include-macros true]))

#_(use-fixtures
  :once {:before (test-utils/create-before-fixture {:use-n-account-as-cut-collector 2
                                                    :use-n-account-as-deposit-collector 3
                                                    :meme-auction-cut 10})
         :after test-utils/after-fixture})

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")

;;;;;;;;;;;;;;
;; Registry ;;
;;;;;;;;;;;;;;

(deftest set-factory-test
  (testing "set-factory can be called under valid conditions"
   (is (registry/set-factory [:meme-registry :meme-registry-fwd]
                             {:factory (contract-address :meme-factory)
                              :factory? true}
                             {:from (last (web3-eth/accounts @web3))})))

  (testing "Unauthorised address cannot call this set-factory"
    (is (thrown? js/Error
                 (registry/set-factory [:meme-registry :meme-registry-fwd]
                                       {:factory (contract-address :meme-factory)}
                                       {:from (first (web3-eth/accounts @web3))})))))

(deftest add-registry-entry-test
  (let [[addr0 addr1] (web3-eth/accounts @web3)]
   (testing "addRegistryEntry cannot be called by regular address"
     (is (thrown? js/Error
                  (contract-call [:param-change-registry :param-change-registry-fwd]
                                 :add-registry-entry addr1
                                 {:from addr0}))))))

(deftest set-emergency-test
  (let [[creator-addr challenger-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit] (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit ])
                                        (map bn/number))
        meme-entry (meme-tests/create-meme creator-addr deposit max-total-supply sample-meta-hash-1)]

    (testing "set-emergency can be called by authorised person"
      (is
       (try
         (contract-call  [:meme-registry :meme-registry-fwd]
                         :set-emergency true
                         {:from (last (web3-eth/accounts @web3))})
         (catch js/Error e nil))))

   (testing "After enabling emergency, check at least 1 method in a RegistryEntry with notEmergency modifier starts failing"
     #_(is (thrown? js/Error
                  (do
                   (registry-entry/approve-and-create-challenge meme-entry
                                                                {:amount deposit
                                                                 :meta-hash sample-meta-hash-1}
                                                                {:from challenger-addr})
                   ;; TODO Fix test
                   #_(registry-entry/load-registry-entry meme-entry)))))

   (testing "Unauthorised address cannot call this method"
     (is (thrown? js/Error
                  (contract-call  :param-change-registry :set-emergency true
                                  {:from (first (web3-eth/accounts @web3))}))))

   ;; Disabling emergency mode!!!!
   (contract-call  [:meme-registry :meme-registry-fwd]
                   :set-emergency false
                   {:from (last (web3-eth/accounts @web3))})))

;;;;;;;;;;;;;;;;;;;;;;
;; MutableForwarder ;;
;;;;;;;;;;;;;;;;;;;;;;

(deftest mutable-forwarder-test
  (testing "setTarget can replace target with new address and contract should still work"
    ;; TODO: should we deploy a diffrent contract, like create another RegistryTest.sol?
    (let [{:keys [:address]} (contract-address :meme-registry)]
      (mutable-forwarder/set-target :meme-registry-fwd address {:from (last (web3-eth/accounts @web3))})
      (let [[creator-addr] (web3-eth/accounts @web3)
            [max-total-supply deposit] (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit])
                                            (map bn/number))
            registry-entry (meme-tests/create-meme creator-addr deposit max-total-supply sample-meta-hash-1)]
        (is registry-entry)))))
