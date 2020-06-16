(ns memefactory.server.ipfs
  (:require [cljs-ipfs-api.core :as ipfs-core]
            [cljs-ipfs-api.swarm :as ipfs-swarm]
            [cljs.nodejs :as nodejs]
            [district.server.config :refer [config]]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(def isIPFS (nodejs/require "is-ipfs"))

(defn ipfs-hash? [hash]
  (-> isIPFS
      (.multihash hash)))

(defn start [opts]
  (let [conn (ipfs-core/init-ipfs opts)
        err-message "Can't connect to IPFS node"]
    (ipfs-swarm/addrs (fn [err _]
                        (when err
                          (do
                            (log/error err-message {:error err
                                                    :connection conn}
                                       ::start)
                            (throw (js/Error. err-message))))))
    conn))

(defstate ipfs
  :start (start (merge (:ipfs @config)
                       (:ipfs (mount/args))))
  :stop :stopped)
