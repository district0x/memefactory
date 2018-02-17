(ns memefactory.server.core
  (:require
    [cljs.nodejs :as nodejs]
    [district.server.config :refer [config]]
    [district.server.endpoints.middleware.logging :refer [logging-middlewares]]
    [district.server.logging]
    [district.server.web3-watcher]
    [mount.core :as mount]
    [memefactory.server.api]
    [memefactory.server.db]
    [memefactory.server.deployer]
    [memefactory.server.generator]
    [memefactory.server.syncer]
    [memefactory.shared.smart-contracts]
    [taoensso.timbre :refer-macros [info warn error]]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:web3 {:port 8545}}}
         :smart-contracts {:contracts-var #'memefactory.shared.smart-contracts/smart-contracts}
         :endpoints {:middlewares [logging-middlewares]}
         :web3-watcher {:on-online (fn []
                                     (warn "Ethereum node went online again")
                                     (mount/stop #'memefactory.server.db/memefactory-db)
                                     (mount/start #'memefactory.server.db/memefactory-db
                                                  #'memefactory.server.syncer/syncer
                                                  #'memefactory.server.emailer/emailer))
                        :on-offline (fn []
                                      (warn "Ethereum node went offline")
                                      (mount/stop #'memefactory.server.syncer/syncer
                                                  #'memefactory.server.emailer/emailer))}})
    (mount/except [#'memefactory.server.deployer/deployer
                   #'memefactory.server.generator/generator])
    (mount/start))
  (warn "System started" {:config @config}))

(set! *main-cli-fn* -main)
