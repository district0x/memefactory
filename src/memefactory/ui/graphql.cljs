;; This will be district-ui module, temporary here

(ns memefactory.ui.graphql
  (:require
    [cljs.spec.alpha :as s]
    [memefactory.shared.graphql-utils :as graphql-utils]
    [memefactory.ui.graphql.events :as events]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch-sync]]
    [venia.core :as v]))

(defn start [opts]
  (set! v/*keyword-transform-fn* graphql-utils/clj->graphql)
  (dispatch-sync [::events/start opts])
  opts)

(defn stop []
  (dispatch-sync [::events/stop]))

(defstate ^{:on-reload :noop} district-ui-graphql
  :start (start (:graphql (mount/args)))
  :stop (stop))

