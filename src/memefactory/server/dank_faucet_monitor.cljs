(ns memefactory.server.dank-faucet-monitor
  (:require [district.server.config :as config]
            [district.server.web3-events :as web3-events]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(defn- dispatcher [callback]
  (fn [_ {:keys [:latest-event?] :as event}]
    (when latest-event?
      (callback event))))

(defn log-dank-event [{:keys [:event :args] :as evt}]
  (case event
    :DankResetEvent (log/info "DANK Faucet allotment for a phone number has been reset" args)
    :OraclizeRequestEvent (log/info "Oraclize query" args)
    :DankTransferEvent (log/info "DANK succesfully transferred from the Faucet" args)
    nil))

(defn start [opts]
  #_(let [callback-ids
        [(web3-events/register-callback! :dank-faucet/dank-reset-event (dispatcher log-dank-event))
         (web3-events/register-callback! :dank-faucet/oraclize-request-event (dispatcher log-dank-event))
         (web3-events/register-callback! :dank-faucet/dank-transfer-event (dispatcher log-dank-event))]]
    (assoc opts :callback-ids callback-ids)))

(defn stop [dank-faucet-monitor]
  #_(web3-events/unregister-callbacks! (:callback-ids @dank-faucet-monitor)))

(defstate dank-faucet-monitor
  :start (start (merge (:dank-faucet-monitor @config/config)
                       (:dank-faucet-monitor (mount/args))))
  :stop (stop dank-faucet-monitor))
