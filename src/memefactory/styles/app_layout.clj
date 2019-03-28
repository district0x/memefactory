(ns memefactory.styles.app-layout
  (:require [clojure.string :as s]
            [garden.def :refer [defstyles]]
            [garden.units :refer [px em]]
            [garden.color :refer [transparentize]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.grid :refer [grid-columns]]
            [memefactory.styles.base.media :refer [for-media-max]]))

(defstyles core
  [:.app-container
   {:display :grid
    :min-height (em 90)
    :-webkit-font-smoothing :antialiased
    :-moz-osx-font-smoothing :grayscale}
   (grid-columns "200px" "1fr")
   (for-media-max :tablet
                  [:&
                   (grid-columns "100%")])
   [:div.no-items-found {:color (transparentize (color :menu-text) 0.5)
                         :width "100%"
                         :text-align :center
                         :padding-top (px 230)
                         :padding-bottom (px 230)}]
   [:.meme-comment {:color (color :pink)
                    :font-style :italic}]
   [:.dank-wrapper
    {:display :inline-flex
     :align-items :center
     :justify-content :center}
    [:span]
    [:img {:width (px 35)}]]
   [:img.dank-logo-small {:width (px 35)}]
   [:a.address {:cursor :pointer
               :color (color :menu-text)}
    [:&.active-address {:color (color :purple)}]]
   [:div.notification
    {:color (color :meme-info-text)
     :background-color (color :deep-purple)
     :position :fixed
     :bottom 0
     :right "30px"
     :z-index 9999
     :display :flex
     :align-items :center
     :justify-content :center
     :padding "10px 20px"
     :background-size "100% auto"
     :transition "transform 450ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"
     :will-change :transform
     :overflow :hidden
     :transform "scaleY(0)"
     :transform-origin "bottom"
     :min-width "300px"
     :min-height "48px"}

    [:&.open
     {:transform "scaleY(1)"}]

    [:&>*
     {:opacity 0
      :will-change :opacity
      :transition "opacity 500ms cubic-bezier(0.23, 1, 0.32, 1) 100ms"}]

    [:&.open>*
     {:opacity 1}]

    [:.notification-message
     {:font-size "15px"
      :overflow :hidden
      :white-space :nowrap
      :text-overflow :ellipsis}]]

   ])
