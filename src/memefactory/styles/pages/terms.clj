(ns memefactory.styles.pages.terms
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.terms-page
   [:.panel {:margin-bottom (em 2)}]
   [:.body {:font-size (px 15)
            :padding (em 2)}]
   [:.terms
    (panel-with-icon {:url "/assets/icons/terms-of-use2.svg"
                      :color "#c3e4f3"})
    [:&
     [:h2.title
      {:color "#fa9456"}]]]])
