(ns user
  (:require [figwheel-sidecar.repl-api]))

(defn start-server! []
  (figwheel-sidecar.repl-api/start-figwheel!
   (-> (figwheel-sidecar.config/fetch-config)
       (assoc-in [:data :figwheel-options :server-port] 4578)
       (assoc-in [:data :figwheel-options :nrepl-port]  7777))
    "dev-server")
  (figwheel-sidecar.repl-api/cljs-repl "dev-server"))

(defn start-ui! []
  (figwheel-sidecar.repl-api/start-figwheel!
   (-> (figwheel-sidecar.config/fetch-config)
       (assoc-in [:data :figwheel-options :nrepl-port]  7778))
    "dev")
  (figwheel-sidecar.repl-api/cljs-repl "dev"))

(defn start-tests! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (assoc-in (figwheel-sidecar.config/fetch-config)
              [:data :figwheel-options :server-port] 4579)
    "server-tests")
  (figwheel-sidecar.repl-api/cljs-repl "server-tests"))

(comment
  (start-ui!)
  (start-server!)
  (start-tests!))
