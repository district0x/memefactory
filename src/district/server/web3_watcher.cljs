(ns district.server.web3-watcher
  (:require [cljs-web3.eth :as web3-eth]
            [cljs-web3.core :as web3-core]
            [district.server.config :refer [config]]
            [district.server.web3 :refer [web3 create]]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(declare start)
(declare stop)

(defstate web3-watcher
  :start (start (merge (:web3-watcher @config)
                       (:web3-watcher (mount/args))))
  :stop (stop web3-watcher))

(defn start [{:keys [:interval :on-online :on-offline] :as opts
              :or {interval 3000 confirmations 3}}]
  (let [interval-id (atom nil)]
    (web3-core/on-disconnect @web3
                             (fn [event]
                               (log/info "Web3 connection interrupted" {:event event})
                               (on-offline)
                               (reset! interval-id (js/setInterval (fn []
                                                                     (web3-eth/is-listening? (create {:url (web3-core/connection-url @web3)})
                                                                                          (fn [error result]
                                                                                            (let [connected? (and (nil? error) result)]
                                                                                              (log/debug "Polling..." {:connected? connected?})
                                                                                              (when connected?
                                                                                                (js/clearInterval @(:interval-id @web3-watcher))
                                                                                                ;; TODO : blows up
                                                                                                (on-online))))))
                                                                   interval))))
    {:interval-id interval-id}))

(defn stop [this]
  (js/clearInterval @(:interval-id @this)))
