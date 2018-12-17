(ns memefactory.ui.config
  (:require [memefactory.shared.graphql-schema :refer [graphql-schema]])
  (:require-macros [memefactory.ui.utils :refer [get-environment]]))

(def development-config
  {:logging {:level :debug
             :console? true}
   :time-source "blockchain"
   :web3 {:url "http://localhost:8549"}
   :graphql {:schema graphql-schema
             :url "http://localhost:6300/graphql"}
   :ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0"}})

(def qa-config
  {:logging {:level :warn
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"}}
   :time-source "js-date"
   :web3 {:url "http://qa.district0x.io:8545"}
   :graphql {:schema graphql-schema
             :url "http://qa.district0x.io:6300/graphql"}
   :ipfs {:host "http://qa.district0x.io:5001" :endpoint "/api/v0"}})

(def production-config
  {:logging {:level :warn
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"}}
   :time-source "js-date"
   :web3 {:url "http://memefactory.io:8545"}
   :graphql {:schema graphql-schema
             :url "http://memefactory.io:6300/graphql"}
   :ipfs {:host "http://memefactory.io:5001" :endpoint "/api/v0"}})

(def config
  (condp = (get-environment)
    "prod" production-config
    "qa"   qa-config
    "dev"  development-config))
