(ns memefactory.server.sigterm
  (:require [cljs.nodejs :as nodejs]
            [district.server.config :refer [config]]
            [mount.core :as mount :refer [defstate]]))

(defn start [{:keys [:on-sigterm]}]
  (-> nodejs/process
      (.on "SIGTERM" on-sigterm)))

(defstate sigterm
  :start (start (merge (:sigterm @config)
                       (:sigterm (mount/args))))
  :stop :stopped)
