(ns memefactory.server.generator
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.cljs-utils :refer [rand-str]]
    [district.server.config :refer [config]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.deployer]
    [mount.core :as mount :refer [defstate]]))

(declare start)
(defstate ^{:on-reload :noop} generator :start (start (merge (:generator @config)
                                                             (:generator (mount/args)))))

(defn start [{:keys [:total-accounts :offerings-per-account]}]
  #_ (let [my-accounts (web3-eth/accounts @web3)]
    (dotimes [address-index total-accounts]
      (dotimes [_ offerings-per-account]
        ))))