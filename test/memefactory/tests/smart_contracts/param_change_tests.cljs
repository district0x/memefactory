(ns memefactory.tests.smart-contracts.param-change-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.test :as test :refer-macros [deftest is testing async]]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.param-change :as param-change]
            [memefactory.server.contract.param-change-factory :as param-change-factory]
            [memefactory.server.contract.registry :as registry]
            [cljs.core.async :refer [go <!]]
            [district.shared.async-helpers :refer [<?]]
            [memefactory.tests.smart-contracts.utils :refer [tx-reverted?]]))

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")

;;;;;;;;;;;;;;;;;
;; ParamChange ;;
;;;;;;;;;;;;;;;;;

(deftest approve-and-create-param-change-test
  (async done
         (go
           (let [[creator-addr challenger-addr] (<! (web3-eth/accounts @web3))
                 [deposit challenge-period-duration] (->> (<? (eternal-db/get-uint-values :param-change-registry-db
                                                                                          [:deposit :challenge-period-duration]))
                                                          (map bn/number))
                 tx-hash (<! (param-change-factory/approve-and-create-param-change
                              {:db (smart-contracts/contract-address :meme-registry-db)
                               :key (name :deposit)
                               :value (web3-utils/to-wei @web3 2 :ether)
                               :amount deposit
                               :meta-hash "QmfWFPzyZRMV7w5NvXfSWeZtEAvsNKYZwuvwpgHD82NR28"}
                              {:from creator-addr}))
                 #_#_registry-entry (->> (registry/param-change-constructed-event-in-tx [:param-change-registry :param-change-registry-fwd] tx-hash)
                                         :args
                                         :registry-entry)
                 #_#_change-entry (<? (param-change/load-param-change registry-entry))
                 ]

             (prn tx-hash)

             (testing "Can create param change registry"
               (is (not (tx-reverted? tx-hash))))

             #_(testing "Properties should be good after creating param change"
                 (is (= (:param-change/db change-entry) (contract-address :meme-registry-db)))
                 (is (= (:param-change/key change-entry) "deposit"))
                 (is (= (:param-change/value change-entry) (js/parseInt (web3/to-wei 2 :ether)))))

             #_(testing "Can create challenge under valid condidtions"
                 (is (<? (registry-entry/approve-and-create-challenge registry-entry
                                                                      {:amount deposit}
                                                                      {:from challenger-addr}))))
             (done))
           )))

#_(deftest apply-param-change-test
    (test/async
     done
     (async/go
       (try
         (let [[creator-addr challenger-addr] (web3-eth/accounts @web3)
               [deposit challenge-period-duration]
               (->> (<? (eternal-db/get-uint-values :param-change-registry-db [:deposit :challenge-period-duration]))
                    (map bn/number))
               registry-entry-tx-hash (<? (param-change-factory/approve-and-create-param-change
                                           {:db (contract-address :meme-registry-db)
                                            :key :deposit
                                            :value (web3/to-wei 800 :ether)
                                            :amount (str deposit)}
                                           {:from creator-addr}))
               registry-entry (->> registry-entry-tx-hash
                                   (registry/param-change-constructed-event-in-tx [:param-change-registry :param-change-registry-fwd])
                                   :args
                                   :registry-entry)
               other-registry-entry-tx-hash (<? (param-change-factory/approve-and-create-param-change
                                                 {:db (contract-address :meme-registry-db)
                                                  :key :deposit
                                                  :value (web3/to-wei 500 :ether)
                                                  :amount deposit}
                                                 {:from creator-addr}))
               other-registry-entry (->> other-registry-entry-tx-hash
                                         (registry/param-change-constructed-event-in-tx [:param-change-registry :param-change-registry-fwd])
                                         :args
                                         :registry-entry)
               change-entry (<? (param-change/load-param-change registry-entry))]

           (testing "Cannot be applied when not whitelisted"
             ;; it will not be whitelisted because we are still in challenge period
             (is (tx-reverted? #(param-change-registry/apply-param-change registry-entry {:from creator-addr}))))

           (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])

           (testing "Param change can be applied under valid conditions"
             (<? (param-change-registry/apply-param-change registry-entry {:from creator-addr})))

           (testing "Properties after applying param change should be good"
             (let [[new-deposit-val] (->> (<? (eternal-db/get-uint-values :meme-registry-db [:deposit]))
                                          (map bn/number))]
               (is (= new-deposit-val (js/parseInt (web3/to-wei 800 :ether))))))

           (testing "Param change cannot be applied twice"
             (is (tx-reverted? #(param-change-registry/apply-param-change registry-entry {:from creator-addr}))))

           (testing "Param change cannot be applied if original value is not current value"
             (is (tx-reverted? #(param-change-registry/apply-param-change other-registry-entry {:from creator-addr}))))

           (testing "Invalid Param change cannot be created"
             (is (tx-reverted? #(param-change-factory/approve-and-create-param-change
                                 {:db (contract-address :meme-registry-db)
                                  :key :challenge-dispensation
                                  :value 101
                                  :amount deposit}
                                 {:from creator-addr}))))
           (done))
         (catch js/Error e (js/console.error e))))))
