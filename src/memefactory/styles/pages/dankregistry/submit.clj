(ns memefactory.styles.pages.dankregistry.submit
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
            [clojure.string :as str]
            [memefactory.styles.component.compact-tile :as compact-tile]))

(defstyles core
  [:.dank-registry-submit-page
   [:.submit-header
    [:.submit-info
     (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                       :color :sky-blue})
     [:.get-dank-button
      (get-dank-button)]]]
   [:.upload
    {:display :grid
     :grid-template-columns "50% 50%"
     ;; :grid-column-gap (em 2)
     :padding-top (em 4)
     }
    (for-media-max :computer
                   [:&
                    {:grid-template-columns "100%"}])
    [:.image-panel {:display :flex
                    :justify-content :center}]
    [">div"
     {:box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :background-color (color :white)
      :border-radius "1em"
      :position :relative
      :padding (em 2)}
     [:&.image-panel {:margin-right (em 1)}
      (for-media-max :computer
                     [:&
                      {:margin-right 0}])
      [:img {:width (px compact-tile/card-width)
             :height (px compact-tile/card-height)}]
      [:.input-group #_{ :margin-left :auto
                      :margin-right :auto}
       [:.dropzone {:width (px compact-tile/card-width)
                    :height (px compact-tile/card-height)}]
       [:.file-name {:display :none}]
       [:.help-block
        {:border-top :none
         :line-height (em 1)
         :margin-top (em -0.5)
         :margin-bottom (em 3)}]]
      [:label.file-input-label
       {:margin-right :auto
        :margin-left :auto
        :padding-top (em 1)
        :padding-left (em 1.9)
        :margin-top (em 2)
        :font-size (px 12)}
       (button {:background-color :purple
                :color :white
                :width (em 12)
                :height (em 3.3)})]
      ["input[type=file]"
       {:display :none}
       #_{:right 0
        :left (em 2)
        :position :absolute
        :margin-right :auto
        :margin-left 0}
       #_(button {:color :meme-buy-button})]]

     [:&.form-panel
      {:margin-left (em 1)
       :padding-top (em 9)
       :padding-left (em 3)
       :padding-right (em 3)}
      (for-media-max :computer
                     [:&
                      {:margin-left 0
                       :margin-top (em 2)
                       :padding-top (em 2)
                       :padding-left (em 2)
                       :padding-right (em 2)}])
      [:.chip-input
       [:.autocomplete-input
        ["input[type=text]"
         ;; {:padding-top 0}
         ]]]
      [:.max-issuance {:font-size (em 0.8)}]
      [:.submit {:display :flex
                 :margin-top (em 1)
                 :position :relative
                 :justify-content :center
                 :align-items :center}
       [:button
        {:margin-right :auto
         :margin-left 0
         :padding-top (em 0.2)
         :font-size (px 12)}
        (button {:background-color :purple
                 :color :white
                 :height (em 3.3)
                 :width (em 10)})]
       [:.dank {:flex 1
                :margin-left (em 1)}]]]]]])
