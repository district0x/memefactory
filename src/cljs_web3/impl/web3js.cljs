(ns web3.impl.web3js
  (:require [cljs-web3.api :as api :refer [Web3Api]]
            [cljs-web3.helpers :as web3-helpers]
            [cljs-web3.macros :refer [defrecord+]]
            [cljs.nodejs :as nodejs]))

(def Web3 (nodejs/require "web3"))

(defrecord+ Web3Js []
  Web3Api
  (-http-provider [_ uri]
    (new Web3 (new (aget Web3 "providers" "HttpProvider") uri)))
  (-websocket-provider [_ uri]
    (new Web3 (new (aget Web3 "providers" "WebsocketProvider") uri)))
  (-connection-url [_ provider]
    (aget provider "currentProvider" "connection" "_url"))
  (-extend [_ provider property methods]
    {:instance _
     :provider (js-invoke provider "extend" (web3-helpers/cljkk->js {:property property
                                                                     :methods methods}))})
  (-is-listening? [_ provider & [callback]]
    (apply js-invoke (aget provider "eth" "net") "isListening" (remove nil? [callback])))
  (-connected? [_ provider]
    (aget provider "currentProvider"  "connected"))
  (-disconnect [_ provider]
    (js-invoke (aget provider "currentProvider") "disconnect"))
  (-on-connect [_ provider & [callback]]
    (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "connect" callback])))
  (-on-disconnect [_ provider & [callback]]
    (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "end" callback])))
  (-on-error [_ provider & [callback]]
    (apply js-invoke (aget provider "currentProvider") (remove nil? ["on" "error" callback])))
  (-address? [_ provider address]
    (js-invoke (aget provider "utils") "isAddress" address))
  (-sha3 [_ provider arg]
    (js-invoke (aget provider "utils") "sha3" arg))
  (-solidity-sha3 [_ provider [arg & [args]]]
    (apply js-invoke (aget provider "utils") "soliditySha3" arg args))
  (-from-ascii [_ provider arg]
    (js-invoke (aget provider "utils") "fromAscii" arg))
  (-to-ascii [_ provider arg]
    (js-invoke (aget provider "utils") "toAscii" arg))
  (-from-wei [_ provider number & [unit]]
    (js-invoke (aget provider "utils") "fromWei" (str number) (name unit)))
  (-to-wei [_ provider number & [unit]]
    (js-invoke (aget provider "utils") "toWei" (str number) (name unit)))
  (-number-to-hex [_ provider arg]
    (js-invoke (aget provider "utils") "numberToHex" arg))
  (-contract-at [_ provider abi address]
    (new (aget provider "eth" "Contract") abi address))
  (-get-transaction-receipt [_ provider tx-hash]
    (js-invoke (aget provider "eth") "getTransactionReceipt" tx-hash))
  (-accounts [_ provider]
    (js-invoke (aget provider "eth") "getAccounts"))
  (-get-block-number [_ provider & [callback]]
    (apply js-invoke (aget provider "eth") "getBlockNumber" (remove nil? [callback])))
  (-get-block [_ provider block-hash-or-number return-transactions? & [callback]]
    (apply js-invoke (aget provider "eth") "getBlock" (remove nil? [block-hash-or-number return-transactions? callback])))
  (-encode-abi [_ contract-instance method args]
    (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "encodeABI" ))
  (-contract-call [_ contract-instance method args opts]
    (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "call" (clj->js opts)))
  (-contract-send [_ contract-instance method args opts]
    (js-invoke (apply js-invoke (aget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "send" (clj->js opts)))
  (-subscribe-events [_ contract-instance event opts & [callback]]
    (apply js-invoke (aget contract-instance "events") (web3-helpers/camel-case (name event)) (remove nil? [(web3-helpers/cljkk->js opts) callback])))
  (-subscribe-logs [_ provider contract-instance opts & [callback]]
    (js-invoke (aget provider "eth") "subscribe" "logs" (web3-helpers/cljkk->js opts) callback))
  (-decode-log [_ provider abi data topics]
    (js-invoke (aget provider "eth" "abi") "decodeLog" (clj->js abi) data (clj->js topics)))
  (-on [_ event-emitter evt callback]
    (js-invoke event-emitter "on" (name evt) callback))
  (-unsubscribe [_ subscription & [callback]]
    (js-invoke subscription "unsubscribe" callback))
  (-clear-subscriptions [_ provider]
    (js-invoke (aget provider "eth") "clearSubscriptions"))
  (-get-past-events [this contract-instance event opts & [callback]]
    (js-invoke contract-instance "getPastEvents" (web3-helpers/camel-case (name event)) (web3-helpers/cljkk->js opts) callback))
  (-increase-time [this provider seconds]
    (js-invoke (aget provider "evm") "increaseTime" seconds))
  (-mine-block [this provider]
    (js-invoke (aget provider "evm") "mineBlock")))

(defn new []
  (->Web3Js))
