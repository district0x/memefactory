(ns cljsjs.eccjs
  (:require ["eccjs" :as eccjs]))

(js/goog.exportSymbol "ecc" eccjs)
