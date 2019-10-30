(ns memefactory.tests.smart-contracts.meme-auction-tests
  (:require [bignumber.core :as bn]
            [cljs-promises.async :refer-macros [<?]]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs.test :as test :refer-macros [deftest is testing use-fixtures async run-tests]]
            [clojure.core.async :as async :refer [<!]]
            [district.server.smart-contracts :refer [contract-call contract-event-in-tx]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-auction :as meme-auction]
            [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
            [memefactory.server.contract.meme-factory :as meme-factory]
            [memefactory.server.contract.meme-registry :as meme-registry]
            [memefactory.server.contract.meme-token :as meme-token]
            [memefactory.tests.smart-contracts.meme-tests :refer [create-meme]]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [memefactory.tests.smart-contracts.utils :refer [tx-reverted?]]))

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")
(def gas-price 2e10)

;;;;;;;;;;;;;;;;;;;;;;;;
;; MemeAuctionFactory ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(deftest transfer-multi-and-start-auction-test
  (test/async
   done
   (async/go
     (let [[creator-addr non-creator-addr] (web3-eth/accounts @web3)
           [max-total-supply deposit challenge-period-duration max-auction-duration]
           (->> (<? (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit
                                                                   :challenge-period-duration :max-auction-duration]))
                (map bn/number))
           registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
           _ (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
           _ (<? (meme/mint registry-entry max-total-supply {:from creator-addr}))
           meme (<? (meme/load-meme registry-entry))
           tx (fn [from]
                (meme-token/transfer-multi-and-start-auction {:from from
                                                              :token-ids (range (:meme/token-id-start meme)
                                                                                (dec (+
                                                                                      (:meme/token-id-start meme)
                                                                                      (:meme/total-minted meme))))
                                                              :start-price (web3/to-wei 0.1 :ether)
                                                              :end-price (web3/to-wei 0.01 :ether)
                                                              :duration max-auction-duration
                                                              :description "Test auction"}))]

       (testing "Only creator can start auction"
          (is (<? (tx-reverted? (<? (tx non-creator-addr))))))

       (testing "Check properties after creating MemeAuction"
         (let [{:keys [:meme-auction] :as x} (:args (meme-auction-factory/meme-auction-started-event-in-tx (<? (tx creator-addr))))
               ;; the auction started by the first event in transaction
               auction (<? (meme-auction/load-meme-auction meme-auction))]
           (is (= (:meme-auction/seller auction) creator-addr))
           (is (= (:meme-auction/token-id auction) (:meme/token-id-start meme)))
           (is (= (:meme-auction/start-price auction) (js/parseInt (web3/to-wei 0.1 :ether))))
           (is (= (:meme-auction/end-price auction) (js/parseInt (web3/to-wei 0.01 :ether))))
           (is (= (:meme-auction/duration auction) max-auction-duration))

           (testing "Correctly calculates reverse auction price"
             (let [current-price (bn/number (<? (meme-auction/current-price meme-auction)))

                   correct-price? (or (= current-price 100000000000000000)
                                      (= current-price 99999925619834700) ;; if 1 second passed
                                      (= current-price 99999851239669420) ;; if 2 seconds passed
                                      (= current-price 99999776859504130) ;; if 3 seconds passed
                                      )]
               (when-not correct-price? (.log js/console "Auction was bought for" current-price))
               (is correct-price?))))

         (testing "onERC721Received fails when called directly, without transferring tokenId"
           (is
            (<? (tx-reverted? (<? (contract-call [:meme-auction-factory :meme-auction-factory-fwd] :on-E-R-C-721-received
                                                 [creator-addr
                                                  (:meme/token-id-start meme)
                                                  (meme-auction/start-auction-data {:start-price (web3/to-wei 0.1 :ether)
                                                                                    :end-price (web3/to-wei 0.01 :ether)
                                                                                    :duration max-auction-duration
                                                                                    :description "Test"})]
                                                 {:from (last (web3-eth/accounts @web3))})))))

           #_(is
            (tx-reverted? #(contract-call [:meme-auction-factory :meme-auction-factory-fwd] :on-E-R-C-721-received
                                          [creator-addr
                                           (:meme/token-id-start meme)
                                           (meme-auction/start-auction-data {:start-price (web3/to-wei 0.1 :ether)
                                                                             :end-price (web3/to-wei 0.01 :ether)
                                                                             :duration max-auction-duration
                                                                             :description "Test"})]
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
           (is (tx-reverted? #(meme-token/transfer-multi-and-start-auction (assoc transfer-data :duration (+ 2 max-auction-duration)) {})))
           (is (tx-reverted? #(meme-token/transfer-multi-and-start-auction (assoc transfer-data :duration 1) {})))))

       (testing "fireMemeAuctionEvent cannot be called directly, only by MemeAuction contract"
         (is (<? (tx-reverted? (<? (contract-call [:meme-auction-factory :meme-auction-factory-fwd]
                                                  :fire-meme-auction-started-event
                                                  [1 creator-addr 2 1 600 "" 1]))))))
       (done)))))

;;;;;;;;;;;;;;;;;
;; MemeAuction ;;
;;;;;;;;;;;;;;;;;

(deftest meme-auction-buy-test
  ;; cut collector is addr 2, check before fixture creation ath the top
  (test/async
   done
   (async/go
     (let [[creator-addr buyer-addr cut-collector-addr] (web3-eth/accounts @web3)
           [max-total-supply deposit challenge-period-duration max-auction-duration meme-auction-cut]
           (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                [:max-total-supply :deposit
                                                 :challenge-period-duration :max-auction-duration :meme-auction-cut]))
                (map bn/number))
           registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
           _ (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
           _ (<? (meme/mint registry-entry max-total-supply {}))
           meme (<? (meme/load-meme registry-entry))
           start-price (web3/to-wei 0.1 :ether)

           transfer-tx (<? (meme-token/transfer-multi-and-start-auction {:from creator-addr
                                                                         :token-ids [(:meme/token-id-start meme)]
                                                                         :start-price start-price
                                                                         :end-price (web3/to-wei 0.01 :ether)
                                                                         :duration max-auction-duration
                                                                         :description "Test auction"}))
           auction-address (-> (meme-auction-factory/meme-auction-started-event-in-tx transfer-tx)
                               :args :meme-auction)
           auction (<? (meme-auction/load-meme-auction auction-address))]

       (testing "Meme cannot be bought if not enough funds is sent"
         (is (<? (tx-reverted? (<? (meme-auction/buy auction-address {:from buyer-addr :value (web3/to-wei 0.0001 :ether)}))))))

       (let [cut-collector-init-balance (web3-eth/get-balance @web3 cut-collector-addr)
             creator-init-balance (web3-eth/get-balance @web3 creator-addr)
             buyer-init-balance (web3-eth/get-balance @web3 buyer-addr)
             buy-tx (<? (meme-auction/buy auction-address {:from buyer-addr :value (web3/to-wei 0.2 :ether)}))
             current-price (-> (meme-auction-factory/meme-auction-buy-event-in-tx buy-tx)
                               :args :price)
             buy-gas (-> (:gas-used (web3-eth/get-transaction-receipt @web3 buy-tx)) (bn/* gas-price))]
         (testing "Buys token collectible under valid conditions"
           (is buy-tx))

         (testing "Check properties after buying"
           (let [auctioneer-cut (/ (bn/number (bn/* current-price meme-auction-cut)) 10000)
                 collector-expected-balance (bn/+ cut-collector-init-balance auctioneer-cut)
                 creator-expected-balance (bn/+ creator-init-balance (bn/- current-price auctioneer-cut))
                 collector-balance-after (web3-eth/get-balance @web3 cut-collector-addr)
                 creator-balance-after (web3-eth/get-balance @web3 creator-addr)
                 buyer-expected-balance (bn/- buyer-init-balance (bn/+ current-price buy-gas))
                 buyer-balance-after (web3-eth/get-balance @web3 buyer-addr)]
             (is (= (<? (meme-token/owner-of (:meme/token-id-start meme)))
                    buyer-addr))
             (is (= 0 (bn/number (bn/- collector-balance-after collector-expected-balance))))
             (is (= 0 (bn/number (bn/- creator-balance-after creator-expected-balance))))
             (is (= 0 (bn/number (bn/- buyer-balance-after buyer-expected-balance)))))))
       (done)))))

(deftest meme-auction-cancel-test
  (test/async
   done
   (async/go
     ;; deployer uses first account as cut collector if no account given

     (let [[creator-addr other-addr] (web3-eth/accounts @web3)
           [max-total-supply deposit challenge-period-duration max-auction-duration]
           (->> (<? (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit
                                                                   :challenge-period-duration :max-auction-duration]))
                (map bn/number))
           registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
           _ (web3-evm/increase-time! @web3 [(inc challenge-period-duration)])
           _ (<? (meme/mint registry-entry max-total-supply {}))
           meme (<? (meme/load-meme registry-entry))
           start-price (js/parseInt (web3/to-wei 0.1 :ether))
           tx (<? (meme-token/transfer-multi-and-start-auction {:from creator-addr
                                                                :token-ids [(:meme/token-id-start meme)]
                                                                :start-price (web3/to-wei 0.1 :ether)
                                                                :end-price (web3/to-wei 0.01 :ether)
                                                                :duration max-auction-duration
                                                                :description "Test auction"}
                                                               {:from creator-addr}))
           auction-address (-> (meme-auction-factory/meme-auction-started-event-in-tx tx)
                               :args :meme-auction)]

       (testing "Cannot be canceled by anybody except the seller"
         (is (<? (tx-reverted? (<? (meme-auction/cancel auction-address {:from other-addr}))))))

       (testing "Cancels auction under valid conditions"
         (is (<? (meme-auction/cancel auction-address {:from creator-addr}))))

       (testing "Seller is owner of the tokenId again"
         (is (= (<? (meme-token/owner-of (:meme/token-id-start meme)))
                creator-addr)))
       (done)))))
