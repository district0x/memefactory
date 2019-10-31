(ns cljs-web3.evm
  (:require [cljs-web3.api :as api]))

(defn increase-time [{:keys [:instance :provider]} seconds]
 (api/-increase-time instance provider seconds))

(defn mine-block [{:keys [:instance :provider]}]
 (api/-mine-block instance provider))
