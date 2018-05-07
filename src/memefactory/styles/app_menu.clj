(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.colors :refer [color]]
            [garden.units :refer [px]]))

(defstyles core
  [:.app-container
   [:.app-menu
    {:background (color "red")}]])
