(ns memefactory.styles.component.buttons
  (:require [garden.units :refer [em px]]
            [memefactory.styles.base.colors :as c]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-max]]))

(defn button [{:keys [color background-color  width height line-height]}]
  [:&
   (font :bungee)
   (merge
     {:border-radius (em 2)
      :display :flex
      :align-items :center
      :justify-content :center
      :height (or height (em 2))
      :width (or width (em 8))
      :border-style "none"
      :color (c/color color)
      :cursor :pointer
      :background-color (c/color background-color)
      :white-space :nowrap}
     (when line-height
       {:line-height line-height}))
   [:&:disabled
    {:opacity 0.3}]

   [:.label
    {:display :flex
     :height (or height (em 2))
     :width (or width (em 8))
     :align-items :center
     :justify-content :center}

    [:img
     {:height (or height (em 1.8))
      :width (or height (em 1.8))}]]])

(defn get-dank-button []
  [:&
   (font :bungee)
   {:display :flex
    :align-items :center
    :justify-content :center
    :height (em 4)
    :cursor :pointer
    :color (c/color :white)
    :background (str "url('/assets/icons/gears-button-bg-l.png') left 1em center / 40% 60% no-repeat,"
                     "url('/assets/icons/gears-button-bg-r.png') right 1em center / 40% 60% no-repeat "

                     (c/color :purple))
    :border-radius "0 0 1em 1em"
    :text-align :center}
   (for-media-max :tablet
                  [:&
                   {:background (str "url('/assets/icons/gears-button-bg-l.png') left 1em center / 29% 29% no-repeat,"
                                     "url('/assets/icons/gears-button-bg-r.png') right 1em center / 29% 29% no-repeat "

                                     (c/color :purple))}])

   [:&.disabled
    {:opacity 0.3
     :cursor :initial}]

   [:span {:font-size (em 1.1)}]

   [:.dank-logo
    {:width (em 2.5)
     :height (em 2.5)}]

   [:.arrow-icon
    {:width (px 15)
     :height (px 15)}]])

(defn tag []
  [:&
   {:line-height 1
    :background-color (c/color :tags-grey)
    :color (c/color :menu-text)
    :margin (em 0.3)
    :border "0.5px solid rgba(224, 180, 240, 0.4)"
    :border-radius (em 2)
    :font-size (px 12)
    :padding "8px 20px"
    :display :inline-block}])

(defn vote-button-icon [top & [left]]
  [:&:before {:content "''"
              :background-image "url('/assets/icons/thumb-up.svg')"
              :background-size "20px 20px"
              :display :inline-block
              :background-repeat :no-repeat
              :width (px 20)
              :position :relative
              :margin-right (px 10)
              :height (px 20)
              :top (px top)
              :left (px (or left 0))}])
