(ns district.server.smart-contracts
  (:require [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.utils :refer [js->cljkk camel-case]]
            [cljs.core.async :refer [<! >! timeout]]
            [cljs.core.match :refer-macros [match]]
            [cljs.nodejs :as nodejs]
            [cljs.pprint]
            [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [district.server.config :refer [config]]
            [district.server.web3 :refer [web3]]
            [mount.core :as mount :refer [defstate]])
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]))

(def fs (nodejs/require "fs"))
(def process (nodejs/require "process"))

(declare start)
(declare wait-for-tx-receipt)

(defstate smart-contracts :start (start (merge (:smart-contracts @config)
                                               (:smart-contracts (mount/args)))))

(defn contract [contract-key]
  (get @(:contracts @smart-contracts) contract-key))

(defn contract-address [contract-key]
  (:address (contract contract-key)))

(defn contract-name [contract-key]
  (:name (contract contract-key)))

(defn contract-abi [contract-key]
  (:abi (contract contract-key)))

(defn contract-bin [contract-key]
  (:bin (contract contract-key)))

(declare contract-call)

(defn instance
  ([contract-key]
   (:instance (contract contract-key)))
  ([contract-key contract-key-or-addr]
   (web3-eth/contract-at @web3 (contract-abi contract-key) (if (keyword? contract-key-or-addr)
                                                             (contract-address contract-key-or-addr)
                                                             contract-key-or-addr))))

(defn update-contract! [contract-key contract]
  (swap! (:contracts @smart-contracts) update contract-key merge contract))


(defn- fetch-contract
  "Given a file-name and a path tries to load abi and bytecode.
  It first try to load it from a json truffle artifact, if it doesn't find it
  tries .abi .bin files for the name.
  Returns a map with :abi and :bin keys."
  [file-name & [{:keys [:path]}]]
  (let [path (or path (str (.cwd process) "/resources/public/contracts/build/"))
        json-file-path (str path file-name ".json")
        abi-file-path (str path file-name ".abi")
        bin-file-path (str path file-name ".bin")]
    (if (.existsSync fs json-file-path)

      (let [content-str (.readFileSync fs json-file-path "utf-8")
            json-file-content (js/JSON.parse content-str)]

        {:abi (aget json-file-content "abi")
         :bin (aget json-file-content "bytecode")})
      {:abi (when (.existsSync fs abi-file-path) (js/JSON.parse (.readFileSync fs abi-file-path "utf-8")))
       :bin (when (.existsSync fs bin-file-path) (.readFileSync fs bin-file-path "utf-8"))})))

(defn load-contract-files [contract {:keys [:contracts-build-path]}]
  (let [{:keys [abi bin]} (fetch-contract (:name contract) {:path contracts-build-path})]
    (merge contract
           {:abi abi
            :bin bin
            :instance (web3-eth/contract-at @web3 abi (:address contract))})))


(defn start [{:keys [contracts-build-path :contracts-var print-gas-usage?] :as opts}]
  (merge
    {:contracts (atom (into {} (map (fn [[k v]]
                                      [k (load-contract-files v opts)])
                                    @contracts-var)))}
    opts))


(defn link-library [bin placeholder library-address]
  (string/replace bin placeholder (subs library-address 2)))


(defn link-contract-libraries [smart-contracts bin library-placeholders]
  (reduce (fn [bin [placeholder replacement]]
            (let [address (if (keyword? replacement)
                            (get-in smart-contracts [replacement :address])
                            replacement)]
              (link-library bin placeholder address)))
          bin library-placeholders))

(defn- handle-deployed-contract! [contract-key {:keys [:abi :bin] :as contract} tx-hash]
  (let [{:keys [:gas-used :block-number :contract-address]} (web3-eth/get-transaction-receipt @web3 tx-hash)
        contract (merge contract {:instance (web3-eth/contract-at @web3 abi contract-address)
                                  :address contract-address})]
    (when (and gas-used block-number)
      (update-contract! contract-key contract)
      (when (:print-gas-usage? @smart-contracts)
        (println (:name contract) contract-address (.toLocaleString gas-used)))
      contract)))

(defn deploy-smart-contract!
  "# arguments:
     * `contract-key` keyword e.g. :some-contract
   ## `args` is a vector of arguments for the constructor
   ## `opts` is a map of options:
    * `placeholder-replacements` : a map containing replacements for library placeholders
    * `from` : address deploying the conract
    * `:gas` : gas limit for the contract creation transaction
   # returns:
   function returns a Promise"
  ([contract-key args {:keys [:placeholder-replacements :from :gas] :as opts}]
   (let [{:keys [:abi :bin] :as contract} (load-contract-files (contract contract-key) @smart-contracts)
         opts (merge {:data (str "0x" (link-contract-libraries @(:contracts @smart-contracts) bin placeholder-replacements))}
                     (when-not from
                       {:from (first (web3-eth/accounts @web3))})
                     (when-not gas
                       {:gas 4000000})
                     opts)]
     (-> (js/Promise.resolve
          (-> (apply web3-eth/contract-new @web3 abi
                     (merge args (select-keys opts [:from :to :gas-price :gas
                                                    :value :data :nonce
                                                    :condition])))
              (aget "transactionHash")))
         (.then #(wait-for-tx-receipt %))
         (.then (fn [receipt]
                  (handle-deployed-contract! contract-key contract (:transaction-hash receipt)))))))

  ([contract-key args]
   (deploy-smart-contract! contract-key args {:from (first (web3-eth/accounts @web3))
                                              :gas 4000000})))

(defn write-smart-contracts! []
  (let [{:keys [:ns :file :name]} (meta (:contracts-var @smart-contracts))]
    (.writeFileSync fs file
                    (str "(ns " ns ") \n\n"
                         "(def " name " \n"
                         (as-> @(:contracts @smart-contracts) $
                               (map (fn [[k v]]
                                      [k (dissoc v :instance :abi :bin)]) $)
                               (into {} $)
                               ;; cljs.pprint/write won't compile with simple optimisations
                               ;; therefore must be required only in dev environment
                               (cljs.pprint/write $ :stream nil))
                         ")"))))


(defn instance-from-arg [contract]
  (cond
    (keyword? contract) (instance contract)
    (sequential? contract) (instance (first contract) (second contract))
    :else contract))

;; TODO : add alts for definite timeout (or # of retries)
(defn- wait-for-tx-receipt*
  "callback is a nodejs style callback i.e. (fn [error data] ...)"
  [tx-hash callback]
  (web3-eth/get-transaction-receipt @web3 tx-hash (fn [error receipt]
                                                    (if error
                                                      (callback error nil)
                                                      (go
                                                        (if receipt
                                                          (callback nil receipt)
                                                          (do
                                                            ;; try again in 1K millis
                                                            (<! (timeout 1000))
                                                            (wait-for-tx-receipt* tx-hash callback))))))))

(defn wait-for-tx-receipt
  "blocks until transaction `tx-hash` gets sent to the network."
  [tx-hash]
  (js/Promise. (fn [resolve reject]
                 (wait-for-tx-receipt* tx-hash (fn [error tx-receipt]
                                                 (if error
                                                   (reject error)
                                                   (resolve tx-receipt)))))))

(defn contract-call
  "# arguments:
   ## `contract` parameter can be one of:
   * keyword :some-contract
   * tuple of keyword and address [:some-contract 0x1234...]
   * instance SomeContract
   ## `method` is a :camel_case keyword corresponding to the smart-contract function
   ## `args` is a vector of arguments for the `method`
   ## `opts` is a map of options passed as message data
   # returns:
   function returns a Promise"
  ([contract method args {:keys [:from :gas] :as opts}]

   ;; (prn "@@@ contract-call" {:a args :o opts})
   
   (let [opts (merge (when-not from
                       {:from (first (web3-eth/accounts @web3))})
                     (when-not gas
                       {:gas 4000000})
                     opts)]
     (js/Promise. (fn [resolve reject]
                    (apply web3-eth/contract-call (instance-from-arg contract) method
                           (merge args
                                  opts
                                  (fn [err data]
                                    (if err
                                      (reject err)
                                      (resolve data)))))))))

  ([contract method args]
   (contract-call contract method args {:from (first (web3-eth/accounts @web3))}))

  ([contract method]
   (contract-call contract method [] {:from (first (web3-eth/accounts @web3))})))

;; TODO : docstring
;; https://github.com/ethereum/wiki/wiki/JavaScript-API#contract-events
(defn create-event-filter
  [contract event filter-opts opts on-event]

  ;; (prn "@@@ create-filter" {:f-o filter-opts :o opts :oe on-event})
  
  (apply web3-eth/contract-call (instance-from-arg contract) event
         [filter-opts
          opts
          on-event]))

(defn contract-event-in-tx [tx-hash contract event-name & args]
  (let [instance (instance-from-arg contract)
        event-filter (apply web3-eth/contract-call instance event-name args)
        formatter (aget event-filter "formatter")
        contract-addr (aget instance "address")
        {:keys [:logs]} (web3-eth/get-transaction-receipt @web3 tx-hash)
        signature (aget event-filter "options" "topics" 0)]
    (reduce (fn [result log]
              (when (= signature (first (:topics log)))
                (let [{:keys [:address] :as evt} (js->clj (formatter (clj->js log)) :keywordize-keys true)]
                  (when (= contract-addr address)
                    (reduced (js->cljkk evt))))))
            nil
            logs)))

(defn contract-events-in-tx [tx-hash contract event-name & args]
  (let [instance (instance-from-arg contract)
        event-filter (apply web3-eth/contract-call instance event-name args)
        formatter (aget event-filter "formatter")
        contract-addr (aget instance "address")
        {:keys [:logs]} (web3-eth/get-transaction-receipt @web3 tx-hash)
        signature (aget event-filter "options" "topics" 0)]
    (reduce (fn [result log]
              (when (= signature (first (:topics log)))
                (let [{:keys [:address] :as evt} (js->clj (formatter (clj->js log)) :keywordize-keys true)]
                  (when (= contract-addr address)
                    (concat result [(js->cljkk evt)])))))
            nil
            logs)))


(defn replay-past-events [event-filter callback & [{:keys [:delay :transform-fn]
                                                    :or {delay 0 transform-fn identity}}]]

  ;; (prn "@@REPLAY" {:f event-filter :c callback})
  
  (let [stopped? (atom false)]
    (.get event-filter (fn [err all-logs]

                         ;; (prn "ALL LOGS" all-logs)
                         
                         (when err
                           (throw (js/Error. err)))
                         (let [all-logs (transform-fn (js->cljkk all-logs))]
                           (go-loop [logs all-logs]
                                    (when (and (not @stopped?)
                                               (seq logs))
                                      (<! (timeout delay))
                                      (callback nil (first logs))
                                      (recur (rest logs)))))))

    (aset event-filter "stopWatching" #(reset! stopped? true)) ;; So we can detect stopWatching was called
    event-filter))
