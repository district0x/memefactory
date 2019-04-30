(ns memefactory.tests.smart-contracts.registry-tests
  (:require [bignumber.core :as bn]
            [cljs-web3.eth :as web3-eth]
            [cljs.test :as test :refer-macros [deftest is testing use-fixtures]]
            [district.server.smart-contracts :refer [contract-address contract-call contract]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.mutable-forwarder :as mutable-forwarder]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [memefactory.tests.smart-contracts.meme-tests :as meme-tests]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [print.foo :refer [look] :include-macros true]
            [clojure.core.async :as async :refer [<!]]
            [cljs-promises.async :refer-macros [<?]]
            [memefactory.tests.smart-contracts.utils :refer [tx-error?]]))

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
  (test/async
   done
   (async/go
     (testing "set-factory can be called under valid conditions"
       (is (<? (registry/set-factory [:meme-registry :meme-registry-fwd]
                                     {:factory (contract-address :meme-factory)
                                      :factory? true}
                                     {:from (last (web3-eth/accounts @web3))}))))

     (testing "Unauthorised address cannot call this set-factory"
       (is (<? (tx-error? (<? (registry/set-factory [:meme-registry :meme-registry-fwd]
                                                    {:factory (contract-address :meme-factory)}
                                                    {:from (second (web3-eth/accounts @web3))}))))))
     (done))))

(deftest add-registry-entry-test
  (test/async
   done
   (async/go
     (let [[addr0 addr1] (web3-eth/accounts @web3)]
       (testing "addRegistryEntry cannot be called by regular address"
         (is (<? (tx-error? (<? (contract-call [:param-change-registry :param-change-registry-fwd]
                                               :add-registry-entry [addr1]
                                               {:from addr0}))))))
       (done)))))

(deftest set-emergency-test
  (test/async
   done
   (async/go
     (let [[creator-addr challenger-addr] (web3-eth/accounts @web3)
           [max-total-supply deposit] (->> (<? (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit ]))
                                           (map bn/number))
           meme-entry (<! (meme-tests/create-meme creator-addr deposit max-total-supply sample-meta-hash-1))]

       (testing "set-emergency can be called by authorised person"
         (is
          (try
            (<? (contract-call  [:meme-registry :meme-registry-fwd]
                                :set-emergency [true]
                                {:from (first (web3-eth/accounts @web3))}))
            (catch js/Error e nil))))

       (testing "After enabling emergency, check at least 1 method in a RegistryEntry with notEmergency modifier starts failing"
         (is (<? (tx-error? (<? (registry-entry/approve-and-create-challenge meme-entry
                                                                             {:amount deposit
                                                                              :meta-hash sample-meta-hash-1}
                                                                             {:from challenger-addr}))))))

       (testing "Unauthorised address cannot call this method"
         (is (<? (tx-error? (<? (contract-call  :param-change-registry :set-emergency [true]
                                                {:from (last (web3-eth/accounts @web3))}))))))

       ;; Disabling emergency mode!!!!
       (<? (contract-call  [:meme-registry :meme-registry-fwd]
                           :set-emergency [false]
                           {:from (first (web3-eth/accounts @web3))}))
       (done)))))

;;;;;;;;;;;;;;;;;;;;;;
;; MutableForwarder ;;
;;;;;;;;;;;;;;;;;;;;;;

(deftest mutable-forwarder-test
  (test/async
   done
   (async/go
     (testing "setTarget can replace target with new address"
       ;; TODO: should we deploy a diffrent contract, like create another RegistryTest.sol?
       (let [{:keys [:address]} (contract-address :meme-registry)]
         (is (<? (mutable-forwarder/set-target :meme-registry-fwd address {:from (first (web3-eth/accounts @web3))})))))
     (done))))
