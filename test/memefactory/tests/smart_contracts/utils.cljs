(ns memefactory.tests.smart-contracts.utils
  (:require
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district.server.web3 :refer [web3]]
    [memefactory.shared.smart-contracts :refer [smart-contracts]]
    [district.server.smart-contracts :refer [instance]]
    [memefactory.server.deployer]
    [mount.core :as mount]
    [cljs.core.async :as async :refer-macros [go]]))

(defn now []
  (from-long (* (:timestamp (web3-eth/get-block @web3 (web3-eth/block-number @web3))) 1000)))

(defn create-before-fixture
  ([] (create-before-fixture {}))
  ([deployer-opts]
   (fn []
     (let [args {:web3 {:port 8549}
                 :smart-contracts {:contracts-var #'smart-contracts
                                   :auto-mining? true}
                 :deployer (merge
                            {:transfer-dank-token-to-accounts 5
                             :initial-registry-params
                             {:meme-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                              :commit-period-duration (t/in-seconds (t/minutes 2))
                                              :reveal-period-duration (t/in-seconds (t/minutes 1))
                                              :deposit (web3/to-wei 10 :ether)
                                              :challenge-dispensation 50
                                              :vote-quorum 50
                                              :max-total-supply 10
                                              :max-auction-duration (t/in-seconds (t/minutes 10))
                                              :meme-auction-cut 10}
                              :param-change-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                                      :commit-period-duration (t/in-seconds (t/minutes 2))
                                                      :reveal-period-duration (t/in-seconds (t/minutes 1))
                                                      :deposit (web3/to-wei 10 :ether)
                                                      :challenge-dispensation 50
                                                      :vote-quorum 50}}}
                            deployer-opts)}]
       (-> (mount/with-args args)
           (mount/only [#'district.server.web3/web3
                        #'district.server.smart-contracts/smart-contracts
                        #'memefactory.server.deployer/deployer])
           (mount/start))))))


(defn after-fixture []
  (mount/stop)
  (async done (js/setTimeout #(done) 1000)))
