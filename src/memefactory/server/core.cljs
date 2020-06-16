(ns memefactory.server.core
  (:require [cljs-time.core :as t]
            [cljs.nodejs :as nodejs]
            [district.graphql-utils :as graphql-utils]
            [district.server.config :as district.server.config]
            [district.server.db :as district.server.db]
            [district.server.graphql :as district.server.graphql]
            [district.server.graphql.utils :as utils]
            [district.server.logging :as district.server.logging]
            [district.server.middleware.logging :refer [logging-middlewares]]
            [district.server.smart-contracts :as district.server.smart-contracts]
            [district.server.web3 :as district.server.web3]
            [district.server.web3-events :as district.server.web3-events]
            [district.shared.async-helpers :as async-helpers]
            [memefactory.server.constants :as constants]
            [memefactory.server.conversion-rates
             :as
             memefactory.server.conversion-rates]
            [memefactory.server.dank-faucet-monitor
             :as
             memefactory.server.dank-faucet-monitor]
            [memefactory.server.db :as memefactory.server.db]
            [memefactory.server.emailer :as memefactory.server.emailer]
            [memefactory.server.graphql-resolvers :refer [resolvers-map]]
            [memefactory.server.ipfs :as memefactory.server.ipfs]
            [memefactory.server.ranks-cache :as memefactory.server.ranks-cache]
            [memefactory.server.syncer :as memefactory.server.syncer]
            [memefactory.server.twitter-bot :as memefactory.server.twitter-bot]
            [memefactory.shared.graphql-schema :refer [graphql-schema]]
            [memefactory.shared.smart-contracts-dev :as smart-contracts-dev]
            [memefactory.shared.smart-contracts-prod :as smart-contracts-prod]
            [memefactory.shared.smart-contracts-qa :as smart-contracts-qa]
            [mount.core :as mount]
            [taoensso.timbre :as log])
  (:require-macros [memefactory.shared.utils :refer [get-environment]]))

(nodejs/enable-util-print!)

(defonce resync-count (atom 0))

(def contracts-var
  (condp = (get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))

(defn on-jsload []
  (district.server.graphql/restart {:schema (utils/build-schema graphql-schema
                                                                resolvers-map
                                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                                 :gql-name->kw graphql-utils/gql-name->kw})
                                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

(defn start []
  (-> (mount/only #{#'district.server.config/config
                    #'district.server.db/db
                    #'district.server.graphql/graphql
                    #'district.server.logging/logging
                    #'district.server.smart-contracts/smart-contracts
                    #'district.server.web3-events/web3-events
                    #'district.server.web3/web3
                    #'memefactory.server.conversion-rates/conversion-rates
                    #'memefactory.server.dank-faucet-monitor/dank-faucet-monitor
                    #'memefactory.server.db/memefactory-db
                    #'memefactory.server.emailer/emailer
                    #'memefactory.server.ipfs/ipfs
                    #'memefactory.server.ranks-cache/ranks-cache
                    #'memefactory.server.syncer/syncer
                    #'memefactory.server.twitter-bot/twitter-bot})
      (mount/with-args
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
                                      :graphiql false}
                            :web3 {:url "ws://127.0.0.1:8545"
                                   :on-offline (fn []
                                                 (log/warn "Ethereum node went offline, stopping syncing modules" {:resyncs @resync-count} ::web3-watcher)
                                                 (mount/stop #'district.server.web3-events/web3-events
                                                             #'memefactory.server.dank-faucet-monitor/dank-faucet-monitor
                                                             #'memefactory.server.db/memefactory-db
                                                             #'memefactory.server.emailer/emailer
                                                             #'memefactory.server.syncer/syncer
                                                             #'memefactory.server.twitter-bot/twitter-bot))
                                   :on-online (fn []
                                                (log/warn "Ethereum node went online again, starting syncing modules" {:resyncs (swap! resync-count inc)} ::web3-watcher)
                                                (mount/start #'district.server.web3-events/web3-events
                                                             #'memefactory.server.dank-faucet-monitor/dank-faucet-monitor
                                                             #'memefactory.server.db/memefactory-db
                                                             #'memefactory.server.emailer/emailer
                                                             #'memefactory.server.syncer/syncer
                                                             #'memefactory.server.twitter-bot/twitter-bot))}
                            :ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0" :gateway "http://127.0.0.1:8080/ipfs"}
                            :smart-contracts {:contracts-var contracts-var}
                            :ranks-cache {:ttl (t/in-millis (t/minutes 60))}
                            :ui {:public-key "PLACEHOLDER"
                                 :root-url "https://memefactory.io"}
                            :twilio-api-key "PLACEHOLDER"
                            :blacklist-file "blacklist.edn"
                            :twitter-bot {:consumer-key "PLACEHOLDER"
                                          :consumer-secret "PLACEHOLDER"
                                          :access-token-key "PLACEHOLDER"
                                          :access-token-secret "PLACEHOLDER"}
                            :web3-events {:events constants/web3-events}}}})
      (mount/start)
      (as-> $ (log/warn "Started" {:components $
                                   :config @district.server.config/config}))))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (start))

(defn -main [& _]
  (async-helpers/extend-promises-as-channels!)
  (.on js/process "unhandledRejection"
       (fn [reason _] (log/error "Unhandled promise rejection " {:reason reason})))
  (start))

(set! *main-cli-fn* -main)
