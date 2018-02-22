(ns memefactory.server.syncer
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [replay-past-events]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.registry :as registry]
    [memefactory.server.db :as db]
    [memefactory.server.deployer]
    [memefactory.server.generator]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :refer-macros [info warn error]]))

(declare start)
(declare stop)
(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop syncer))

(defn on-registry-entry-event [err {{:keys [:registry-entry :event-type :version :data] :as args} :args}]
  (println registry-entry (web3/to-ascii event-type)))

(defn start [opts]
  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))
  (-> (registry/registry-entry-event [:meme-registry :meme-registry-fwd] {} {:from-block 0 :to-block "latest"})
    (replay-past-events on-registry-entry-event)))


(defn stop [syncer]
  )
