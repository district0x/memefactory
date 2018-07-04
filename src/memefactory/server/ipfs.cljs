(ns memefactory.server.ipfs
  (:require [cljs-ipfs-api.core :as ipfs-core]
            [district.server.config :refer [config]]
            [mount.core :as mount :refer [defstate]]))

(defn start [{:keys [:host :port :endpoint] :as opts}]
  (try
    (let [conn (ipfs-core/init-ipfs {:host (str host ":" port)
                                     :endpoint endpoint})]      
      conn)
    (catch :default e
      (throw (js/Error. "Can't connect to IPFS node")))))

(defstate ipfs
  :start (start (merge (:ipfs @config)
                       (:ipfs (mount/args))))
  :stop :stopped)
