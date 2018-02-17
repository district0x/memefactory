(ns memefactory.server.dev
  (:require
    [cljs-web3.core :as web3]
    [cljs.nodejs :as nodejs]
    [cljs.pprint :as pprint]
    [district.server.config :refer [config]]
    [district.server.db :refer [db]]
    [district.server.endpoints]
    [district.server.endpoints.middleware.logging :refer [logging-middlewares]]
    [district.server.logging :refer [logging]]
    [district.server.smart-contracts]
    [district.server.web3 :refer [web3]]
    [district.server.web3-watcher]
    [mount.core :as mount]
    [memefactory.server.api]
    [memefactory.server.db]
    [memefactory.server.deployer]
    [memefactory.server.emailer]
    [memefactory.server.generator]
    [memefactory.server.syncer]
    [memefactory.shared.smart-contracts]
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
          {:deployer {:write? true}}))
    (mount/start)
    pprint/pprint))

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :endpoints {:port 6300
                                        :middlewares [logging-middlewares]}
                            :web3 {:port 8549}
                            :generator {}}}
         :smart-contracts {:contracts-var #'memefactory.shared.smart-contracts/smart-contracts
                           :print-gas-usage? true
                           :auto-mining? true}
         :deployer {:write? true}})
    (mount/except [#'memefactory.server.deployer/deployer
                   #'memefactory.server.generator/generator])
    (mount/start)
    pprint/pprint))

(set! *main-cli-fn* -main)
