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
    [memefactory.server.db]
    [memefactory.server.deployer]
    [memefactory.server.emailer]
    [memefactory.server.generator]
    [memefactory.server.graphql-resolvers :refer [graphql-resolvers]]
    [memefactory.server.syncer]
    [memefactory.shared.graphql-schema :refer [graphql-schema]]
    [memefactory.shared.smart-contracts]
    [mount.core :as mount]
    [venia.core :as v]
    [print.foo :include-macros true]))

(nodejs/enable-util-print!)

(defn on-jsload []
  (graphql/restart {:root-value graphql-resolvers :schema graphql-schema}))


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
  (mount.core/stop #'memefactory.server.db/memefactory-db
                   #'memefactory.server.syncer/syncer)
  (-> (mount.core/start #'memefactory.server.db/memefactory-db
                        #'memefactory.server.syncer/syncer)
    pprint/pprint))


(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :graphql {:port 6300
                                      :middlewares [logging-middlewares]
                                      :schema graphql-schema
                                      :root-value graphql-resolvers
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
                                                        :max-start-price (web3/to-wei 1 :ether)
                                                        :max-total-supply 10000
                                                        :offering-duration (t/in-seconds (t/minutes 10))}
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

(comment
  (graphql/run-query
    (v/graphql-query {:venia/queries [[:searchMemes [:reg-entry/address
                                                     :reg-entry/createdOn
                                                     [:availableVoteAmount {:voter "0x987"}]
                                                     [:vote {:voter "0x987"} [:amount :option]]]]]})
    println))
