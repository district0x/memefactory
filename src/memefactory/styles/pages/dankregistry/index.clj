(ns memefactory.styles.pages.dankregistry.index
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.component.search :refer [search-panel]]))

(defstyles core
  [:.dank-registry-index-page
   {:max-width (px 985)
    :margin-left :auto
    :margin-right :auto}
   (search-panel {:background-panel-image "/assets/icons/mf-search2.svg"
                  :color :blue
                  :icon "/assets/icons/memesubmiticon.svg"})
   [:.spinner-container {:width (px 900)
                         :height (px 500)}
    [:.spinner-outer {:margin-left :auto
                      :margin-right :auto
                      :padding-top (em 12)}]]
   [:.scroll-area
    {:box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :border-radius "1em 1em 1em 1em"
     :overflow :hidden
     :background-color (color :meme-panel-bg)
     :margin-top (em 2)
     :padding-top (em 1)}
    [:.tiles
     { :min-height (px 550)
      :background-color (color :meme-panel-bg)}
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
       :animation-timing-function :linear}]]]])
