(ns cljsjs.filesaverjs
  (:require ["file-saver" :as fs]))

(js/goog.exportSymbol "saveAs" fs)