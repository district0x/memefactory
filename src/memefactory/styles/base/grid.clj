(ns memefactory.styles.base.grid
  (:require [clojure.string :as s]))

(defn grid-columns [& columns]
  {:grid-template-columns (s/join " " (map str columns))})
