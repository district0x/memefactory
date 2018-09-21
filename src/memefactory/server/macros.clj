(ns memefactory.server.macros)

(defmacro defer [& body]
  `(.nextTick
   js/process
   (fn []
     ~@body)))
