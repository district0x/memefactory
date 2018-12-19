(ns memefactory.server.macros
  (:require [taoensso.timbre]
            [cljs.core]))

(defmacro promise->
  "Takes `thenable` functions as arguments (i.e. functions returning a JS/Promise) and chains them,
   taking care of error handling
   Example:
   (promise-> (thenable-1)
              (thenable-2))"
  [promise & body]
  `(.catch
    (-> ~promise
        ~@(map (fn [expr] (list '.then expr)) body))
    (fn [error#]
      (taoensso.timbre/error "Promise rejected" {:error error#}))))

(defmacro defer [& body]
  `(.nextTick
    js/process
    (fn []
      ~@body)))
