(ns memefactory.tests.runner
  (:require [cljs.nodejs :as nodejs]
            [cljs.test :refer [run-tests]]
            [district.graphql-utils :as graphql-utils]
            [district.server.graphql :as graphql]
            [district.server.graphql.utils :as utils]
            [memefactory.server.graphql-resolvers :refer [resolvers-map]]
            [memefactory.shared.graphql-schema :refer [graphql-schema]]
            [memefactory.tests.graphql-resolvers.graphql-resolvers-tests]
            [memefactory.tests.smart-contracts.deployment-tests]
            [memefactory.tests.smart-contracts.meme-auction-tests]
            [memefactory.tests.smart-contracts.meme-tests]
            [memefactory.tests.smart-contracts.param-change-tests]
            [memefactory.tests.smart-contracts.registry-entry-tests]
            [memefactory.tests.smart-contracts.registry-tests]))

(nodejs/enable-util-print!)

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

(set! (.-error js/console) (fn [x] (.log js/console x)))

(defn -main [& _]
  (run-tests 'memefactory.tests.smart-contracts.deployment-tests
             'memefactory.tests.smart-contracts.meme-auction-tests
             'memefactory.tests.smart-contracts.param-change-tests
             'memefactory.tests.smart-contracts.registry-entry-tests
             'memefactory.tests.smart-contracts.registry-tests
             'memefactory.tests.smart-contracts.meme-tests
             'memefactory.tests.graphql-resolvers.graphql-resolvers-tests))

(set! *main-cli-fn* -main)
