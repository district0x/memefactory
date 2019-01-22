(ns memefactory.styles.pages.marketplace
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [memefactory.styles.component.search :refer [search-panel]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))

(defstyles core
  [:.marketplace-page
   (search-panel {:background-panel-image "/assets/icons/mf-search.svg"
                  :color :dark-blue
                  :icon "/assets/icons/cart-green-blue.svg"})
   ;; TODO: don;t know where this is coming from, but need to override flex that breaks loading div
   #_[".tiles>*"
      {:flex :none}]
   [:.scroll-area
    {:width "100%"
     :margin-left :auto
     :margin-right :auto
     }
    [:.tiles
     {:display :flex
      :margin-top (em 2)
      :padding-top (em 2)
      :padding-bottom (em 2)
      :background-color (color :meme-panel-bg)
      :border-radius "1em 1em 1em 1em"}
     (for-media-max :tablet
                   [:&
                    {:margin-right (em 0)
                     :margin-left (em 0)}])
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
     [:.footer
      [:.token-id {:display :inline-block}]
      [:.number-minted {:margin-left (em 0.4)
                        :font-size (em 0.8)}]]
     #_[:.container
        [:.meme-card.front
         {:backface-visibility :visible}]]
     #_[">div>div"
        {:display :flex
         :flex-wrap :wrap
         :justify-content :space-evenly}]]]
   ])
