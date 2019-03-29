(ns memefactory.styles.pages.dankregistry.challenge
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
            [memefactory.styles.component.panels :refer [panel-with-icon]]
            [memefactory.styles.component.buttons :refer [get-dank-button button]]
            [clojure.string :as str]))

(defstyles core
  [:.dank-registry-challenge
   [:.challenge-header
    {:margin-bottom (em 6)}

    (for-media-max :tablet
                   [:&
                    {:margin-bottom (em 0)}])
    [:.challenge-info
     (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                       :color :sky-blue})
     [:.get-dank-button
      (get-dank-button)]]]
   [:.challenge-controls
    {:text-align :center}
    [:.help-block {:border-top :none}]
    [:div.vs {:display :grid
              :grid-template-columns "1fr 1fr 1fr"
              :align-items :center
              :text-align :center
              :width (em 10)
              :margin-left :auto
              :margin-right :auto}
     [:img {:object-fit :cover
            :max-width "100%"}]
     [:span (font :bungee)
      {:color (color :rare-meme-icon-bg)}]]
    [:textarea {:background-color (color :main-content-bg)
                :border-radius (em 1)
                :margin-top (em 2)
                :height (em 8)
                :border :none
                :resize :none
                :width (em 16)
                :padding (em 1)
                :color :grey}]

    [:.not-enough-dank {:margin-top (em 1)
                        :color :red}]

    [:button.open-challenge {:margin-top (em 2)}
     [:&.disabled
      {:opacity 0.3}]]
    [:button
     {:margin-left :auto
      :margin-bottom (em 1)
      :margin-right :auto}
     (button {:color :white
              :background-color :purple
              :width (em 13)
              :height (em 3)})]]])
