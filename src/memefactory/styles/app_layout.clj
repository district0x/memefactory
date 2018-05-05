(ns memefactory.styles.app-layout
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [garden.units :refer [px]]))

(def breakpoints {:mobile   320
                  :tablet   768
                  :computer 992
                  :large    1200
                  :wide     1920})
(defn medias [t]
  (- (get breakpoints t)
     1))

(defn grid-columns [columns]
  [:grid-template-columns (str (* 100 (/ columns 16))
                               "%")])
(defn for-media [max-width payload]
  (at-media {:screen true
             :max-width max-width}
            payload))

(defstyles core
  [:.app-container
   {:display :grid}
   (grid-columns 2)
   (for-media (medias :mobile)
              (grid-columns 1))])
