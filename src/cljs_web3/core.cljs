(ns cljs-web3.core
  (:require [cljs-web3.api :as api]))

(defn http-provider [web3 uri]
  (api/-http-provider web3 uri))

(defn connection-url [{:keys [:instance :provider]}]
  (api/-connection-url instance provider))

(defn websocket-provider [web3 uri]
  (api/-websocket-provider web3 uri))

(defn extend [{:keys [:instance :provider]} property methods]
  (api/-extend instance provider property methods))

(defn connected? [{:keys [:instance :provider]}]
  (api/-connected? instance provider))

(defn disconnect [{:keys [:instance :provider]}]
  (api/-disconnect instance provider))

(defn on-connect [{:keys [:instance :provider]} & [callback]]
  (api/-on-connect instance provider callback))

(defn on-disconnect [{:keys [:instance :provider]} & [callback]]
  (api/-on-disconnect instance provider callback))

(defn on-error [{:keys [:instance :provider]} & [callback]]
  (api/-on-error instance provider callback))
