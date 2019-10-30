(ns memefactory.tests.runner
  (:require [cljs-promises.async :refer-macros [<?]]
            [cljs-promises.async]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs.nodejs :as nodejs]
            [clojure.core.async :as async :refer [<!]]
            [district.graphql-utils :as graphql-utils]
            [district.server.graphql :as graphql]
            [district.server.graphql.utils :as utils]
            [district.server.web3 :refer [web3]]
            [doo.runner :refer-macros [doo-tests]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.graphql-resolvers :refer [resolvers-map]]
            [memefactory.shared.graphql-schema :refer [graphql-schema]]
            [memefactory.tests.graphql-resolvers.graphql-resolvers-tests]
            [memefactory.tests.smart-contracts.deployment-tests]
            [memefactory.tests.smart-contracts.meme-auction-tests]
            [memefactory.tests.smart-contracts.meme-tests]
            [memefactory.tests.smart-contracts.param-change-tests]
            [memefactory.tests.smart-contracts.registry-entry-tests]
            [memefactory.tests.smart-contracts.registry-tests]
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [taoensso.timbre :as log]))

(nodejs/enable-util-print!)
(cljs-promises.async/extend-promises-as-pair-channels!)

(set! (.-error js/console) (fn [x] (.log js/console x)))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

(defn start-and-run-tests []
  (async/go
    ((test-utils/create-before-fixture))
    (log/info "Account balances now are " ::start-and-run-tests)
    (doseq [acc (web3-eth/accounts @web3)]
      (println (str "Balance of " acc " is " (<? (dank-token/balance-of acc)))))
    (log/info "Running tests" ::start-and-run-tests)
    (cljs.test/run-tests
     'memefactory.tests.graphql-resolvers.graphql-resolvers-tests
     'memefactory.tests.smart-contracts.registry-entry-tests
     'memefactory.tests.smart-contracts.meme-tests
     'memefactory.tests.smart-contracts.meme-auction-tests
     'memefactory.tests.smart-contracts.registry-tests
     ;; 'memefactory.tests.smart-contracts.param-change-tests
     )))

(start-and-run-tests)
