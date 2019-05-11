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
   [memefactory.server.db]
   [memefactory.server.emailer]
   [memefactory.server.generator]
   [memefactory.server.graphql-resolvers :refer [resolvers-map]]
   [memefactory.server.ipfs]
   [memefactory.server.pinner]
   [memefactory.server.ranks-cache]
   [memefactory.server.sigterm]
   [memefactory.server.syncer]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts-dev :as smart-contracts-dev]
   [memefactory.shared.smart-contracts-prod :as smart-contracts-prod]
   [memefactory.shared.smart-contracts-qa :as smart-contracts-qa]
   [mount.core :as mount]
   [taoensso.timbre :as log])
  (:require-macros [memefactory.shared.utils :refer [get-environment]]))

(nodejs/enable-util-print!)

(def contracts-var
  (condp = (get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "qa-dev" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))


(defn restart-syncing-modules [err evt]
  (log/error "Error in event callback, restarting syncing modules"
             {:err err :event evt}
             ::web3-events-on-erorr)
  (js/setTimeout                                            ;; some extra time for parity to put itself together after error
    (fn []
      (mount/stop #'memefactory.server.db/memefactory-db
                  #'district.server.web3-events/web3-events)
      (mount/start #'district.server.web3-events/web3-events
                   #'memefactory.server.db/memefactory-db
                   #'memefactory.server.syncer/syncer
                   #'memefactory.server.pinner/pinner
                   #'memefactory.server.emailer/emailer)
      10000)))


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
                                      :graphiql false}
                            :web3 {:url "http://localhost:8545"}
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
                                                    (.exit nodejs/process 0))}}}
         :web3-events {:events constants/web3-events
                       ;; to prevent loopback of death, can be called only once per 2 minutes
                       :on-error (gfun/throttle restart-syncing-modules 120000)}})
      (mount/start)
      (#(log/warn "Started" {:components %
                             :config @config}))))

(set! *main-cli-fn* -main)
