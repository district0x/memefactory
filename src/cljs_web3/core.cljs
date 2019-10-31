(ns cljs-web3.core
  (:require [cljs-web3.api :as api]))

(defn http-provider [web3 uri]
  (api/-http-provider web3 uri))

(defn websocket-provider [web3 uri]
  (api/-websocket-provider web3 uri))

(defn extend [{:keys [:instance :provider]} property methods]
  (api/-extend instance provider property methods))
