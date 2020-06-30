(ns memefactory.styles.pages.how-it-works
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.media :refer [for-media-max]]
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
      #_(for-media-max :tablet
                     [:& {:display :none}])
      [:img {:width (px 130)}]]
     [:.coinbase-wallet
      #_(for-media-min :tablet
                     [:& {:display :none}])
      [:img {:width (px 130)}]]]]])
