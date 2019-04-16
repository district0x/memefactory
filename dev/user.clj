(ns user
  (:require [figwheel-sidecar.repl-api]))

(defn start-server! []
  (figwheel-sidecar.repl-api/start-figwheel!
   (-> (figwheel-sidecar.config/fetch-config)
       (assoc-in [:data :figwheel-options :server-port] 4578)
       (assoc-in [:data :figwheel-options :nrepl-port] 7777))
    "dev-server")
  (figwheel-sidecar.repl-api/cljs-repl "dev-server"))

(defn start-ui! []
  (figwheel-sidecar.repl-api/start-figwheel!
   (-> (figwheel-sidecar.config/fetch-config)
       (assoc-in [:data :figwheel-options :nrepl-port] 7778))
    "dev-ui")
  (figwheel-sidecar.repl-api/cljs-repl "dev-ui"))

(defn start-tests! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (assoc-in (figwheel-sidecar.config/fetch-config)
              [:data :figwheel-options :server-port] 4579)
    "server-tests")
  (figwheel-sidecar.repl-api/cljs-repl "server-tests"))

(defn start-pinner! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (-> (figwheel-sidecar.config/fetch-config)
        (assoc-in [:data :figwheel-options :server-port] 4577)
        (assoc-in [:data :figwheel-options :nrepl-port]  7779))
    "dev-pinner")
  (figwheel-sidecar.repl-api/cljs-repl "dev-pinner"))

(comment
  (start-ui!)
  (start-server!)
  (start-tests!))
