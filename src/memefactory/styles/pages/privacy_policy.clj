(ns memefactory.styles.pages.privacy-policy
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
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
