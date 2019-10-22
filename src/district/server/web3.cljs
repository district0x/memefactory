(ns district.server.web3
  (:require
   [cljs-web3.helpers :as web3-helpers]
   [cljs-web3.macros]
   [clojure.string :as string]
   [cljs-web3.core :as web3-core]
   [web3.impl.web3js :as web3js]
   [district.server.config :refer [config]]
   [mount.core :as mount :refer [defstate]]))

(declare start)
(declare stop)

(defstate web3
  :start (start (merge (:web3 @config)
                       (:web3 (mount/args))))
  :stop (stop web3))

(defn websocket-connection? [uri]
  (string/starts-with? uri "ws"))

(defn create [{:keys [:host :port :url] :as opts}]

  (prn "@@@ create")

  (let [uri (if url
              url
              (str (or host "http://127.0.0.1") ":" port))
        instance (web3js/new)]
    {:instance instance
     :provider (if (websocket-connection? uri)
                 (web3-core/websocket-provider instance uri)
                 (web3-core/http-provider instance uri))}))

(defn start [{:keys [:port :url] :as opts}]

  (prn "@@@ start")

  (when (and (not port) (not url))
    (throw (js/Error. "You must provide port or url to start the web3 component")))

  (web3-core/extend (create opts)
    :evm
    [(web3-helpers/method {:name "increaseTime"
                           :call "evm_increaseTime"
                           :params 1})
     (web3-helpers/method {:name "mineBlock"
                           :call "evm_mine"})]))

(defn stop [this]
  (web3-core/disconnect @this))
