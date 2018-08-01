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
            [clojure.string :as str]))

(defstyles core
  [:.dank-registry-submit-page
   [:.submit-header
    {:padding-top (em 2)
     :margin-right (em 6)
     :margin-left (em 6)}
    [:.submit-info
     (panel-with-icon {:url "/assets/icons/memesubmiticon.png"
                       :color :purple})
     [:.get-dank-button
      (get-dank-button)]]]
   [:.upload
    {:display :grid
     :grid-template-columns "50% 50%"
     ;; :grid-column-gap (em 2)
     :padding-top (em 2)
     :margin-right (em 6)
     :margin-left (em 6)}
    (for-media-max :tablet
                   [:&
                    {:grid-template-columns "100%"}])
    [">div"
     {:box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :background-color (color :white)
      :border-radius "1em"
      :position :relative
      :padding (em 2)}
     [:&.image-panel {:margin-right (em 1)}
      (for-media-max :tablet
                     [:&
                      {:margin-right 0}])
      [:.input-group {:width (px 200)
                      :right 0
                      :left 0
                      :margin-left :auto
                      :margin-right :auto}]
      [:label.file-input-label
       {:right 0
        :left (em 2)
        :position :absolute
        :margin-right :auto
        :margin-left 0
        :padding-left (em 1)
        :line-height (em 2)}
       (button {:color :meme-buy-button})]
      ["input[type=file]"
       {:display :none}
       #_{:right 0
        :left (em 2)
        :position :absolute
        :margin-right :auto
        :margin-left 0}
       #_(button {:color :meme-buy-button})]]
     [:&.form-panel
      {:margin-left (em 1)}
      (for-media-max :tablet
                     [:&
                      {:margin-left 0
                       :margin-top (em 2)}])
      [:.submit {:position :relative
                 :justify-content :center
                 :align-items :center
                 :display :flex}
       [:button
        {:right 0
         :left 0
         :margin-right :auto
         :margin-left 0}
        (button {:color :meme-buy-button})]
       [:.dank {:display :block
                :vertical-align :middle}]]]]]])
