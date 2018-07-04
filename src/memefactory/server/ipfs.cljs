(ns memefactory.server.ipfs
  (:require [cljs-ipfs-api.core :as ipfs-api]
            [district.server.config :refer [config]]
            [mount.core :as mount :refer [defstate]]))

(defn start [{:keys [:host :endpoint] :as opts}]
  (if-let [conn (ipfs-api/init-ipfs opts)]
    conn
    (throw (js/Error. "Can't connect to IPFS node"))))

(defstate ipfs
  :start (start (merge (:ipfs @config)
                       (:ipfs (mount/args))))
  :stop :stopped)
