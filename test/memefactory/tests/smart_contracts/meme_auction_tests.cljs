(ns memefactory.tests.smart-contracts.meme-auction-tests
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.evm :as web3-evm]
            [cljs-web3-next.helpers :as web3-helpers]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.core.async :refer [go <!]]
            [cljs.test :as test :refer-macros [deftest is testing async]]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [district.shared.async-helpers :refer [<? promise->]]
            [memefactory.server.contract.eternal-db :as eternal-db]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-auction :as meme-auction]
            [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
            [memefactory.server.contract.meme-token :as meme-token]
            [memefactory.tests.smart-contracts.meme-tests :refer [create-meme]]
            [memefactory.tests.smart-contracts.utils :refer [tx-reverted?]]
            [taoensso.timbre :as log :refer [spy]]))

(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")
(def gas-price 2e10)

;;;;;;;;;;;;;;;;;;;;;;;;
;; MemeAuctionFactory ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-balance [address]
  (js-invoke (aget @web3 "eth") "getBalance" address))

(deftest transfer-multi-and-start-auction-test
  (async done
         (go
           (let [[creator-addr non-creator-addr] (<! (web3-eth/accounts @web3))
                 [max-total-supply deposit challenge-period-duration max-auction-duration]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit
                                                                         :challenge-period-duration :max-auction-duration]))
                      (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (<! (web3-evm/increase-time @web3 (inc challenge-period-duration)))
                 _ (<? (meme/mint registry-entry max-total-supply {:from creator-addr}))
                 meme (<? (meme/load-meme registry-entry))
                 token-ids (range (:meme/token-id-start meme)
                                  (dec (+ (:meme/token-id-start meme)
                                          (:meme/total-minted meme))))
                 tx (fn [from]
                      (meme-token/transfer-multi-and-start-auction {:from from
                                                                    :token-ids token-ids
                                                                    :start-price (web3-utils/to-wei @web3 0.1 :ether)
                                                                    :end-price (web3-utils/to-wei @web3 0.01 :ether)
                                                                    :duration max-auction-duration
                                                                    :description "Test auction"}
                                                                   {:from from}))]

             (testing "Only creator can start auction"
               (is (tx-reverted? (<? (tx non-creator-addr)))))

             (testing "Check properties after creating MemeAuction"
               (let [event-emitter (smart-contracts/subscribe-events :meme-auction-factory-fwd
                                                                     :MemeAuctionStartedEvent
                                                                     {:from-block (<! (web3-eth/get-block-number @web3))
                                                                      :to-block "latest"}
                                                                     [(fn [_ {{:keys [:meme-auction :seller :token-id :start-price :end-price]} :args :as tx}]

                                                                        (is (contains? (set token-ids) (js/parseInt token-id)))

                                                                        (is (= seller creator-addr))

                                                                        (is (= start-price (web3-utils/to-wei @web3 0.1 :ether)))

                                                                        (is (= end-price (web3-utils/to-wei @web3 0.01 :ether)))

                                                                        (promise-> (meme-auction/current-price meme-auction)
                                                                                   (fn [current-price]
                                                                                     (is (= current-price "100000000000000000")))))])]
                 (<! (tx creator-addr))
                 (web3-eth/unsubscribe event-emitter)))

             (testing "onERC721Received fails when called directly, without transferring tokenId"
               (is
                (tx-reverted? (<? (smart-contracts/contract-send [:meme-auction-factory :meme-auction-factory-fwd] :on-E-R-C-721-received
                                                                 [creator-addr
                                                                  (:meme/token-id-start meme)
                                                                  (meme-auction/start-auction-data {:start-price (web3-utils/to-wei @web3 0.1 :ether)
                                                                                                    :end-price (web3-utils/to-wei @web3 0.01 :ether)
                                                                                                    :duration max-auction-duration
                                                                                                    :description "Test"})]
                                                                 {:from (last (<! (web3-eth/accounts @web3)))})))))

             (testing "Fails when passed duration is bigger than :max-duration TCR parameter or shorter than 1 minute"
               (let [transfer-data {:from creator-addr
                                    :token-ids (range (:meme/token-id-start meme)
                                                      (+
                                                       (:meme/token-id-start meme)
                                                       (:meme/total-minted meme)))
                                    :start-price (web3-utils/to-wei @web3 0.1 :ether)
                                    :end-price (web3-utils/to-wei @web3 0.01 :ether)
                                    :description "Test auction"}]
                 (is (tx-reverted? (<! (meme-token/transfer-multi-and-start-auction (assoc transfer-data :duration (+ 2 max-auction-duration)) {}))))
                 (is (tx-reverted? (<! (meme-token/transfer-multi-and-start-auction (assoc transfer-data :duration 1) {}))))))

             (testing "fireMemeAuctionEvent cannot be called directly, only by MemeAuction contract"
               (is (tx-reverted? (<? (smart-contracts/contract-send [:meme-auction-factory :meme-auction-factory-fwd]
                                                                    :fire-meme-auction-started-event
                                                                    [1 creator-addr 2 1 600 "" 1])))))
             (done)))))

;;;;;;;;;;;;;;;;;
;; MemeAuction ;;
;;;;;;;;;;;;;;;;;

(deftest meme-auction-buy-test
  ;; cut collector is addr 2, check before fixture creation ath the top
  (async done
         (go
           (let [[creator-addr buyer-addr cut-collector-addr] (<! (web3-eth/accounts @web3))
                 [max-total-supply deposit challenge-period-duration max-auction-duration meme-auction-cut]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db
                                                      [:max-total-supply :deposit
                                                       :challenge-period-duration :max-auction-duration :meme-auction-cut]))
                      (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (web3-evm/increase-time @web3 (inc challenge-period-duration))
                 _ (<? (meme/mint registry-entry max-total-supply {}))
                 {:keys [:meme/token-id-start] :as meme} (<? (meme/load-meme registry-entry))
                 start-price (web3-utils/to-wei @web3 0.1 :ether)
                 _ (<? (meme-token/transfer-multi-and-start-auction {:from creator-addr
                                                                     :token-ids [(:meme/token-id-start meme)]
                                                                     :start-price start-price
                                                                     :end-price (web3-utils/to-wei @web3 0.01 :ether)
                                                                     :duration max-auction-duration
                                                                     :description "Test auction"}))
                 creator-init-balance (bn/number (<! (get-balance creator-addr)))
                 buyer-init-balance (bn/number (<! (get-balance buyer-addr)))
                 cut-collector-init-balance (bn/number (<! (get-balance cut-collector-addr)))
                 block-number  (<! (web3-eth/get-block-number @web3))
                 event-emitter (smart-contracts/subscribe-events :meme-auction-factory-fwd
                                                                 :MemeAuctionStartedEvent
                                                                 {:from-block block-number
                                                                  :to-block  (inc block-number)}
                                                                 [(fn [_ {{:keys [:meme-auction :seller :token-id :start-price :end-price]} :args :as tx}]
                                                                    (promise->
                                                                     ;; Meme cannot be bought if not enough funds is sent
                                                                     (meme-auction/buy meme-auction {:from buyer-addr :value (web3-utils/to-wei @web3 0.0001 :ether)})
                                                                     #(is tx-reverted? %)

                                                                     ;; Buys token collectible under valid conditions
                                                                     #(meme-auction/buy meme-auction {:from buyer-addr :value (web3-utils/to-wei @web3 0.2 :ether)})
                                                                     (fn [buy-tx]
                                                                       (is buy-tx)
                                                                       (js/Promise.all [(get-balance creator-addr)
                                                                                        (get-balance buyer-addr)
                                                                                        (get-balance cut-collector-addr)
                                                                                        (js/Promise.resolve buy-tx)
                                                                                        (meme-auction-factory/meme-auction-buy-event-in-tx buy-tx)]))
                                                                     (fn [[creator-balance-after
                                                                           buyer-balance-after
                                                                           collector-balance-after
                                                                           {:keys [:cumulative-gas-used]}
                                                                           {:keys [:price :seller-proceeds :auctioneer-cut] :as resp}]]
                                                                       (let [creator-expected-balance (+ (bn/number creator-init-balance) (- (bn/number price) (bn/number auctioneer-cut)))
                                                                             buyer-expected-balance (- buyer-init-balance (+ (bn/number price) cumulative-gas-used))
                                                                             collector-expected-balance (+ (bn/number cut-collector-init-balance) (bn/number auctioneer-cut))]
                                                                         (is (bn/= 0 (bn/- creator-expected-balance creator-balance-after)))
                                                                         (is (bn/= 0 (bn/- (bn/+ creator-init-balance (bn/number seller-proceeds)) (bn/number creator-balance-after))))
                                                                         (is (bn/= 0 (bn/- collector-expected-balance collector-balance-after)))))
                                                                     #(meme-token/owner-of token-id-start)
                                                                     (fn [owner]
                                                                       (is (= buyer-addr owner)))
                                                                     #(done)))])]
             (web3-eth/unsubscribe event-emitter)))))

(deftest meme-auction-cancel-test
  (async done
         (go
           ;; deployer uses first account as cut collector if no account given
           (let [[creator-addr other-addr] (<! (web3-eth/accounts @web3))
                 [max-total-supply deposit challenge-period-duration max-auction-duration]
                 (->> (<? (eternal-db/get-uint-values :meme-registry-db [:max-total-supply :deposit
                                                                         :challenge-period-duration :max-auction-duration]))
                      (map bn/number))
                 registry-entry (<! (create-meme creator-addr deposit max-total-supply sample-meta-hash-1))
                 _ (web3-evm/increase-time @web3 (inc challenge-period-duration))
                 _ (<? (meme/mint registry-entry max-total-supply {}))
                 meme (<? (meme/load-meme registry-entry))
                 start-price (js/parseInt (web3-utils/to-wei @web3 0.1 :ether))
                 tx (<? (meme-token/transfer-multi-and-start-auction {:from creator-addr
                                                                      :token-ids [(:meme/token-id-start meme)]
                                                                      :start-price (web3-utils/to-wei @web3 0.1 :ether)
                                                                      :end-price (web3-utils/to-wei @web3 0.01 :ether)
                                                                      :duration max-auction-duration
                                                                      :description "Test auction"}
                                                                     {:from creator-addr}))
                 block-number  (<! (web3-eth/get-block-number @web3))
                 event-emitter (smart-contracts/subscribe-events :meme-auction-factory-fwd
                                                                 :MemeAuctionStartedEvent
                                                                 {:from-block block-number
                                                                  :to-block (inc block-number)}
                                                                 [(fn [_ {{:keys [:meme-auction]} :args}]
                                                                    (promise-> ;; Cannot be canceled by anybody except the seller
                                                                     (meme-auction/cancel meme-auction {:from other-addr})
                                                                     (fn [tx]
                                                                       (is tx-reverted? tx))

                                                                     ;; Cancels auction under valid conditions
                                                                     #(meme-auction/cancel meme-auction {:from creator-addr})
                                                                     (fn [tx]
                                                                       (is tx))

                                                                     ;; Seller is owner of the tokenId again
                                                                     #(meme-token/owner-of (:meme/token-id-start meme))
                                                                     (fn [owner]
                                                                       (is (= creator-addr owner)))
                                                                     #(done)))])]
             (web3-eth/unsubscribe event-emitter)))))
