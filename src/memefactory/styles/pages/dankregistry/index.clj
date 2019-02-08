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
   {:max-width (px 985)
    :margin-left :auto
    :margin-right :auto}
   (search-panel {:background-panel-image "/assets/icons/mf-search2.svg"
                  :color :blue
                  :icon "/assets/icons/memesubmiticon.svg"})
   [:.tiles
    {:margin-top (em 2)
     :padding-top (em 2)
     :padding-bottom (em 2)
     :background-color (color :meme-panel-bg)
     :border-radius "1em 1em 1em 1em"}
    [:.loading
     {:flex :none
      :color (color :busy-grey)
      :border-top-color (color :white)
      :margin :auto
      :border-width (em 1)
      :border-top-width (em 1)
      :width (em 7)
      :height (em 7)
      :border-style :solid
      :border-top-style :solid
      :border-radius "50%"
      :animation-name :spin
      :animation-duration "2s"
      :animation-iteration-count :infinite
      :animation-timing-function :linear}]

    [:.spinner-outer {:margin-left :auto
                      :margin-right :auto
                      :margin-top (em -1)}]

    #_[">div>div"
       {:display :flex
        :flex-wrap :wrap
        :justify-content :space-evenly}]]])
