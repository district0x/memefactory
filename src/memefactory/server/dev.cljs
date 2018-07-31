(ns memefactory.server.dev
  (:require
   [camel-snake-kebab.core :as cs :include-macros true]
   [memefactory.server.contract.registry-entry :as registry-entry]
   [cljs-time.core :as t]
   [cljs-web3.core :as web3]
   [cljs.nodejs :as nodejs]
   [cljs.pprint :as pprint]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [district.server.config :refer [config]]
   [district.server.db :refer [db]]
   [district.server.endpoints]
   [district.server.graphql :as graphql]
   [district.server.logging :refer [logging]]
   [district.server.middleware.logging :refer [logging-middlewares]]
   [district.server.smart-contracts]
   [district.server.web3 :refer [web3]]
   [district.server.web3-watcher]
   [goog.date.Date]
   [graphql-query.core :refer [graphql-query]]
   #_[memefactory.server.api]
   [memefactory.server.db]
   [memefactory.server.deployer]
   [memefactory.server.emailer]
   [memefactory.server.generator]
   [memefactory.server.graphql-resolvers :refer [resolvers-map]]
   [memefactory.server.syncer]
   [memefactory.server.ipfs]
   [district.graphql-utils :as graphql-utils]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts]
   [mount.core :as mount]
   [district.server.graphql.utils :as utils]
   [print.foo :include-macros true]
   [clojure.pprint :refer [print-table]]
   [district.server.db :as db]
   [clojure.string :as str]
   [memefactory.server.contract.dank-token :as dank-token]
   [memefactory.server.contract.eternal-db :as eternal-db]
   [bignumber.core :as bn]
   [memefactory.server.graphql-resolvers :refer [reg-entry-status
                                                 reg-entry-status-sql-clause
                                                 last-block-timestamp]]))

(nodejs/enable-util-print!)

(def graphql-module (nodejs/require "graphql"))
(def parse-graphql (aget graphql-module "parse"))
(def visit (aget graphql-module "visit"))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

(defn deploy-to-mainnet []
  (mount/stop #'district.server.web3/web3
              #'district.server.smart-contracts/smart-contracts)
  (mount/start-with-args (merge
                           (mount/args)
                           {:web3 {:port 8545}
                            :deployer {:write? true
                                       :gas-price (web3/to-wei 4 :gwei)}})
                         #'district.server.web3/web3
                         #'district.server.smart-contracts/smart-contracts))


(defn redeploy []
  (mount/stop)
  (-> (mount/with-args
        (merge
          (mount/args)
          {:deployer {:write? true}}))
    (mount/start)
    pprint/pprint))

(defn resync []
  (mount/stop #'memefactory.server.db/memefactory-db
              #'memefactory.server.syncer/syncer)
  (-> (mount/start #'memefactory.server.db/memefactory-db
                   #'memefactory.server.syncer/syncer)
      pprint/pprint))

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :graphql {:port 6300
                                      :middlewares [logging-middlewares]
                                      :schema (utils/build-schema graphql-schema
                                                                  resolvers-map
                                                                  {:kw->gql-name graphql-utils/kw->gql-name
                                                                   :gql-name->kw graphql-utils/gql-name->kw})
                                      :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                                      :path "/graphql"
                                      :graphiql true}
                            :web3 {:port 8549}
                            :endpoints {:port 6200}
                            :generator {:memes/use-accounts 1
                                        :memes/items-per-account 1
                                        :memes/scenarios [:scenario/buy]
                                        :param-changes/use-accounts 1
                                        :param-changes/items-per-account 1
                                        :param-changes/scenarios [:scenario/apply-param-change]}
                            :deployer {:transfer-dank-token-to-accounts 1
                                       :initial-registry-params
                                       {:meme-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                                        :commit-period-duration (t/in-seconds (t/minutes 2))
                                                        :reveal-period-duration (t/in-seconds (t/minutes 1))
                                                        :deposit (web3/to-wei 1000 :ether)
                                                        :challenge-dispensation 50
                                                        :vote-quorum 50
                                                        :max-total-supply 10
                                                        :max-auction-duration (t/in-seconds (t/weeks 20))}
                                        :param-change-registry {:challenge-period-duration (t/in-seconds (t/minutes 10))
                                                                :commit-period-duration (t/in-seconds (t/minutes 2))
                                                                :reveal-period-duration (t/in-seconds (t/minutes 1))
                                                                :deposit (web3/to-wei 1000 :ether)
                                                                :challenge-dispensation 50
                                                                :vote-quorum 50}}}
                            :ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0" :gateway "http://127.0.0.1:8080/ipfs"}}}
         :smart-contracts {:contracts-var #'memefactory.shared.smart-contracts/smart-contracts
                           :print-gas-usage? true
                           :auto-mining? true}})
    (mount/except [#'memefactory.server.deployer/deployer
                   #'memefactory.server.generator/generator])
    (mount/start)
    pprint/pprint))

(set! *main-cli-fn* -main)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Some useful repl tools ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn increase-time [seconds]
  (web3-evm/increase-time! @web3 [seconds])
  (web3-evm/mine! @web3))

(defn select
  "Usage: (select [:*] :from [:memes])"
  [& [select-fields & r]]
  (-> (db/all (->> (partition 2 r)
                   (map vec)
                   (into {:select select-fields})))
      (print-table)))

(defn print-db
  "(print-db) prints all db tables to the repl
   (print-db :users) prints only users table"
  ([] (print-db nil))
  ([table]
   (let [all-tables (if table
                      [(name table)]
                      (->> (db/all {:select [:name] :from [:sqlite-master] :where [:= :type "table"]})
                           (map :name)))]
     (doseq [t all-tables]
       (println "#######" (str/upper-case t) "#######")
       (select [:*] :from [(keyword t)])
       (println "\n\n")))))

(defn print-statuses []
  (->> (db/all {:select [:v.* :re.*]
                :from [[:reg-entries :re]]
                :left-join [[:votes :v] [:= :re.reg-entry/address :v.reg-entry/address]]})
       (group-by :reg-entry/address)
       (map (fn [[address [r :as votes]]]
              {:address address
               :server-status (name (reg-entry-status (last-block-timestamp) r))
               :query-status (-> (db/get {:select [[(reg-entry-status-sql-clause (last-block-timestamp)) :status]] 
                                          :from  [[:reg-entries :re]]
                                          :where [:= :re.reg-entry/address address]})
                                 :status
                                 graphql-utils/gql-name->kw 
                                 name)
               :blockhain-status (name (:reg-entry/status (registry-entry/load-registry-entry address)))
               :v+ (:challenge/votes-for r)
               :v- (:challenge/votes-against r)
               :v? (count (filter #(and (pos? (:vote/amount %))
                                        (or (zero? (:vote/revealed-on %))
                                            (nil? (:vote/revealed-on %)))) votes))})) 
       print-table))

(defn increase-time-to-next-period [re-address]
  (let [now (last-block-timestamp)
        entry (db/get {:select [:*]
                       :from [:reg-entries]
                       :where [:= :reg-entry/address re-address]})
        current-status (reg-entry-status now entry)
        time-to-next (case current-status
                       :reg-entry.status/challenge-period (- (:reg-entry/challenge-period-end entry) now)
                       :reg-entry.status/commit-period    (- (:challenge/commit-period-end entry) now)
                       :reg-entry.status/reveal-period    (- (:challenge/reveal-period-end entry) now)
                       :reg-entry.status/whitelisted      (println "Not moving for whitelisted")
                       :reg-entry.status/blacklisted      (println "Not moving for blacklisted"))]
    (println "Increasing time by " time-to-next)
    (increase-time time-to-next)))

(defn print-balances []
  (->> (web3-eth/accounts @web3)
       (map (fn [account]
              {:account account
               :dank (dank-token/balance-of account)
               :eth (web3-eth/get-balance @web3 account)}))
       print-table))

(defn print-params []
  (let [param-keys [:max-total-supply :deposit :challenge-period-duration
                    :commit-period-duration :reveal-period-duration :max-auction-duration
                    :vote-quorum :challenge-dispensation]]
    (->> (eternal-db/get-uint-values :meme-registry-db param-keys)
         (map bn/number)
         (zipmap param-keys)
         (pprint/pprint))))

