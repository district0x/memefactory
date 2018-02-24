(ns memefactory.server.dev
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs.nodejs :as nodejs]
    [cljs.pprint :as pprint]
    [district.server.config :refer [config]]
    [district.server.db :refer [db]]
    [district.server.endpoints.middleware.logging :refer [logging-middlewares]]
    [district.server.endpoints]
    [district.server.logging :refer [logging]]
    [district.server.smart-contracts]
    [district.server.web3 :refer [web3]]
    [district.server.web3-watcher]
    [goog.date.Date]
    [memefactory.server.api]
    [memefactory.server.db]
    [memefactory.server.deployer]
    [memefactory.server.emailer]
    [memefactory.server.generator]
    [memefactory.server.syncer]
    [memefactory.shared.smart-contracts]
    [mount.core :as mount]
    [print.foo :include-macros true]))

(nodejs/enable-util-print!)

(defn on-jsload []
  (mount/stop #'district.server.endpoints/endpoints)
  (mount/start #'district.server.endpoints/endpoints))

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
          {:deployer {:write? false}}))
    (mount/start)
    pprint/pprint))

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :endpoints {:port 6300
                                        :middlewares [logging-middlewares]}
                            :web3 {:port 8549}
                            :generator {:memes/use-accounts 1
                                        :memes/items-per-account 1
                                        :param-changes/use-accounts 1
                                        :param-changes/items-per-account 1}
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
                                                        :sale-duration (t/in-seconds (t/minutes 10))}
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
