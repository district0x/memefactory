(ns memefactory.styles.pages.privacy-policy
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
  [:.privacy-policy-page
   [:.panel {:margin-bottom (em 2)}]
   [:.body {:font-size (px 15)
            :padding (em 2)}]
   [:.privacy-policy
    (panel-with-icon {:url "/assets/icons/privacy-policy2.svg"
                      :color "#d9da6b"})
    [:&
     [:h2.title
      {:color "#7ab5e2"}]]]])
