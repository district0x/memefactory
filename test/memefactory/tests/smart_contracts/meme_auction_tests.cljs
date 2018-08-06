(ns memefactory.tests.smart-contracts.meme-auction-tests
  (:require [bignumber.core :as bn]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs.test :refer-macros [deftest is testing use-fixtures async run-tests]]
            [district.server.smart-contracts :refer [contract-call contract-event-in-tx]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-auction :as meme-auction]
            [memefactory.server.contract.meme-auction-factory
             :as
             meme-auction-factory]
            [memefactory.server.contract.meme-factory :as meme-factory]
            [memefactory.server.contract.meme-registry :as meme-registry]
            [memefactory.server.contract.meme-token :as meme-token]
            [memefactory.tests.smart-contracts.meme-tests :refer [create-meme]]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [print.foo :refer [look] :include-macros true]
            [cljs.core.async :as async :refer-macros [go]]))

(use-fixtures 
  :each {:before (test-utils/create-before-fixture {:use-n-account-as-cut-collector 2
                                                    :use-n-account-as-deposit-collector 3
                                                    :meme-auction-cut 10})
         :after test-utils/after-fixture})

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")

;;;;;;;;;;;;;;;;;;;;;;;;
;; MemeAuctionFactory ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(deftest transfer-multi-and-start-auction-test
  (let [[creator-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration max-auction-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit
                                                            :challenge-period-duration :max-auction-duration])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        _ (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
        _ (meme/mint registry-entry max-total-supply {:from creator-addr})
        meme (meme/load-meme registry-entry)
        tx (meme-token/transfer-multi-and-start-auction {:from creator-addr
                                                         :token-ids  (range (:meme/token-id-start meme)
                                                                            (dec (+
                                                                                  (:meme/token-id-start meme)
                                                                                  (:meme/total-minted meme))))
                                                         :start-price (web3/to-wei 0.1 :ether)
                                                         :end-price (web3/to-wei 0.01 :ether)
                                                         :duration max-auction-duration
                                                         :description "Test auction"})]
    (testing "Creates MemeAuction under valid conditions"
      (is tx))

    (testing "Check properties after creating MemeAuction"
      (let [{:keys [:meme-auction] :as x} (:args (meme-auction-factory/meme-auction-event-in-tx tx))
            ;; the auction started by the first event in transaction
            auction (meme-auction/load-meme-auction meme-auction)]
        (is (= (:meme-auction/seller auction) creator-addr)) 
        (is (= (:meme-auction/token-id auction) (:meme/token-id-start meme)))
        (is (= (:meme-auction/start-price auction) (js/parseInt (web3/to-wei 0.1 :ether))))
        (is (= (:meme-auction/end-price auction) (js/parseInt (web3/to-wei 0.01 :ether)))) 
        (is (= (:meme-auction/duration auction) max-auction-duration))

        (testing "Correctly calculates reverse auction price"
          (let [current-price (bn/number (meme-auction/current-price meme-auction))
                correct-price? (or (= current-price 99850000000000000)  ;; if one second passed
                                   (= current-price 99700000000000000)  ;; if two seconds passed
                                   (= current-price 99550000000000000)) ;; if three seconds passed
                ]
            (when-not correct-price? (.log js/console "Auction was bought for " current-price))
            (is correct-price?))))
      
      (testing "onERC721Received fails when called directly, without transferring tokenId"
        (is
         (thrown? js/Error
          (contract-call [:meme-auction-factory :meme-auction-factory-fwd] :on-E-R-C-721-received
                         creator-addr
                         (:meme/token-id-start meme)
                         (meme-auction/start-auction-data {:start-price (web3/to-wei 0.1 :ether)
                                                           :end-price (web3/to-wei 0.01 :ether)
                                                           :duration max-auction-duration})      
                         {:from (last (web3-eth/accounts @web3))})))))
    
    (testing "Fails when passed duration is bigger than :max-duration TCR parameter or shorter than 1 minute"
      (let [transfer-data {:from creator-addr
                           :token-ids (range (:meme/token-id-start meme)
                                             (+
                                              (:meme/token-id-start meme)
                                              (:meme/total-minted meme)))
                           :start-price (web3/to-wei 0.1 :ether)
                           :end-price (web3/to-wei 0.01 :ether)
                           :description "Test auction"}]
        (is (thrown? js/Error (meme-token/transfer-multi-and-start-auction (assoc transfer-data :duration (+ 2 max-auction-duration)) {})))
        (is (thrown? js/Error (meme-token/transfer-multi-and-start-auction (assoc transfer-data :duration 1) {})))))
    

    (testing "fireMemeAuctionEvent cannot be called directly, only by MemeAuction contract"
      (is (thrown? js/Error
                   (contract-call [:meme-auction-factory :meme-auction-factory-fwd]
                                  :fire-meme-auction-event "auction_started"))))))

;;;;;;;;;;;;;;;;;
;; MemeAuction ;;
;;;;;;;;;;;;;;;;;

(deftest meme-auction-buy-test
  ;; cut collector is addr 2, check before fixture creation ath the top
  (let [[creator-addr buyer-addr cut-collector-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration max-auction-duration meme-auction-cut]
        (->> (eternal-db/get-uint-values :meme-registry-db
                                         [:max-total-supply :deposit
                                          :challenge-period-duration :max-auction-duration :meme-auction-cut])
             (map bn/number))
        create-meme-tx (meme-factory/approve-and-create-meme {:meta-hash sample-meta-hash-1
                                                              :total-supply max-total-supply
                                                              :amount deposit}
                                                             {:from creator-addr})
        
        registry-entry (-> create-meme-tx meme-registry/registry-entry-event-in-tx :args :registry-entry)
        _ (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
        _ (meme/mint registry-entry max-total-supply {})
        meme (meme/load-meme registry-entry)
        start-price (web3/to-wei 0.1 :ether)

        transfer-tx (meme-token/transfer-multi-and-start-auction {:from creator-addr 
                                                                  :token-ids [(:meme/token-id-start meme)]
                                                                  :start-price start-price
                                                                  :end-price (web3/to-wei 0.01 :ether)
                                                                  :duration max-auction-duration
                                                                  :description "Test auction"})
        auction-address (-> (meme-auction-factory/meme-auction-event-in-tx transfer-tx)
                            :args :meme-auction)
        auction (meme-auction/load-meme-auction auction-address)]  

    (testing "Meme cannot be bought if not enough funds is sent"
      (is (thrown? js/Error
                   (meme-auction/buy auction-address {:from buyer-addr :value (web3/to-wei 0.0001 :ether)}))))

    (let [cut-collector-init-balance (web3-eth/get-balance @web3 cut-collector-addr)
          creator-init-balance (web3-eth/get-balance @web3 creator-addr)
          buyer-init-balance (web3-eth/get-balance @web3 buyer-addr)
          buy-tx (meme-auction/buy auction-address {:from buyer-addr :value (web3/to-wei 0.2 :ether)})
          [_ current-price _ _] (-> (meme-auction-factory/meme-auction-event-in-tx buy-tx)
                                    :args :data)
          buy-gas (:gas-used (web3-eth/get-transaction-receipt @web3 buy-tx))]
      
      (testing "Buys token collectible under valid conditions"
        (is buy-tx)) 

      (testing "Check properties after buying"
        (let [auctioneer-cut (bn/div-to-int (bn/+ current-price meme-auction-cut) 10000)
              collector-expected-balance (bn/+ cut-collector-init-balance auctioneer-cut)
              creator-expected-balance (bn/+ creator-init-balance (bn/- current-price auctioneer-cut))
              buyer-expected-balance (bn/- buyer-init-balance (bn/+ current-price buy-gas))
              collector-balance-after (web3-eth/get-balance @web3 cut-collector-addr)
              creator-balance-after (web3-eth/get-balance @web3 creator-addr)
              buyer-balance-after (web3-eth/get-balance @web3 buyer-addr)]
          
          (is (= (meme-token/owner-of (:meme/token-id-start meme))
                 buyer-addr))
          (is (= 0 (bn/number (bn/- collector-balance-after collector-expected-balance))))
          (is (= 0 (bn/number (bn/- creator-balance-after creator-expected-balance))))
          (is (= 0 (bn/number (bn/- buyer-balance-after buyer-expected-balance))))))))) 

(deftest meme-auction-cancel-test
  ;; deployer uses first account as cut collector if no account given
  (let [[creator-addr other-addr] (web3-eth/accounts @web3)
        [max-total-supply deposit challenge-period-duration max-auction-duration]
        (->> (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit
                                                            :challenge-period-duration :max-auction-duration])
             (map bn/number))
        registry-entry (create-meme creator-addr deposit max-total-supply sample-meta-hash-1)
        _ (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
        _ (meme/mint registry-entry max-total-supply {})
        meme (meme/load-meme registry-entry)
        start-price (js/parseInt (web3/to-wei 0.1 :ether))
        tx (meme-token/transfer-multi-and-start-auction {:from creator-addr
                                                         :token-ids [(:meme/token-id-start meme)]
                                                         :start-price (web3/to-wei 0.1 :ether)
                                                         :end-price (web3/to-wei 0.01 :ether)
                                                         :duration max-auction-duration
                                                         :description "Test auction"}
                                                        {:from creator-addr})
        auction-address (-> (meme-auction-factory/meme-auction-event-in-tx tx)
                            :args :meme-auction)]

    (testing "Cannot be canceled by anybody except the seller"
      (is (thrown? js/Error (meme-auction/cancel auction-address {:from other-addr}))))

    (testing "Cancels auction under valid conditions"
      (is (meme-auction/cancel auction-address {:from creator-addr})))

    (testing "Seller is owner of the tokenId again"
      (is (= (meme-token/owner-of (:meme/token-id-start meme))
             creator-addr)))))
