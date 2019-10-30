(ns memefactory.tests.smart-contracts.utils
  (:require [cljs-time.coerce :refer [to-epoch from-long]]
            [cljs-time.core :as t]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs.test :as test]
            [clojure.core.async :as async :refer [<!]]
            [district.shared.async-helpers :refer [safe-go <?]]
            [district.server.smart-contracts :refer [wait-for-tx-receipt]]
            [district.server.web3 :refer [web3]]
            [memefactory.shared.smart-contracts-dev :refer [smart-contracts]]
            [mount.core :as mount])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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
  (test/async done (js/setTimeout #(done) 1000)))

(defn tx-reverted? [transaction]
  (go
    (let [out-ch (async/chan)]
      (try
        (<? (transaction))
        (async/put! out-ch false)
        (catch :default e
          (async/put! out-ch true)))
      out-ch)))
