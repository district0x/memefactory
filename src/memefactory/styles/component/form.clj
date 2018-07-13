(ns memefactory.styles.component.form
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [memefactory.styles.base.fonts :refer [font]]
   [garden.units :refer [px em pt]]))

(defstyles core
  [:.dropzone
   {:width (px 200)
    :height (px 200)
    :background-color :grey}
   [:img {:width (px 200)}]]
  [:.chip-input
   {:display :flex
    :color (color :menu-text)}
   [:ol.chips {:margin-top 0
               :margin-bottom (em 0.3)
               :padding-left 0
               :display :flex}
    [:li
     ;; (font :filson)
     {:list-style-type :none
      :padding-right (em 1)
      :border-width (px 1)
      :border-radius "0.3em"
      :margin-left (em 0.3)
      :margin-right (em 0.3)
      :border-style :solid
      :border-color "#CCC"
      :padding "0.4em 1em 0.4em 1em"
      :background-color (color :search-input-bg)}
     [:span
      ;;{:font-size (px 12)}
      [:&:last-child
       {:padding-right (em 0.4)
        :font-style :italic
        :display :none
        :padding-left (em 0.4)}]]]]
   [:.autocomplete-input
    {:display :inline
     :width "100%"}
    ["input[type=text]"
     {:line-height (em 2.5)
      }]
    [:ol.options
     {:position :absolute
      :border-radius "0 0 0.3em 0.3em"
      :padding-left (em 0)
      :margin-top 0
      :margin-left (em 1)
      :border-width (px 1)
      :border-style :solid
      :border-color "#CCC"
      :background-color "white"}
     [:li
      ;; (font :filson)
      {:list-style-type :none
       :padding "0.4em 1em 0.4em 1em"}
      [:&.selected {:background-color (color :search-input-bg)
                    :border-radius (em 0.3)}]]]
    [:.help-block
     {:position :absolute}]]]
  [:.chip-input.focused+.help-block:before
   {:transform "scale(1)"}]

  [:.input-group
   ["input[type=text]"
    ;; (font :filson)
    {:box-sizing :border-box
     :padding-left (em 0.3)
     :border :none
     :color (color :menu-text)
     :line-height (em 1.95)
     ;; :border-bottom "1px solid #CCC"
     :width "100%"
     :position :relative}
    [:&:focus {:outline "none"}]
    [:&:focus+.help-block:before
     {:transform "scale(1)"}]]
   [:.help-block
    {:border :none
     :border-top "1px solid #CCC"}
    [:&:before
     {:content "''"
      :background-color :dodgerblue
      :display :inline-block
      :height (px 2)
      :left (em 0)
      ;; :margin-top: -4px;
      :position :relative
      :top (em -0.95);
      :transform "scale(0, 1)"
      :transition "all 0.2s linear"
      :width "100%";;(px 202)
      }]]]

  [:select
   {:overflow :hidden
    :width "100%"
    :border-radius "0.3em"
    :padding "0.4em"
    :color (color :menu-text)
    :-webkit-appearance :none
    :-moz-appearance :none
    :background-size ["0.6em"  "0.6em"]
    :background-position "right 0.5em center";
    :background-repeat :no-repeat
    :background-image "url('/assets/icons/dropdown.png')"
    }]
  )
