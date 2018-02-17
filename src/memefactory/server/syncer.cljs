(ns memefactory.server.syncer
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.server.config :refer [config]]
    [district.server.web3 :refer [web3]]
    [mount.core :as mount :refer [defstate]]
    [memefactory.server.db :as db]
    [memefactory.server.deployer]
    [memefactory.server.generator]
    [district.server.smart-contracts :refer [replay-past-events]]
    [taoensso.timbre :refer-macros [info warn error]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop syncer))

(defn start [opts]
  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))
  )


(defn stop [syncer]
  )
