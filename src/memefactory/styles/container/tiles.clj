(ns memefactory.styles.container.tiles
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [px em]]))

(defstyles core
  [:.tiles
   {:display :flex
    :flex-wrap :wrap
    :justify-content :space-around
    :padding-left (em 2.5)
    :padding-right (em 2.5)
    :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
    }
   [">*"
    {:flex "1 0 calc(26% - 1em)"}]])
