(ns memefactory.styles.component.compact-tile
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-keyframes]]
            [garden.units :refer [em pt px]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.component.buttons :refer [button]]
            [memefactory.styles.component.overflow :refer [of-ellipsis]]))

(def bar-height (px 50))
(def card-aspect-ratio 1.5)
(def card-width 290)
(def card-height (int (* card-aspect-ratio card-width)))

(def animation-speed "0.35s")
(def animation-timing "ease-in")

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
  {:-webkit-animation-duration animation-speed
   :animation-duration animation-speed
   :animation-fill-mode :both
   :animation-timing-function animation-timing
   :-webkit-transform-style :preserve-3d
   :transform-style :preserve-3d})

(def overlay-background (str "linear-gradient(to bottom, "
                             (color :meme-bg) " 0%,"
                             (color :meme-bg) " 100%)"))

(def overlay-background-footer (str "linear-gradient(to bottom, "
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

  (at-keyframes
   :fade-out
   [:from {:opacity 1.0}]
   [:to {:opacity 0.0}])

  ;; Flipping Front Forwards (not facing)
  (at-keyframes
   :flipping-front-not-facing
   [:from {:transform "rotateY(0deg);"}]
   [:to {:transform "rotateY(180deg);"}])

  ;; Flipping Front Backwards (facing)
  (at-keyframes
   :flipping-front-facing
   [:from {:transform "rotateY(180deg);"}]
   [:to {:transform "rotateY(0deg);"}])

  ;; Flipping the Back Forwards (facing)
  (at-keyframes
   :flipping-back-facing
   [:from {:transform "rotateY(-180deg);"}]
   [:to {:transform "rotateY(0deg);"}])

  ;; Flipping the Back Backwards (not facing)
  (at-keyframes
   :flipping-back-not-facing
   [:from {:transform "rotateY(0deg);"}]
   [:to {:transform "rotateY(-180deg);"}])

  ;;
  ;; Main Tile Styling
  ;;

  ;; General Fade In Animation
  [:.initial-fade-in-delay
   {:animation-name :initial-fade-in
    :-webkit-animation-delay "0.35s"
    :animation-delay "0.35s"
    :-webkit-animation-duration "0.50s"
    :animation-duration "0.50s"
    :animation-fill-mode :both}]

  [:.initial-fade-in
   {:animation-name :initial-fade-in
    :-webkit-animation-duration "0.50s"
    :animation-duration "0.50s"
    :animation-fill-mode :both}]

  [:.flippable-tile
   {:display :grid
    :perspective (px 1280)
    :-webkit-transform-style :preserve-3d
    :transform-style :preserve-3d
    :-webkit-backface-visibility :hidden
    :backface-visibility :hidden
    :cursor :pointer
    :background-color "white"
    :border-radius (em 1)
    :overflow :visible
    :grid-template-areas "'fill'"
    :grid-template-rows (px card-height)
    :grid-template-columns (px card-width)
    :width (px card-width)
    :height (px card-height)}

   [:&.mobile
    {:perspective :none
     :-webkit-transform-style :flat
     :transform-style :flat}]

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
      :transform "rotate(5px);"
      :grid-area "fill"
      :animation-name :flipping-back-not-facing
      :-webkit-backface-visibility :hidden
      :backface-visibility :hidden}
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
   {:display :grid
    :grid-template-areas "'fill'"
    :grid-template-columns (px card-width)
    :grid-template-rows (px card-height)
    :height (px card-height)
    :width (px card-width)
    :border-radius (em 1)
    :overflow :hidden}
   [:a {:color :white}]
   [:.overlay
    (merge
     {:display :grid
      :grid-area :fill
      :height (px card-height)
      :width (px card-width)
      :color (color :meme-info-text)
      :grid-template-areas "
                'numbers-info .           details-button'
                'logo         logo        logo'
                'data         data        data'
                'hard-rule    hard-rule   hard-rule'
                'description  description description'
                'input        input       input'"
      :grid-template-rows "40px 60px 165px 0.8em 1fr 87px"
      :grid-template-columns "1fr 1fr 1fr"
      :background overlay-background}
     no-select-style)

    [:.logo
     {:grid-area :logo
      :display :flex
      :align-items :center
      :justify-content :center}]

    [:.details-button
     {:grid-area :details-button
      :margin-top (em 0.8)
      :margin-right (em 0.8)}
     [:span
      {:font-size (em 0.9)
       :text-decoration :underline
       :color :white}]]

    [:.numbers-info
     (font :bungee)
     {:color (color :yellow)
      :padding-top (px 15)
      :margin-left (px -18)
      :grid-area :numbers-info}
     [:.registry-number
      {:font-size (em 1.2)}]
     [:.card-number
      {:font-size (em 0.9)}]]

    [:.meme-data
     {:grid-area :data
      :width (px card-width)
      :text-align :center
      :margin 0
      :margin-top (em 0.1)
      :padding-left (em 1.2)
      :padding-right (em 1.2)
      :font-size (pt 13)
      :line-height (em 1.8)
      :list-style :none}

     [:>li
      (of-ellipsis)
      {:font-size (em 0.7)
       :padding-top (em 0.1)
       :padding-right (em 0.5)
       :padding-left (em 0.5)
       :padding-bottom (em 0.1)}

      [:label {:font-weight :bold
               :padding-right (em 0.3)}]

      [:span {:overflow :hidden
              :text-overflow :ellipsis
              :white-space :nowrap}]]]

    [:hr
     {:grid-area :hard-rule
      :margin (em 0.4)
      :border 0
      :height 0
      :border-top "1px solid rgba(0, 0, 0, 0.1)"
      :border-bottom "1px solid rgba(255, 255, 255, 0.3)"}]

    [:.description
     {:display :flex
      :justify-content :center
      :align-items :center
      :grid-area :description
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
      [:&.buy:after :&.vote:after
       {:content "' '"
        :height (em 1)
        :width (em 1)
        :display :inline-block
        :background-position "0 0.17em"
        :margin-left (em 0.7)
        :background-size "1em, 1em"
        :background-repeat "no-repeat"
        :background-image "url(/assets/icons/arrow-white-right.svg)"}]]]]

   [:.remove-purple-footer
    {:background (str overlay-background " !important")}]

   [:.meme-placeholder
    {:grid-area :fill
     :display :flex
     :justify-content :center
     :align-items :center}

    [">*"
     (merge
      {:max-width (px 65)
       :height (px 42)}
      no-select-style)]]

   [:.meme-image
    (merge
     {:grid-area :fill
      :width (px card-width)
      :height (px card-height)
      :margin :auto}
     no-select-style
     no-drag-style)]

   [:video.meme-image
    {:object-fit :fill}]

   [:div.video-thumbnail
    {:position :relative}
    [:i.play-button
     {:background-image "url('/assets/icons/play-button.svg')"
      :position :absolute
      ; right-bottom corner:
      :bottom (px 20)
      :right (px 20)
      ; or center:
      ;:top "50%"
      ;:left "50%"
      ;:transform "translate(-50%, -50%)"
      ;
      :height (px 45)
      :width (px 45)
      :border-radius "50%"
      :background-color "rgba(0,0,0,0.5)"}]
    [:i.play-button:hover {:filter "invert(25%)"
                           :background-color "rgb(0,0,0)"}]]]

  [:.compact-tile-container
   {:display :flex
    :width "100%"}]

  [:.compact-tile
   {:background (color :meme-panel-bg)
    :width (px card-width)
    :margin-top (em 1)
    :margin-bottom (em 1)
    :display :block
    :position :relative
    :text-align :center}

   [:.footer
    {:position :relative
     :bottom 0
     :cursor :pointer
     :line-height (em 1.2)
     :font-size (em 0.9)
     :color (color :menu-text)}

    [:.token-id :.title :.meme-number {:display :inline-block}]

    [:.token-id {:padding-right (em 0.3)}]

    [:.title :.number-minted :.price :.token-id
     {:text-align :center
      :color (color :meme-tile-footer)}]

    [:.title {:margin-top (em 1)
              :font-weight :bold
              :cursor :pointer}]

    [:.number-minted :.cards-left
     {:margin-top (em 0.2)}]

    [:.price
     (font :bungee)
     {:margin-top (em 0.2)}]]]

  [:.image-tape-container
   {:overflow :hidden
    :grid-area :fill
    :background-color "rgba(0, 0, 0, 0.3)"
    :z-index 1}

   [:&:hover
    {:animation-name :fade-out
     :animation-duration "0.11s"
     :animation-delay "0.001s"
     :animation-timing-function :linear
     :animation-fill-mode :both}]

   [:.image-tape
    {:display :flex
     :position :relative
     :align-items :center
     :justify-content :center
     :top (px (- (/ card-height 2) 45))
     :right (px (- (/ card-width 2) 5))
     :background-color (color :rank-yellow)
     :height (px (* card-height 0.15))
     :transform "rotate(-12deg)"
     :width (px (* card-width 2.01))}
    [:>*
     (font :bungee)
     {:overflow :hidden
      :color (color :violet)
      :font-size (em 2.01)}]]])
