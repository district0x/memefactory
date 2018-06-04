(ns memefactory.server.macros
  (:require [taoensso.timbre :as log]
            [cljs.core :as cljs]))

(defn- compiletime-info
  [and-env and-form ns]
  (let [meta-info (meta and-form)]
    {:ns (str (ns-name ns))
     :line (:line meta-info)
     :file (:file meta-info)}))

(defmacro try-catch-throw [& body]
  `(try
     ~@body
     (catch js/Object e#
       (log/error "Unexpected exception" (merge {:error (cljs/ex-message e#)} ~(compiletime-info &env &form *ns*)))
       (throw (js/Error. e#)))))
