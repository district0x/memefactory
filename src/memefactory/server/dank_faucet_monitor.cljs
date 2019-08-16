(ns memefactory.server.dank-faucet-monitor
  (:require [district.server.config :as config]
            [district.server.web3-events :as web3-events]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(defn- dispatcher [callback]
  (fn [_ {:keys [:latest-event?] :as event}]

    (log/debug "### FAUCET EVENT" event)

    (when latest-event?
      (callback event))))

(defn log-dank-event [{:keys [:event] :as evt}]

  (log/debug "### LATEST FAUCET EVENT" evt)

  )

(defn start [opts]
  (let [callback-ids
        [
         ;; (web3-events/register-callback! :dank-faucet/not-enough-eth (dispatcher log-dank-event))
         (web3-events/register-callback! :dank-faucet/dank-event (dispatcher log-dank-event))
         (web3-events/register-callback! :dank-faucet/oraclize-call (dispatcher log-dank-event))
         (web3-events/register-callback! :dank-faucet/dank-reset (dispatcher log-dank-event))]]
    (assoc opts :callback-ids callback-ids)))

(defn stop [dank-faucet-monitor]
  (web3-events/unregister-callbacks! (:callback-ids @dank-faucet-monitor)))

(defstate dank-faucet-monitor
  :start (start (merge (:dank-faucet-monitor @config/config)
                       (:dank-faucet-monitor (mount/args))))
  :stop (stop dank-faucet-monitor))
