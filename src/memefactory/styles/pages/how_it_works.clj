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
    [:iframe {:margin-left :auto
              :margin-right :auto
              :display :block
              :margin-bottom (px 25)
              :margin-top (px 25)}]]
   [:.how-it-works
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]
   [:.setting-up
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]
   [:.browsing-memes
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]
   [:.buying-memes
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]
   [:.getting-dank
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]
   [:.voting
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]
   [:.challenging
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]
   [:.submitting-memes
    (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                      :color :sky-blue})]])
