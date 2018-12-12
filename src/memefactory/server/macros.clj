(ns memefactory.server.macros)

(defmacro promise-> [promise & body]
  `(-> ~promise
        ~@(map (fn [expr] (list '.then expr)) body)))

(defmacro defer [& body]
  `(.nextTick
   js/process
   (fn []
     ~@body)))
