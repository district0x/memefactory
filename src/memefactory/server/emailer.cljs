(ns memefactory.server.emailer
  (:require
    [cljs-web3.eth :as web3-eth]
    [district.encryption :as encryption]
    [district.sendgrid :refer [send-email]]
    [district.server.config :refer [config]]
    [goog.format.EmailAddress :as email-address]
    [mount.core :as mount :refer [defstate]]
    [memefactory.server.deployer]
    [memefactory.server.generator]
    [taoensso.timbre :refer [info warn error]]))

(declare start)
(declare stop)
(defstate emailer
  :start (start (merge (:emailer @config)
                       (:emailer (mount/args))))
  :stop (stop emailer))

(defn start [opts]
  )


(defn stop [emailer]
  )