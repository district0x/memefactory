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

  [:.input-group
   ["input[type=text]"
    {:box-sizing :border-box
     :border :none
     :border-bottom "1px solid #CCC"
     :width "100%"
     :position :relative}
    [:&:focus {:outline "none"}]
    [:&:focus+.help-block:before
     {:transform "scale(1)"}]]
   
   [:.help-block
    {}
    [:&:before
     {:content "''"
      :background-color :dodgerblue
      :display :inline-block
      :height (px 2)
      :left (em 0)
      ;; :margin-top: -4px;
      :position :relative
      :top (em -1);
      :transform "scale(0, 1)"
      :transition "all 0.2s linear"
      :width "100%";;(px 202)
      }]]])
