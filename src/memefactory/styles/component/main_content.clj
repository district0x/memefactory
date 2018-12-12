(ns memefactory.styles.component.main-content
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]))


(defstyles core
  [:.main-content
   {:min-height (em 90)
    :box-shadow "inset 20px 20px 30px rgba(0,0,0,0.05)"
    :background (color :main-content-bg)}
   [:.main-content-inner
    {:max-width (px 985)
     :margin-left :auto
     :margin-right :auto
     :padding-top (em 4)}
    (for-media-max :large
                   [:&
                    {:margin-left (em 1)
                     :margin-right (em 1)}])
    (for-media-max :tablet
                   [:&
                    {:padding-top (em 1)}])
    ]])
