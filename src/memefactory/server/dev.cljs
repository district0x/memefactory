(ns memefactory.server.dev
  (:require
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-time.core :as t]
   [cljs-web3.core :as web3]
   [cljs.nodejs :as nodejs]
   [cljs.pprint :as pprint]
   [district.server.config :refer [config]]
   [district.server.db :refer [db]]
   [district.server.graphql :as graphql]
   [district.server.logging :refer [logging]]
   [district.server.middleware.logging :refer [logging-middlewares]]
   [district.server.smart-contracts]
   [district.server.web3 :refer [web3]]
   [district.server.web3-watcher]
   [goog.date.Date]
   [graphql-query.core :refer [graphql-query]]
   [memefactory.server.db]
   [memefactory.server.deployer]
   [memefactory.server.emailer]
   [memefactory.server.generator]
   [memefactory.server.graphql-resolvers :refer [resolvers-map]]
   [memefactory.server.syncer]
   [district.graphql-utils :as graphql-utils]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts]
   [mount.core :as mount]
   [district.server.graphql.utils :as utils]
   [print.foo :include-macros true]
   [clojure.pprint :refer [print-table]]
   [district.server.db :as db]
   [clojure.string :as str]))

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
                                                                :vote-quorum 50}}}}}
         :smart-contracts {:contracts-var #'memefactory.shared.smart-contracts/smart-contracts
                           :print-gas-usage? true
                           :auto-mining? true}})
    (mount/except [#'memefactory.server.deployer/deployer
                   #'memefactory.server.generator/generator])
    (mount/start)
    pprint/pprint))

(set! *main-cli-fn* -main)

(defn select
  "Usage: (select [:*] :from [:memes])"
  [& [select-fields & r]]
  (-> (db/all (->> (partition 2 r)
                   (map vec)
                   (into {:select select-fields})))
      (print-table)))

(defn print-db
  "Prints all db tables to the repl"
  []
  (let [all-tables (->> (db/all {:select [:name] :from [:sqlite-master] :where [:= :type "table"]})
                        (map :name))]
    (doseq [t all-tables]
      (println "#######" (str/upper-case t) "#######")
      (select [:*] :from [(keyword t)])
      (println "\n\n"))))
