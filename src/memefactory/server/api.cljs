(ns memefactory.server.api
  (:require [district.server.endpoints :refer [send reg-get!]]
            [district.server.config :refer [config]])
    (:require-macros [memefactory.server.macros :refer [try-catch]]))

(reg-get! "/config"
          (fn [req res]
            (try-catch
             (send res (:ui @config)))))
