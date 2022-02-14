(ns memefactory.styles.pages.get-dank
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.media :refer [for-media-max]]
            [memefactory.styles.component.buttons :refer [get-dank-button]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.get-dank-page
   [:.spinner-outer {:margin-left :auto
                     :margin-right :auto
                     :margin-top (em 4)}]
   [:.get-dank-box
    (panel-with-icon {:url "/assets/icons/dank-logo.svg"
                      :color :purple})

    [:h2.title
     {:padding-left (em 2)
      :padding-right (em 2)}]

    [:h3.title
     {:white-space :normal
      :padding-left (em 2)
      :padding-right (em 2)}]

    [:.icon
     {:background-size [(em 4) (em 4)]
      :background-position-x (em 0.2)}]

    [:.body
     {:min-height (em 9)}
     [:.form
      {:display :flex
       :flex-direction :row
       :font-size (px 14)
       :padding-top (em 2)
       :padding-left (em 1)
       :padding-right (em 1)
       :height (em 4)
       :justify-content :center}
      [:.labeled-input-group
       {:padding-left (em 1)
        :padding-right (em 1)
        :width "100%"}
       [:label {:position "relative"
                :display "unset"}]
       ]
      [:.country-code {:width (em 6.1)}]
      (for-media-max :tablet
                     [:&
                      {:flex-direction :column}])]
     [:p {:margin-top (px 55)
          :margin-left (px 20)
          :margin-right (px 20)}]]

    [:.footer
     (get-dank-button)
     {:width "100%"
      :border :none}
     [:&:disabled
      {:opacity 0.3}]]]])
