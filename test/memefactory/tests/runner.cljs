(ns memefactory.tests.runner
  (:require
   [cljs-time.core :as time]
   [cljs.nodejs :as nodejs]
   [cljs.test :refer [run-tests]]
   [district.graphql-utils :as graphql-utils]
   [district.server.db]
   [district.server.graphql :as graphql]
   [district.server.graphql.utils :as utils]
   [district.server.logging]
   [district.server.middleware.logging :refer [logging-middlewares]]
   [district.shared.async-helpers :as async-helpers]
   [doo.runner :refer-macros [doo-tests]]
   [memefactory.server.graphql-resolvers :refer [resolvers-map]]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts-dev :refer [smart-contracts]]
   [memefactory.tests.graphql-resolvers.graphql-resolvers-tests]
   [mount.core :as mount]
   [taoensso.timbre :as log]

   [memefactory.tests.smart-contracts.deployment-tests]
   ;; [memefactory.tests.smart-contracts.meme-auction-tests]
   ;; [memefactory.tests.smart-contracts.meme-tests]
   ;; [memefactory.tests.smart-contracts.param-change-tests]
   ;; [memefactory.tests.smart-contracts.registry-entry-tests]
   ;; [memefactory.tests.smart-contracts.registry-tests]

   ))

(nodejs/enable-util-print!)

(async-helpers/extend-promises-as-channels!)

(defn start-and-run-tests []
  (-> (mount/with-args {:web3 {:url "ws://127.0.0.1:8545"}
                        :db {:opts {:memory true}}
                        :smart-contracts {:contracts-var #'smart-contracts}
                        :graphql {:port 6300
                                  :middlewares [logging-middlewares]
                                  :schema (utils/build-schema graphql-schema
                                                              resolvers-map
                                                              {:kw->gql-name graphql-utils/kw->gql-name
                                                               :gql-name->kw graphql-utils/gql-name->kw})
                                  :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                                  :path "/graphql"
                                  :graphiql true}
                        :logging {:level :info
                                  :console? true}
                        :time-source :blockchain
                        :ranks-cache {:ttl (time/in-millis (time/minutes 60))}})
      (mount/only [#'district.server.logging/logging
                   #'district.server.db/db
                   #'memefactory.server.db/memefactory-db
                   #'district.server.graphql/graphql
                   #'district.server.web3/web3
                   #'district.server.smart-contracts/smart-contracts])
      (mount/start)
      (as-> $ (log/warn "Started" $)))

  (;;doo-tests
   run-tests
   'memefactory.tests.graphql-resolvers.graphql-resolvers-tests
   'memefactory.tests.smart-contracts.deployment-tests
   ;; 'memefactory.tests.smart-contracts.registry-entry-tests
   ;; 'memefactory.tests.smart-contracts.meme-tests
   ;; 'memefactory.tests.smart-contracts.meme-auction-tests
   ;; 'memefactory.tests.smart-contracts.registry-tests
   ;; 'memefactory.tests.smart-contracts.param-change-tests
   ))

(start-and-run-tests)
