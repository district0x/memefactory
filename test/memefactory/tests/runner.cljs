(ns memefactory.tests.runner
  (:require [cljs.nodejs :as nodejs]
            [district.graphql-utils :as graphql-utils]
            [district.server.graphql :as graphql]
            [district.server.graphql.utils :as utils]
            [doo.runner :refer-macros [doo-tests]]
            [memefactory.server.graphql-resolvers :refer [resolvers-map]]
            [memefactory.shared.graphql-schema :refer [graphql-schema]]
            [memefactory.tests.graphql-resolvers.graphql-resolvers-tests]
            [memefactory.tests.smart-contracts.deployment-tests]
            [memefactory.tests.smart-contracts.meme-auction-tests]
            [memefactory.tests.smart-contracts.meme-tests]
            [memefactory.tests.smart-contracts.param-change-tests]
            [memefactory.tests.smart-contracts.registry-entry-tests]
            [memefactory.tests.smart-contracts.registry-tests]
            [memefactory.tests.smart-contracts.utils :as test-utils]))

(nodejs/enable-util-print!)

(set! (.-error js/console) (fn [x] (.log js/console x)))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

;; Lets prepare everything for the tests!!!

((test-utils/create-before-fixture))

(doo-tests
 ;; 'memefactory.tests.smart-contracts.deployment-tests
 #_'memefactory.tests.smart-contracts.meme-auction-tests
 #_'memefactory.tests.smart-contracts.param-change-tests
 #_'memefactory.tests.smart-contracts.registry-entry-tests
 #_'memefactory.tests.smart-contracts.registry-tests
 #_'memefactory.tests.smart-contracts.meme-tests
 'memefactory.tests.graphql-resolvers.graphql-resolvers-tests)
