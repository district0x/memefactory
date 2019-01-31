(ns memefactory.server.core
  (:require
   [cljs-time.core :as t]
   [cljs.nodejs :as nodejs]
   [district.graphql-utils :as graphql-utils]
   [district.server.config :refer [config]]
   [district.server.graphql :as graphql]
   [district.server.graphql.utils :as utils]
   [district.server.logging]
   [district.server.middleware.logging :refer [logging-middlewares]]
   [district.server.web3 :refer [web3]]
   [district.server.web3-watcher]
   [memefactory.server.db]
   [memefactory.server.emailer]
   [memefactory.server.generator]
   [memefactory.server.graphql-resolvers :refer [resolvers-map]]
   [memefactory.server.ipfs]
   [memefactory.server.ranks-cache]
   [memefactory.server.syncer]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts]
   [mount.core :as mount]
   [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(defn -main [& _]
  (-> (mount/with-args
        {:config {:default {:logging {:console? false}
                            :time-source :js-date
                            :graphql {:port 6300
                                      :middlewares [logging-middlewares]
                                      :schema (utils/build-schema graphql-schema
                                                                  resolvers-map
                                                                  {:kw->gql-name graphql-utils/kw->gql-name
                                                                   :gql-name->kw graphql-utils/gql-name->kw})
                                      :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                                      :path "/graphql"
                                      :graphiql true}
                            :web3 {:url "localhost:8545"}
                            :ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0" :gateway "http://127.0.0.1:8080/ipfs"}
                            :smart-contracts {:contracts-var #'memefactory.shared.smart-contracts/smart-contracts}
                            :ranks-cache {:ttl (t/in-millis (t/minutes 60))}
                            :web3-watcher {:on-online (fn []
                                                        (log/warn "Ethereum node went online again" ::web3-watcher)
                                                        (mount/stop #'memefactory.server.db/memefactory-db)
                                                        (mount/start #'memefactory.server.db/memefactory-db
                                                                     #'memefactory.server.syncer/syncer
                                                                     #'memefactory.server.emailer/emailer))
                                           :on-offline (fn []
                                                         (log/warn "Ethereum node went offline" ::web3-watcher)
                                                         (mount/stop #'memefactory.server.syncer/syncer
                                                                     #'memefactory.server.emailer/emailer))}
                            :ui {:public-key "2564e15aaf9593acfdc633bd08f1fc5c089aa43972dd7e8a36d67825cd0154602da47d02f30e1f74e7e72c81ba5f0b3dd20d4d4f0cc6652a2e719a0e9d4c7f10943"
                                 :root-url "https://memefactory.io"}
                            :twilio-api-key "PUT_THE_REAL_KEY_HERE" ;; TODO move this to config outside sources
                            :blacklist-file "blacklist.edn" ;; TODO move this to config outside sources
                            }}})
      (mount/start))
  (log/warn "System started" {:config @config} ::main))

(set! *main-cli-fn* -main)
