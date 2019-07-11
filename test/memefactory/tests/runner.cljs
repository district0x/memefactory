(ns memefactory.tests.runner
  (:require [cljs.nodejs :as nodejs]
            [district.graphql-utils :as graphql-utils]
            [district.server.graphql :as graphql]
            [district.server.graphql.utils :as utils]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]
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
            [memefactory.tests.smart-contracts.utils :as test-utils]
            [taoensso.timbre :as log]
            [cljs-promises.async]
            [clojure.core.async :as async :refer [<!]]
            [cljs-promises.async :refer-macros [<?]]))

(nodejs/enable-util-print!)

(def child-process (nodejs/require "child_process"))
(def spawn (aget child-process "spawn"))

(set! (.-error js/console) (fn [x] (.log js/console x)))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

;; Lets prepare everything for the tests!!!

(defn start-and-run-tests []
  (async/go
    ((test-utils/create-before-fixture))
    (log/info "Transfering dank to accounts" ::deploy-contracts-and-run-tests)
    (doseq [acc (web3-eth/accounts @web3)]
        (<? (dank-token/transfer {:to acc :amount "1000e18"} {:gas 200000})))
    #_(log/info "Account balances now are " ::deploy-contracts-and-run-tests)
    #_(doseq [acc (web3-eth/accounts @web3)]
      (println (str "Balance of " acc " is " (<? (dank-token/balance-of acc)))))
    (log/info "Running tests" ::deploy-contracts-and-run-tests)
    #_(cljs.test/run-tests
     'memefactory.tests.graphql-resolvers.graphql-resolvers-tests
     'memefactory.tests.smart-contracts.registry-entry-tests
     'memefactory.tests.smart-contracts.meme-tests
     'memefactory.tests.smart-contracts.meme-auction-tests
     'memefactory.tests.smart-contracts.registry-tests
     'memefactory.tests.smart-contracts.param-change-tests)))

(defn deploy-contracts-and-run-tests
  "Redeploy smart contracts with truffle"
  []
  (log/warn "Redeploying contracts, please be patient..." ::redeploy)
  (let [child (spawn "truffle migrate --network ganache --reset" (clj->js {:stdio "inherit" :shell true}))]
    (-> child
        (.on "close" (fn []
                       ;; Give it some time to write smart_contracts.cljs
                       ;; if we remove the timeout, it start mount components while we still have the old smart_contract.cljs
                       (js/setTimeout #(start-and-run-tests) 5000))))))

(cljs-promises.async/extend-promises-as-pair-channels!)
#_(deploy-contracts-and-run-tests)
(start-and-run-tests)
