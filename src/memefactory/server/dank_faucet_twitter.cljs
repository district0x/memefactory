(ns memefactory.server.dank-faucet-twitter
  (:require [cljs.core.async :as async :refer [<! go]]
            [cljs.nodejs :as nodejs]
            [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.helpers :refer [zero-address]]
            [cljs-web3-next.utils :as web3-utils]
            [clojure.string :as string]
            [district.server.config :as config]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [district.shared.async-helpers :refer [<? safe-go]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.shared.utils :refer [tweet-url-regex]]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

;; Launch a service to receive and parse tweets URLs to call the DANK faucet with the tweet account and address
;; obtained from the tweet.
;; It uses the twitter API to gather the tweet info and uses OpenZeppelin Defender Relay to invoke and manage
;; the call to the Faucet contract.
;; The twitter accounts used for gathering the DANK allotment are (hashed and) stored in the contract to ensure users
;; can only receive a single allotment. Additional checking are done before calling the contract to
;; i) informing properly the user in case of an error
;; ii) avoid launching transactions which are likely to fail
;; Note that the Facuet contract is not call as soon as a request is received, but the requests are added to a queue,
;; which is periodically check to emit pending requests.
;; To make this work:
;; 1) Ensure the Faucet contract has enough DANK to serve the requests
;; 2) Ensure the Defender Relay account associated to this service has enough MATIC to call the Faucet contract

(def express (nodejs/require "express"))
(def cors (nodejs/require "cors"))
(def Twitter (nodejs/require "twitter"))
(def Relayer (.-Relayer (nodejs/require "defender-relay-client")))

(def tweet-body-regex #"(?:.|\n)*I'm verifying my address \[?(0x[a-fA-F0-9]{40})]? and claiming my free \$DANK tokens on @memefactory0x so I can vote and challenge memes(?:.|\n)*")
(def hashed-account-data-regex #"0x22d58044([a-fA-F0-9]{64})[a-fA-F0-9]+") ; sha3("sendDank(bytes32,address)"): 0x22d58044
(def account-min-age (* 90 24 60 60 1000)) ; 90 days in milliseconds

(defonce relayer (atom nil))
(defonce requests-queue (atom #queue []))
(defonce hash-nonce (atom ""))
(defonce in-progress? (atom false))


(defn get-tweet-id [tweet-url]
  (log/info (str "Parsing tweet url: " tweet-url))
  (when tweet-url (last (re-matches tweet-url-regex tweet-url))))

(defn fetch-tweet [twitter-obj tweet-id]
  (safe-go
    (let [tweet (js->clj (<? (-> (.get twitter-obj
                                       (str "/statuses/show.json?id=" tweet-id)
                                       {})
                                 (.catch (fn[e] (log/error "Failed to request Twitter API" {:error e}))))
                             ) :keywordize-keys true)]
      (if tweet
        (let [address (last (re-matches tweet-body-regex (:text tweet)))]
          {:valid? (not (string/blank? address))
           :address address
           :tweet-id tweet-id
           :tweet-account-id (get-in tweet [:user :id])
           :tweet-account-created (js/Date.parse (get-in tweet [:user :created_at]))})
        {:valid? false}))))


(defn send-response [res body]
  (.send res (js/JSON.stringify (clj->js body))))

(defn enough-dank? [num-requests]
  (safe-go
    (let [balance (<? (dank-token/balance-of (smart-contracts/contract-address :dank-faucet)))
          dank-allotment (<? (smart-contracts/contract-call :dank-faucet :allotment))]
      (> balance (* num-requests dank-allotment)))))

(defn send-error [res msg]
  (log/info "Failed to process Faucet request" {:message msg})
  (send-response res {:status "error" :message msg}))

(defn tweet-valid? [tweet-info]
  (:valid? tweet-info))

(defn account-old-enough? [tweet-info]
  (> (- (.now js/Date) account-min-age) (:tweet-account-created tweet-info)))

(defn hash-account-id [tweet-account-id]
  (web3-utils/solidity-sha3 @web3 (str tweet-account-id @hash-nonce)))

(defn account-in-queue? [tweet-account-id]
  (some #(= (:tweet-account-id %) tweet-account-id) @requests-queue))

(defn get-account-from-tx [tx]
  (let [data (:data tx)]
    (last (re-matches hashed-account-data-regex data))))

(defn account-in-relayer? [tweet-account-id]
  (safe-go
    (let [hash-tweet-account (hash-account-id tweet-account-id)
          pending-txs (<? (.list @relayer {:status "pending"}))]
      (some #(= (get-account-from-tx (js->clj % :keywordize-keys true)) hash-tweet-account) pending-txs)
      )))

(defn account-in-chain? [tweet-account-id]
  (safe-go
    (let [hash-tweet-account (hash-account-id tweet-account-id)]
      (> (js/BigInt (<? (smart-contracts/contract-call :dank-faucet :allocated-dank [hash-tweet-account])))
         0 ))))

(defn account-already-used? [{:keys [:tweet-account-id]}]
  (safe-go
    (or (account-in-queue? tweet-account-id) (<? (account-in-relayer? tweet-account-id)) (<? (account-in-chain? tweet-account-id)))))

(defn add-to-queue [tweet-info]
  (log/info "Faucet request added to queue" tweet-info)
  (swap! requests-queue conj (select-keys tweet-info [:tweet-account-id :address])))


(defn build-tx-data [hashed-account-id address]
  (web3-eth/encode-abi (smart-contracts/instance :dank-faucet) :send-dank [hashed-account-id address]))

(defn submit-send-dank-tx [hashed-account-id address tx-speed]
  (log/info (str "Submitting Dank Faucet requests: " hashed-account-id " " address))
  (-> (.sendTransaction @relayer #js {:to (smart-contracts/contract-address :dank-faucet)
                       :data (build-tx-data hashed-account-id address)
                       :speed tx-speed
                       :gasLimit "200000"
                       })
      (.then (fn[m] (log/info "Submitted tx to relayer" {:info m})))
      (.catch (fn[e] (log/error "Failed to submit tx service" {:error e})))
      ))

(defn send-dank [{:keys [:tweet-account-id :address] :as item} tx-speed]
  (safe-go
    (log/info (str "Processing Dank Faucet requests: " tweet-account-id " " address))
    (when (and (<? (enough-dank? 1)) (not (<? (account-already-used? item))))
      (<? (submit-send-dank-tx (hash-account-id tweet-account-id) address tx-speed)))))


(defn count-pending-requests []
  (count @requests-queue))

(defn address-valid? [tweet-info]
  (let [address (:address tweet-info)]
    (and (not= zero-address address)
         (not= (smart-contracts/contract-address :dank-faucet) address))))

;; process the HTTP requests: Parse the tweet URL, check if tweet is properly formatted and tweet account is not
;; used yet (among other things) and add the request to the queue (or send an error if didn't go through)
(defn process-request [twitter-obj]
  (fn [req res]
    (safe-go
      (try
        (if (not (<? (enough-dank? (inc (count-pending-requests)))))
          (send-error res "no dank left in faucet")
          (let [tweet-url (aget req "body" "tweet-url")
                tweet-id (get-tweet-id tweet-url)]
            (if (string/blank? tweet-id)
              (send-error res "tweet url not valid")
              (let [tweet-info (<? (fetch-tweet twitter-obj tweet-id))]
                (cond
                  (not (tweet-valid? tweet-info)) (send-error res "tweet format not valid")
                  (not (address-valid? tweet-info)) (send-error res "address not valid")
                  (not (account-old-enough? tweet-info)) (send-error res "twitter account not old enough")
                  (<? (account-already-used? tweet-info)) (send-error res "twitter account already used")
                  :else (do
                          (add-to-queue tweet-info)
                          (send-response res {:status "success"
                                              :twitter-account (:tweet-account-id tweet-info)
                                              :address (:address tweet-info)})))))))
        (catch :default e
          (log/error "Failed to process request" {:error e})
          (send-error res "failed to process request"))))))


(defn start-server [port path twitter-obj]
  (let [app (express)]
    (.use app (cors))
    (.use app (.json express))
    (.post app path (process-request twitter-obj))
    (.listen app port)))


;; Starts the daemon which periodically checks the pending requests and launch the transactions accordingly
(defn start-queue-processor [interval tx-speed]
  (js/setInterval
    (fn []
      (when (compare-and-set! in-progress? false true)
        (try
          (safe-go
            (log/debug (str "Checking requests queue. Items: " (count @requests-queue)))
            (while (seq @requests-queue)
              (let [item (peek @requests-queue)]
                (swap! requests-queue pop)
                (<? (send-dank item tx-speed)))))
          (finally (reset! in-progress? false)))))
    interval))

(defn start [{:keys [:port :path :consumer-key :consumer-secret :bearer-token :send-interval :account-hash-nonce
                     :relay-api-key :relay-secret-key :tx-speed] :or {tx-speed "average"} :as opts}]
  (let [twitter-obj (Twitter. #js {:consumer_key consumer-key
                                   :consumer_secret consumer-secret
                                   :bearer_token bearer-token})
        relayer-obj (Relayer. #js {:apiKey relay-api-key,
                                   :apiSecret relay-secret-key})
        server (start-server port path twitter-obj)
        queue-timeout (start-queue-processor send-interval tx-speed)]
    (reset! relayer relayer-obj)
    (reset! hash-nonce account-hash-nonce)
  {:server server
   :queue-timeout queue-timeout}))

(defn stop [faucet]
  (js/clearInterval (:queue-timeout @faucet))
  (.close (:server @faucet)))

(defstate dank-faucet-twitter
          :start (start (merge (:dank-faucet-twitter @config/config)
                               (:dank-faucet-twitter (mount/args))))
          :stop (stop dank-faucet-twitter))
