(ns memefactory.server.dank-faucet-monitor
  (:require [district.server.config :as config]
            [district.server.web3-events :as web3-events]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(defn- dispatcher [callback]
  (fn [_ {:keys [:latest-event?] :as event}]
    (when latest-event?
      (callback event))))

(defn log-dank-event [{:keys [:event :args]}]
  (case event
    :DankResetEvent (log/info "DANK Faucet allotment for an account has been reset" args)
    :ResetAllotmentEvent (log/info "DANK Faucet allotment has been reset" args)
    :DankTransferEvent (log/info "DANK successfully transferred from the Faucet" args)
    nil))

(defn start [opts]
  (let [callback-ids
        [(web3-events/register-callback! :dank-faucet/dank-reset-event (dispatcher log-dank-event))
         (web3-events/register-callback! :dank-faucet/reset-allotment-event (dispatcher log-dank-event))
         (web3-events/register-callback! :dank-faucet/dank-transfer-event (dispatcher log-dank-event))]]
    (assoc opts :callback-ids callback-ids)))

(defn stop [dank-faucet-monitor]
  (web3-events/unregister-callbacks! (:callback-ids @dank-faucet-monitor)))

(defstate dank-faucet-monitor
  :start (start (merge (:dank-faucet-monitor @config/config)
                       (:dank-faucet-monitor (mount/args))))
  :stop (stop dank-faucet-monitor))
