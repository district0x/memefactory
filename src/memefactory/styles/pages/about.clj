(ns memefactory.styles.pages.about
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.about-page
   [:.panel {:margin-bottom (em 2)}]
   [:.body {:font-size (px 15)
            :padding (em 2)}]
   [:.about
    (panel-with-icon {:url "/assets/icons/about2.svg"
                      :color "#93bfd4"})
    [:&
     [:h2.title
      {:color "#05d36e"}]]]])
