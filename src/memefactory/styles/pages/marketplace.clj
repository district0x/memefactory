(ns memefactory.styles.pages.marketplace
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.media :refer [for-media-max]]
            [memefactory.styles.component.compact-tile
             :refer
             [overlay-background-footer]]
            [memefactory.styles.component.search :refer [search-panel]]))

(defstyles core
  [:.marketplace-page
   (search-panel {:background-panel-image "/assets/icons/mf-search.svg"
                  :color :dark-blue
                  :icon "/assets/icons/cart-green-blue.svg"})
   ;; TODO: don;t know where this is coming from, but need to override flex that breaks loading div
   #_[".tiles>*"
      {:flex :none}]
   [:.search-results
    {:box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :border-radius "1em 1em 1em 1em"
     :background-color :white
     :margin-top (em 2)
     :padding-top (em 1)
     :padding-bottom (em 1)
     :overflow :hidden}
    [:.scroll-area
     {:width "100%"
      :margin-left :auto
      :margin-right :auto}
     [:.tiles
      {:display :flex
       :min-height (px 550)

       :padding-bottom (em 2)
       :background-color (color :meme-panel-bg)}
      (for-media-max :computer
                     [:&
                      {:margin-right (em 0)
                       :margin-left (em 0)}])
      [:.meme-card
       [:.overlay {:background overlay-background-footer}]]
      [:.spinner-container {:width (px 900)
                            :height (px 500)}
       (for-media-max :computer
                      [:&
                       {:width (px 290)}])
       [:.spinner-outer {:margin-left :auto
                         :margin-right :auto
                         :padding-top (em 12)}]]
      [:.footer
       [:.token-id {:display :inline-block}]]]]]])
