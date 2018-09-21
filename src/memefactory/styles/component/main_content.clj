(ns memefactory.styles.component.main-content
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]))


(defstyles core
  [:.main-content
   {:min-height (em 90)
    :box-shadow "inset 20px 20px 30px rgba(0,0,0,0.05)"
    :background (color :main-content-bg)}])
