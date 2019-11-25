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
   [district.server.web3-events]
   [goog.functions :as gfun]
   [memefactory.server.constants :as constants]
   [memefactory.server.conversion-rates]
   [memefactory.server.dank-faucet-monitor]
   [memefactory.server.db]
   [memefactory.server.emailer]
   [memefactory.server.generator]
   [memefactory.server.graphql-resolvers :refer [resolvers-map]]
   [memefactory.server.ipfs]
   [memefactory.server.pinner]
   [memefactory.server.ranks-cache]
   [memefactory.server.sigterm]
   [memefactory.server.syncer]
   [memefactory.server.twitter-bot]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts-dev :as smart-contracts-dev]
   [memefactory.shared.smart-contracts-prod :as smart-contracts-prod]
   [memefactory.shared.smart-contracts-qa :as smart-contracts-qa]
   [mount.core :as mount]
   [memefactory.shared.utils :as shared-utils]
   [taoensso.timbre :as log]
   [district.shared.async-helpers :as async-helpers])
  (:require-macros [memefactory.shared.utils :refer [get-environment]]))

(nodejs/enable-util-print!)

(def contracts-var
  (condp = (get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "qa-dev" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))


(defn -main [& _]

  (async-helpers/extend-promises-as-channels!)

  (.on js/process "unhandledRejection"
       (fn [reason p] (log/error "Unhandled promise rejection " {:reason reason})))

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
                                      :graphiql false}
                            :web3 {:url "ws://127.0.0.1:8545"
                                   :on-offline (fn []
                                                 (log/error "Ethereum node went offline, stopping syncing modules" ::web3-watcher)
                                                 (mount/stop #'memefactory.server.db/memefactory-db
                                                             #'district.server.web3-events/web3-events
                                                             #'memefactory.server.syncer/syncer
                                                             #'memefactory.server.pinner/pinner
                                                             #'memefactory.server.emailer/emailer))
                                   :on-online (fn []
                                                (log/warn "Ethereum node went online again, starting syncing modules" ::web3-watcher)
                                                (mount/start #'memefactory.server.db/memefactory-db
                                                             #'district.server.web3-events/web3-events
                                                             #'memefactory.server.syncer/syncer
                                                             #'memefactory.server.pinner/pinner
                                                             #'memefactory.server.emailer/emailer))}
                            :ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0" :gateway "http://127.0.0.1:8080/ipfs"}
                            :smart-contracts {:contracts-var contracts-var}
                            :ranks-cache {:ttl (t/in-millis (t/minutes 60))}
                            :ui {:public-key "PLACEHOLDER"
                                 :root-url "https://memefactory.io"}
                            :twilio-api-key "PLACEHOLDER"
                            :blacklist-file "blacklist.edn"
                            :sigterm {:on-sigterm (fn [args]
                                                    (log/warn "Received SIGTERM signal. Exiting" {:args args})
                                                    (mount/stop)
                                                    (.exit nodejs/process 0))}
                            :twitter-bot {:consumer-key "PLACEHOLDER"
                                          :consumer-secret "PLACEHOLDER"
                                          :access-token-key "PLACEHOLDER"
                                          :access-token-secret "PLACEHOLDER"}
                            :web3-events {:events constants/web3-events}}}})
      (mount/start)
      (#(log/warn "Started" {:components %
                             :config @config}))))

(set! *main-cli-fn* -main)
