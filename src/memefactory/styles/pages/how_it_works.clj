(ns memefactory.styles.pages.how-it-works
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.how-it-works-page
   [:.panel {:margin-bottom (em 2)}]
   [:.body {:font-size (px 15)
            :padding (em 2)}
    [:iframe :img.how
     {:margin-left :auto
      :margin-right :auto
      :display :block
      :margin-bottom (px 25)
      :margin-top (px 25)
      :width (px 560)
      :height (px 315)}
     (for-media-max :tablet
                    [:& {:width "100%"
                         :height :unset}])]]
   [:.how-it-works
    (panel-with-icon {:url "/assets/icons/how-it-works2.svg"
                      :color "#202ca1"})
    [:.badges {:display :flex
               :align-items :center
               :justify-content :space-evenly}
     [:.metamask-wallet
      (for-media-max :tablet
                     [:& {:display :none}])
      [:img {:width (px 130)}]]
     [:.coinbase-wallet
      (for-media-min :tablet
                     [:& {:display :none}])
      [:img {:width (px 130)}]]]]])
