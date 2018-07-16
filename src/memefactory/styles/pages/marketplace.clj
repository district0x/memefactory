(ns memefactory.styles.pages.marketplace
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
            [clojure.string :as str]))

(defstyles core
  [:.marketplace-page
   [:.marketplace
    {:padding-top (em 4)}
    [:.search-form
     (for-media-max :tablet
                    [:&
                     {:margin-right (em 1)
                      :margin-left (em 1)}])
     {:margin-right (em 6)
      :border-radius "1em 1em 1em 1em"
      :background-color (color :meme-panel-bg)
      :background-size [(em 10) (em 10)]
      :background-repeat :no-repeat
      :background-position-y :bottom
      :background-position-x :right
      :background-image "url('/assets/icons/search-background.png')"
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :margin-left (em 6)}
     #_[:>div
      {:position :relative}
      (border-top {:color (color :border-line)})
      (for-media-max :tablet
                     [:&
                      {:border-radius "1em 1em 1em 1em"
                       :margin-bottom (em 2)}])
      ]
     [:.header :h2
      (font :bungee)
      (for-media-max :tablet
                     [:&
                      {:margin-top (em 4)
                       :font-size (px 19)}])
      {:white-space :nowrap
       :position :relative
       ;; :color (color :section-caption)
       :font-size (px 25)
       :margin-top (em 0.3)
       :margin-bottom (em 0.1)
       :text-align :center}]
     [:.header :h3
      {:white-space :nowrap
       :margin-top (em 0.1)
       :position :relative
       :color (color :section-subcaption)
       :font-size (px 15)
       :text-align :center}]
     [:.icon
      (for-media-max :tablet
                     [:&
                      {:margin-right :auto
                       :margin-left :auto
                       :right 0
                       :left 0
                       }])
      {:display :block
       :z-index 1
       :background-size [(em 4) (em 4)]
       :background-repeat :no-repeat
       :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
       :border-radius "0em 0em 1em 1em"
       :margin-left (em 2)
       :margin-top (em 0)
       :position :absolute
       :height (em 4)
       :width (em 4)}]

     [:.form
      {:display :grid
       :margin-left (em 2)
       :padding-top (em 2)
       :grid-template-columns "60% 20% 20%"
       :grid-template-rows "2.5em 3em 2em"
       :grid-template-areas
       (str
        "'name dropdown .'\n"
        "'chip chip .'\n"
        "'. checkbox .'\n")}
      [:.name {:grid-area :name}
       [:.help-block {:height :2px}]]
      [:.options {:grid-area :dropdown}]
      [:.ac-options {:grid-area :chip}]
      [:.check-cheapest {:grid-area :checkbox}
       [:label {:font-size "8px"
                :position :absolute
                :margin-left (em 2)
                :margin-top (em 0.6)
                :line-height "0.5em"}]
       [:.help-block {:display :none}]]]]
    [:.search-form
     (for-media-max :tablet
                    [:.more {:background-color (color :new-meme-icon-bg)}])
     [:.icon {:background-color (color :new-meme-icon-bg)
              :background-image (str "url('/assets/icons/marketplaceicon.png')")}]
     [:h2.title {:color (color :new-meme-icon-bg)}]]]

   [:.tiles
    {:display :block
     :margin-top (em 2)
     :padding-top (em 2)
     :padding-bottom (em 2)
     :margin-right (em 6)
     :margin-left (em 6)
     :background-color (color :meme-panel-bg)
     :border-radius "1em 1em 1em 1em"}
    [">div>div"
     {:display :flex
      :flex-wrap :wrap
      :justify-content :space-evenly}]]
   ])
