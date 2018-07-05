(ns memefactory.styles.base.media
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [garden.units :refer [px]]))

(def breakpoints {:mobile   320
                  :tablet   768
                  :computer 992
                  :large    1200
                  :wide     1920})

(defn min-medias [t]
  (str (- (get breakpoints t)
          1) "px"))

(defn medias [t]
  (str (get breakpoints t) "px"))

(defn for-media-max [m payload]
  (let [max-width (min-medias m)]
    (at-media {:screen true
               :max-width max-width}
              payload)))

(defn for-media-min [m payload]
  (let [min-width (medias m)]
    (at-media {:screen true
               :min-width min-width}
              payload)))
