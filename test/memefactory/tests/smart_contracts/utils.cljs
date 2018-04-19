(ns memefactory.tests.smart-contracts.utils
  (:require
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district.server.web3 :refer [web3]]
    [memefactory.shared.smart-contracts :refer [smart-contracts]]
    [mount.core :as mount]))


(defn now []
  (from-long (* (:timestamp (web3-eth/get-block @web3 (web3-eth/block-number @web3))) 1000)))

(defn create-before-fixture []
  (fn []
    (-> (mount/with-args
          {:web3 {:port 8549}
           :smart-contracts {:contracts-var #'smart-contracts
                             :auto-mining? true}
           :deployer {:transfer-dank-token-to-accounts 1
                      :initial-registry-params
                      {:meme-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                       :commit-period-duration (t/in-seconds (t/minutes 2))
                                       :reveal-period-duration (t/in-seconds (t/minutes 1))
                                       :deposit (web3/to-wei 1000 :ether)
                                       :challenge-dispensation 50
                                       :vote-quorum 50
                                       :max-total-supply 10
                                       :max-auction-duration (t/in-seconds (t/minutes 10))}
                       :param-change-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                               :commit-period-duration (t/in-seconds (t/minutes 2))
                                               :reveal-period-duration (t/in-seconds (t/minutes 1))
                                               :deposit (web3/to-wei 1000 :ether)
                                               :challenge-dispensation 50
                                               :vote-quorum 50}}}})
      (mount/only [#'district.server.web3
                   #'district.server.smart-contracts/smart-contracts
                   #'memefactory.server.deployer/deployer])
      (mount/start))))


(defn after-fixture []
  (mount/stop)
  (async done (js/setTimeout #(done) 500)))
