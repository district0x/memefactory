(ns memefactory.styles.pages.dankregistry.index
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top border-bottom]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [memefactory.styles.component.search :refer [search-panel]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))

(defstyles core
  [:.dank-registry-index-page
   (search-panel {:background-panel-image "/assets/icons/mf-search2.svg"
                  :color :mymemefolio-green
                  :icon "/assets/icons/portfolio2.svg"})

   [:.tiles
    {:display :grid
     :grid-template-columns "1fr 1fr 1fr"
     :margin-top (em 2)
     :padding-top (em 2)
     :padding-bottom (em 2)
     :margin-right (em 6)
     :margin-left (em 6)
     :background-color (color :meme-panel-bg)
     :border-radius "1em 1em 1em 1em"}
    [:.compact-tile
     {:margin :auto}]]])
