(ns memefactory.styles.component.compact-tile
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [px em]]))

(def bar-height (px 50))
(def card-aspect-ratio 1.5)
(def card-width 16)
(def card-height (int (* card-aspect-ratio card-width)))

(defstyles core
  [:.meme-card
   {:width (em card-width)
    :height (em card-height)
    :background-color (color "gray")
    :border-radius "1em"
    }
   [:img
    {:width (em card-width)
     :height (em card-height)}
    ]]
  [:.compact-tile
   {:background (color :meme-panel-bg)
    :margin (em 1)
    ;; :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :block}])
