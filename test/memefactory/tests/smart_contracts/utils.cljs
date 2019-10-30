(ns memefactory.tests.smart-contracts.utils
  (:require [cljs-time.coerce :refer [to-epoch from-long]]
            [cljs-time.core :as t]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
            ;; [clojure.core.async :refer [<!]]
            [cljs-promises.async :refer-macros [<?]]
            [district.server.smart-contracts :refer [wait-for-tx-receipt]]
            [district.server.web3 :refer [web3]]
            [memefactory.shared.smart-contracts-dev :refer [smart-contracts]]
            [mount.core :as mount]))

(defn now []
  (from-long (* (:timestamp (web3-eth/get-block @web3 (web3-eth/block-number @web3))) 1000)))

(defn create-before-fixture []
  (fn []
    (let [args {:web3 {:port 8545}
                :smart-contracts {:contracts-var #'smart-contracts
                                  :auto-mining? true}
                :time-source :blockchain
                :ranks-cache {:ttl (t/in-millis (t/minutes 60))}}]
      (-> (mount/with-args args)
          (mount/only [#'district.server.web3/web3
                       #'district.server.smart-contracts/smart-contracts])
          (mount/start)))))

(defn after-fixture []
  (mount/stop)
  (async done (js/setTimeout #(done) 1000)))

(defn tx-reverted? [transaction]
  (try
    (<! (transaction))
    (catch :default e
      true)))
