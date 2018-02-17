(ns memefactory.server.api
  (:require
    [district.server.endpoints :refer [send reg-get! query-params route-params]]))

(reg-get! "/hello"
          (fn [req res]
            (send res "hello")))