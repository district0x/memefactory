(ns cljs-web3.helpers
  (:require [cljs-web3.utils :refer [js->cljkk]]))

(defn method [{:keys [:name :call :params :input-formatter :output-formatter]
               :as signature}]
  (js->cljkk signature))
