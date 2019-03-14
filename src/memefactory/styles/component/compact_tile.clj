(ns memefactory.styles.component.compact-tile
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media at-keyframes]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.component.buttons :refer [button]]
   [memefactory.styles.component.overflow :refer [of-ellipsis]]
   [garden.units :refer [px em pt]]))


(def bar-height (px 50))
(def card-aspect-ratio 1.5)
(def card-width 290)
(def card-height (int (* card-aspect-ratio card-width)))


(def animation-speed "0.35s")
(def animation-timing "linear")


(def no-select-style
  {:-webkit-touch-callout :none
   :-webkit-user-select :none
   :-khtml-user-select :none
   :-moz-user-select :none
   :-ms-user-select :none
   :user-select :none})


(def no-drag-style
  {:-webkit-user-drag :none
   :-khtml-user-drag :none
   :-moz-user-drag :none
   :-o-user-drag :none
   :user-drag :none})


(def default-animation-style
  {:animation-duration animation-speed
   :animation-fill-mode :both
   :animation-timing-function animation-timing})


(def overlay-background (str "linear-gradient(to bottom, " 
                             (color :meme-bg) " 0%,"
                             (color :meme-bg) " 80%,"
                             (color :meme-bg-bottom) " 80%,"
                             (color :meme-bg-bottom) " 100%)"))


(defstyles core

  ;;
  ;; Flippable Tile Animations
  ;;

  (at-keyframes
   :initial-fade-in
   [:from {:opacity 0.0}]
   [:to {:opacity 1.0}])

  ;; Flipping Front Forwards (not facing)
  (at-keyframes
   :flipping-front-not-facing
   [:from {:transform "rotateY(0deg);"
           :opacity 1.0}]
   ["50%" {:opacity 0.0}]
   [:to {:transform "rotateY(180deg);"
         :opacity 1.0}])

  ;; Flipping Front Backwards (facing)
  (at-keyframes
   :flipping-front-facing
   [:from {:transform "rotateY(180deg);"
           :opacity 1.0}]
   ["50%" {:opacity 0.0}]
   [:to {:transform "rotateY(0deg);"
         :opacity 1.0}])

  ;; Flipping the Back Forwards (facing)
  (at-keyframes
   :flipping-back-facing
   [:from {:transform "rotateY(90deg);"
           :opacity 0.0}]
   ["50%" {:transform "rotateY(90deg);"
           :opacity 1.0}]
   [:to {:transform "rotateY(0deg);"
         :opacity 1.0}])

  ;; Flipping the Back Backwards (not facing)
  (at-keyframes
   :flipping-back-not-facing
   [:from {:transform "rotateY(0deg);"
           :opacity 1.0}]
   ["50%" {:transform "rotateY(90deg);"
           :opacity 0.0}]
   [:to {:transform "rotateY(90deg);"
         :opacity 0.0}])

  
  ;;
  ;; Main Tile Styling
  ;;

  ;; General Fade In Animation
  [:.initial-fade-in-delay
   {:animation-name :initial-fade-in
    :animation-delay "1.50s"
    :animation-duration "0.50s"
    :animation-fill-mode :both}]

  [:.initial-fade-in
   {:animation-name :initial-fade-in
    :animation-duration "0.50s"
    :animation-fill-mode :both}]

  [:.flippable-tile
   {:display :grid
    :cursor :pointer
    :background-color "white"
    :transition "transform 0.1s ease"
    :border-radius (em 1)
    :overflow :hidden
    :grid-template-areas "'fill'"
    :width (px card-width)
    :height (px card-height)}
   
   [:&:hover
    {:transform "translateY(-4px)"
     :transition "transform 0.1s ease 0.1s"}]

   [:&:active
    {:transform "translateY(2px)"
     :transition "transform 0.1s linear"}]

   ;; Front of flippable card styling
   [:.flippable-tile-front
    (merge 
     {:z-index 9000
      :grid-area "fill"
      :animation-name :flipping-front-facing}
     default-animation-style)]
   
   ;; Back of flippable card styling
   [:.flippable-tile-back
    (merge
     {:z-index 9001
      :grid-area "fill"
      :animation-name :flipping-back-not-facing}
     default-animation-style)]]
  
  ;; Main Animation for flipping
  [:.flippable-tile.flipped
   [:.flippable-tile-front
    (merge
     {:animation-name :flipping-front-not-facing}
     default-animation-style)]
   
   [:.flippable-tile-back
    (merge
     {:animation-name :flipping-back-facing}
     default-animation-style)]]

  ;;
  ;; Inner Card Styling
  ;;

  [:.meme-card
   {:display :flex
    :height (px card-height)
    :width (px card-width)
    :align-items :center
    :justify-content :center
    :border-radius (em 1)
    :overflow :hidden}
   
   [:.overlay
    (merge
     {:display :grid
      :height (px card-height)
      :width (px card-width)
      :color (color :meme-info-text)
      :grid-template-areas "
                'logo'
                'data'
                'hard-rule'
                'description'
                'input'"
      :grid-template-rows "4em 1fr auto 3em 90px"
      :grid-template-columns (str card-width "px")
      :background overlay-background}
     no-select-style)
    
    [:.logo
     {:grid-area :logo
      :display :flex
      :align-items :center
      :justify-content :center
      :margin-top (em 2)}]
    
    [:.meme-data 
     {:grid-area :data
      :text-align :center
      :padding-left (em 1.2)
      :padding-right (em 1.2)
      :font-size (pt 13)
      :line-height (em 1.8)
      :list-style :none}
     
     [:>li
      (of-ellipsis)
      {:margin-top (em 0.3)
       :font-size (em 0.7)
       :padding-right (em 0.5)
       :padding-left (em 0.5)
       :margin-bottom (em 0.3)}

      [:label {:font-weight :bold
               :padding-right (em 0.3)}]

      [:span {:overflow :hidden
              :text-overflow :ellipsis
              :white-space :nowrap}]]]

    [:hr
     {:grid-area :hard-rule
      :margin (em 1)
      :border 0
      :height 0
      :border-top "1px solid rgba(0, 0, 0, 0.1)"
      :border-bottom "1px solid rgba(255, 255, 255, 0.3)"}]
    
    [:.description
     {:grid-area :description
      :font-style :italic
      :overflow :hidden
      :text-overflow :ellipsis
      :white-space :normal
      :padding-left (em 1.2)
      :padding-right (em 1.2)}]
    
    [:.input
     {:grid-area :input}
     
     [:button
      {:right 0
       :left 0
       :margin-right :auto
       :margin-left :auto
       :position "absolute"
       :bottom (em 1.8)}
      (button {:background-color :meme-buy-button
               :color :light-light-grey
               :width (em 9)
               :height (em 3)})
      [:&.buy:after {:content "' '"
                     :height (em 1)
                     :width (em 1)
                     :display :inline-block
                     :background-position "0 0.3em"
                     :margin-left (em 1)
                     :background-size "1em, 1em"
                     :background-repeat "no-repeat"
                     :background-image "url(/assets/icons/arrow-white-right.svg)"}]]]]

   [:.meme-placeholder
    {:display :flex
     :justify-content :center
     :align-items :center}

    [">*"
     (merge
      {:max-width (px 65)
       :height (px 42)}
      no-select-style)]]

   [:.meme-image
    (merge
     {:width (px card-width)
      :height (px card-height)
      :margin :auto}
     no-select-style
     no-drag-style)]]

  [:.compact-tile
   {:background (color :meme-panel-bg)
    :width (px card-width)
    :margin-top (em 1)
    :margin-bottom (em 1)
    ;; :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :block
    :position :relative
    :text-align :center}

   [:.footer
    {:position :relative
     :bottom 0
     :cursor :pointer
     :line-height (em 1.2)
     :font-size (em 0.9)}

    [:.token-id :.title {:display :inline-block}]

    [:.token-id {:padding-right (em 0.3)}]

    [:.title :.number-minted :.price :.token-id
     {:text-align :center
      :color (color :meme-tile-footer)}]

    [:.title {:margin-top (em 1)
              :font-weight :bold
              :cursor :pointer}]

    [:.number-minted {:margin-top (em 0.3)}]

    [:.price
     (font :bungee)
     {:margin-top (em 0.3)}]]])
