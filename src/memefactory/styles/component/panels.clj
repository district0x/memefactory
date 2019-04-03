(ns memefactory.styles.component.panels
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top border-bottom]]
            [memefactory.styles.base.colors :as c]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))

(defn panel-with-icon [{:keys [url color]}]
  [:&
   {:background (c/color :meme-panel-bg)
    :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
    :border-radius "1em 1em 1em 1em"
    :position :relative}
   [:.icon
    (for-media-max :large
                   [:&
                    {:margin-right :auto
                     :margin-left :auto
                     :right 0
                     :left 0
                     :position :static}])
    {:display :block
     :background-size [(em 4) (em 4)]
     :background-repeat :no-repeat
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :border-radius "0em 0em 1em 1em"
     :margin-left (em 2)
     :margin-top (em 0)
     :position :absolute
     :height (em 4)
     :width (em 4.3)}]
   [:h2.title
    (font :bungee)
    (for-media-max :large
                   [:&
                    {:font-size (px 19)}])
    {:width "100%"
     :position :relative
     :font-size (px 23)
     :margin-top (em 0.3)
     :margin-bottom 0
     :padding-top (em 0.7)
     :text-align :center}
    [:&.secondary
     {:padding-top (em 1)
      :padding-bottom (em 1)
      :margin-top (em 0)}]]
   [:h3.title
    {:white-space :unset
     :margin-top (px 10)
     :position :relative
     :color (c/color :section-subcaption)
     :font-size (px 13)
     :text-align :center
     :padding-left (px 5)
     :padding-right (px 5)}]

   [:.icon {:background-color (c/color color)
            :background-position-x (em 1)
            :background-position-y :center
            :background-image (str "url('" url "')")}]
   [:h2.title {:color (c/color color)}]
   ])

(defn tabs []
  [:&
   {:display :flex
    :margin-top (em 2)
    :margin-bottom (em 0)
    :height (em 3)
    ;; :line-height (em 3)
    :flex-wrap :wrap
    :justify-content :flex-start}
   (for-media-max :tablet
                  [:&
                   {:justify-content :space-evenly
                    :margin-right (em 2)
                    :margin-left (em 2)}])
   [">div"
    (for-media-max :tablet
                   [:&
                    {:padding-right (em 0.2)
                     :padding-left (em 0.2)}])
    {:padding-right (em 3)}
    [:a
     {:color (c/color :section-subcaption)
      :padding-bottom (em 0.2)
      :cursor :pointer}]
    [:&.selected
     [:a
      (border-bottom {:color (c/color :pink)
                      :width (px 2)})]]]])
