(ns district.server.smart-contracts
  (:require
   [cljs-web3.core :as web3-core]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.utils :as web3-utils]
   [district.shared.async-helpers :refer [promise->]]
   [taoensso.timbre :as log]
   [clojure.set :as clojure-set]
   [cljs.core.async :refer [<! timeout] :as async]
   [cljs.core.match :refer-macros [match]]
   [cljs.nodejs :as nodejs]
   ;; [cljs.pprint]
   [taoensso.timbre :as log]
   [clojure.string :as string]
   [district.shared.async-helpers :as async-helpers]
   [district.server.config :refer [config]]
   [district.server.web3 :refer [web3]]
   [mount.core :as mount :refer [defstate]]
   [cljs.core.async.impl.protocols])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def fs (nodejs/require "fs"))
(def process (nodejs/require "process"))

(declare start)

(defstate smart-contracts :start (start (merge (:smart-contracts @config)
                                               (:smart-contracts (mount/args))))
  :stop ::stopped)

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

(defn instance
  ([contract-key]
   (let [contr (contract contract-key)]
     (if-not (:forwards-to contr)
       (:instance contr)
       (instance (:forwards-to contr) contract-key))))
  ([contract-key contract-key-or-addr]
   (web3-eth/contract-at @web3 (contract-abi contract-key) (if (keyword? contract-key-or-addr)
                                                             (contract-address contract-key-or-addr)
                                                             contract-key-or-addr))))

(defn contract-by-address [contract-address]
  (reduce-kv (fn [_ contract-key {:keys [:address] :as contract}]
               (when (= (string/lower-case contract-address) (string/lower-case address))
                 (reduced (assoc contract :contract-key contract-key))))
             nil
             @(:contracts @smart-contracts)))

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

    (when-not abi
      (println "Couldn't find ABI for " (:name contract)))

    (when-not bin
      (println "Couldn't find bin for " (:name contract)))

    (merge contract
           {:abi abi
            :bin bin
            :instance (web3-eth/contract-at @web3 abi (:address contract))})))

(defn instance-from-arg [contract & [{:keys [:ignore-forward?]}]]
  (cond
    (and ignore-forward? (keyword? contract)) (instance contract contract)
    (keyword? contract) (instance contract)
    (sequential? contract) (instance (first contract) (second contract))
    :else contract))

(defn- enrich-event-log [contract-name contract-instance {:keys [:event :return-values] :as log}]
  (-> log
      (update :return-values #(web3-utils/return-values->clj return-values (web3-utils/event-interface contract-instance event)))
      (update :event (fn [event-name]
                       (if (= (first event-name)
                              (string/upper-case (first event-name)))
                         (keyword event-name)
                         (web3-utils/kebab-case (keyword event-name)))))
      (assoc :contract (dissoc (contract-by-address (:address log))
                               :abi :bin :instance))
      (clojure-set/rename-keys {:return-values :args})))

(defn contract-call
  "Will call a method and execute its smart contract method in the EVM without sending any transaction.
   # arguments:
   ## `contract` parameter can be one of:
   * keyword :some-contract
   * tuple of keyword and address [:some-contract 0x1234...]
   * instance SomeContract
   ## `method` is a :camel_case keyword corresponding to the smart-contract function
   ## `args` is a vector of arguments for the `method`
   ## `opts` is a map of options passed as message data
   # returns:
   function returns a Promise resolving to the result of `method` call."
  ([contract method args {:keys [:ignore-forward?] :as opts}]
   (web3-eth/contract-call @web3
                           (instance-from-arg contract {:ignore-forward? ignore-forward?})
                           method
                           args
                           (dissoc opts :ignore-forward?)))
  ([contract method args]
   (contract-call contract method args {}))
  ([contract method]
   (contract-call contract method [] {})))

(defn contract-send
  "Will send a transaction to the smart contract and execute its method.
   # arguments:
   ## `contract` parameter can be one of:
   * keyword :some-contract
   * tuple of keyword and address [:some-contract 0x1234...]
   * instance SomeContract
   ## `method` is a :camel_case keyword corresponding to the smart-contract function
   ## `args` is a vector of arguments for the `method`
   ## `opts` is a map of options passed as message data
   # returns:
   function returns a Promise resolving to a tx receipt."
  ([contract method args {:keys [:from :gas :ignore-forward?] :as opts}]
   (promise-> (if from
                (js/Promise.resolve [from])
                (web3-eth/accounts @web3))
              (fn [accounts]
                (let [opts (merge {:from (first accounts)}
                                  (when-not gas
                                    {:gas 4000000})
                                  (dissoc opts :ignore-forward?))]
                  (-> (web3-eth/contract-send @web3
                                              (instance-from-arg contract {:ignore-forward? ignore-forward?})
                                              method
                                              args
                                              opts))))
              #(web3-utils/js->cljkk %)))
  ([contract method args]
   (contract-send contract method args {}))
  ([contract method]
   (contract-send contract method [] {})))

(defn subscribe-events [contract event {:keys [:from-block :address :topics :ignore-forward? :latest-event?] :as opts} & [callback]]
  (let [contract-instance (instance-from-arg contract {:ignore-forward? ignore-forward?})]
    (web3-eth/subscribe-events @web3
                               contract-instance
                               event
                               opts
                               (fn [error evt]
                                 (if callback
                                   (callback error (->> evt
                                                        web3-utils/js->cljkk
                                                        (#(assoc % :latest-event? latest-event?))
                                                        (enrich-event-log contract contract-instance)))
                                   (log/warn (str "No callback specified for event " evt)))))))

(defn subscribe-event-logs [contract event {:keys [:from-block :address :topics :ignore-forward?] :as opts} & [callback]]
  (let [contract-instance (instance-from-arg contract {:ignore-forward? ignore-forward?})
        event-signature (:signature (web3-utils/event-interface contract-instance event))]
    (web3-eth/subscribe-logs @web3
                             contract-instance
                             (merge {:address (aget contract-instance "options" "address")
                                     :topics [event-signature]}
                                    opts)
                             (fn [error event]
                               (callback error (web3-utils/js->cljkk event))))))

(defn contract-event-in-tx [contract event {:keys [:transaction-hash] :as tx-receipt}]
  (let [contract-instance (instance-from-arg contract)
        {:keys [:signature] :as event-interface} (web3-utils/event-interface contract-instance event)]
    (promise-> (web3-eth/get-transaction-receipt @web3 transaction-hash)
               (fn [tx-receipt]
                 (let [{:keys [:logs :inputs]} (web3-utils/js->cljkk tx-receipt)]
                   (reduce (fn [result {:keys [:topics :data] :as log}]
                             (when (= signature (first topics))
                               (let [return-values (web3-eth/decode-log @web3 (:inputs event-interface) data topics)]
                                 (reduced (web3-utils/return-values->clj return-values event-interface)))))
                           nil
                           logs))))))

(defn replay-past-events-in-order [events callback {:keys [:from-block :to-block
                                                           :ignore-forward? :delay
                                                           :transform-fn :on-finish]
                                                    :or {delay 0 transform-fn identity}
                                                    :as opts}]
  (let [logs-chans (for [[k [contract event]] events]
                     (let [logs-ch (async/promise-chan)
                           contract-instance (instance-from-arg contract {:ignore-forward? ignore-forward?})]
                       (web3-eth/get-past-events @web3
                                                 contract-instance
                                                 event
                                                 opts
                                                 (fn [error events]
                                                   (let [logs (->> events
                                                                   web3-utils/js->cljkk
                                                                   (map (partial enrich-event-log contract contract-instance)))]
                                                     (async/put! logs-ch {:err error :logs logs}))))
                       logs-ch))]

    ;; go chan by chan collecting events
    (go-loop [all-logs []
              [logs-ch & rest-logs] logs-chans]
      (if logs-ch
        (let [{:keys [:err :logs]} (async/<! logs-ch)
              logs (map #(assoc % :err err) logs)]
          ;; keep collecting
          (recur (into all-logs logs) rest-logs))

        ;; no more channels to read, sort and callback
        (let [sorted-logs (transform-fn (sort-by (juxt :block-number :transaction-index :log-index) all-logs))]
          (go-loop [logs sorted-logs]
            (if (seq logs)
              (do
                (when (pos? delay)
                  (<! (timeout delay)))
                (let [first-log (first logs)]

                  (when (fn? callback)
                    (doseq [res (callback (:err first-log) (dissoc first-log :err))]
                      ;; if callback returns a promise or chan we block until it resolves
                      (cond
                        (satisfies? cljs.core.async.impl.protocols/ReadPort res)
                        (<! res)

                        (async-helpers/promise? res)
                        (<! (async-helpers/promise->chan res))))))
                (recur (rest logs)))

              (when (fn? on-finish)
                (on-finish sorted-logs)))))))))

(defn start [{:keys [:contracts-var] :as opts}]
  (merge
   {:contracts (atom (into {} (map (fn [[k v]]
                                     [k (load-contract-files v opts)])
                                   @contracts-var)))}
   opts))
