(ns memefactory.server.core
  (:require
    [cljs.nodejs :as nodejs]
    [district.server.config :refer [config]]
    [district.server.logging]
    [district.server.middleware.logging :refer [logging-middlewares]]
    [district.server.web3-watcher]
    [memefactory.server.db]
    [memefactory.server.deployer]
    [memefactory.server.emailer]
    [memefactory.server.generator]
    [memefactory.server.syncer]
    [memefactory.server.ranks-cache]
    [memefactory.shared.smart-contracts]
    [mount.core :as mount]
    [taoensso.timbre :refer-macros [info warn error]]
    [cljs-time.core :as t]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:web3 {:port 8545}}}
         :smart-contracts {:contracts-var #'memefactory.shared.smart-contracts/smart-contracts}
         :graphql {:port 6300
                   :middlewares [logging-middlewares]
                   :schema "type Query { hello: String}"
                   :root-value {:hello (constantly "Hello world")}
                   :path "/graphql"}
         :web3-watcher {:on-online (fn []
                                     (warn "Ethereum node went online again")
                                     (mount/stop #'memefactory.server.db/memefactory-db)
                                     (mount/start #'memefactory.server.db/memefactory-db
                                                  #'memefactory.server.syncer/syncer
                                                  #'memefactory.server.emailer/emailer))
                        :on-offline (fn []
                                      (warn "Ethereum node went offline")
                                      (mount/stop #'memefactory.server.syncer/syncer
                                                  #'memefactory.server.emailer/emailer))}
         :syncer {:ipfs-config {:host "http://127.0.0.1:5001" :endpoint "/api/v0"}}
         :ranks-cache {:ttl (t/in-millis (t/minutes 60))}})
    (mount/except [#'memefactory.server.deployer/deployer
                   #'memefactory.server.generator/generator])
    (mount/start))
  (warn "System started" {:config @config}))

(set! *main-cli-fn* -main)

(comment
  (-> (mount/only [#'memefactory.server.generator/generator])
      (mount/stop)
      cljs.pprint/pprint)

  (-> (mount/with-args {:generator {:memes/use-accounts 1
                                    :memes/items-per-account 3
                                    :memes/scenarios [:scenario/buy]}})
      (mount/only [#'memefactory.server.generator/generator])
      (mount/start)
      cljs.pprint/pprint)
 )
