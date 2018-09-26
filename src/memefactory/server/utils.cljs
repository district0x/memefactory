(ns memefactory.server.utils
  (:require [cljs-web3.eth :as web3-eth]
            [district.server.web3 :as web3]))

(defn now-in-seconds []
  ;; if we are in dev we use blockchain timestamp so we can
  ;; increment it by hand, and also so we don't need block mining
  ;; in order to keep js time and blockchain time close
  (if js/goog.DEBUG
    (->> (web3-eth/block-number @web3/web3) (web3-eth/get-block @web3/web3) :timestamp)
    (quot (.getTime (js/Date.)) 1000)))
