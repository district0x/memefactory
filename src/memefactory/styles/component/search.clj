(ns memefactory.styles.component.search
  (:require [garden.units :refer [em px]]
            [memefactory.styles.base.colors :as c]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-max]]))

(defn search-panel [{:keys [background-panel-image
                            color
                            icon]}]
  [:&
   [:div.search-form
    (for-media-max :computer
                   [:&
                    {:background-image :none
                     :background-color (color :meme-panel-bg)}])
    {:position :relative
     :border-radius "1em 1em 1em 1em"
     :background-color (c/color :meme-panel-bg)
     :background-size [(em 16) (em 16)]
     :background-repeat :no-repeat
     :background-position "right -3em bottom -1.2em"
     :background-image (str "url('" background-panel-image "')")
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :padding-bottom (em 1)}
    [:.header :h2
     (font :bungee)
     (for-media-max :computer
                    [:&
                     {:font-size (px 19)}])
     {:white-space :nowrap
      :position :relative
      :color (c/color color)
      :font-size (px 23)
      :margin-top (em 0.3)
      :padding-top (em 0.2)
      :margin-bottom 0
      :text-align :center}]
    [:.header :h3
     {:white-space :unset
      :margin-top (px 10)
      :position :relative
      :color (c/color :section-subcaption)
      :font-size (px 13)
      :text-align :center
      :padding-left (px 5)
      :padding-right (px 5)}]
    [:.icon
     (for-media-max :computer
                    [:&
                     {:margin-right :auto
                      :margin-left :auto
                      :right 0
                      :left 0
                      :position :static
                      }])
     {:display :block
      :z-index 1
      :background-size [(em 4) (em 4)]
      :background-repeat :no-repeat
      :background-position-x (em 1)
      :background-position-y (em 0.7)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "0em 0em 1em 1em"
      :margin-left (em 2)
      :margin-top (em 0)
      :position :absolute
      :height (em 4)
      :width (em 4.3)}]

    [:.form
     {:display :grid
      :margin-left (em 2)
      :margin-right (em 2)
      :padding-top (em 2)
      :grid-template-columns "7fr minmax(180px, 3fr) 3fr"
      :grid-template-areas
      (str
       "'name dropdown .'\n"
       "'chip chip .'\n"
       "'. radios .'\n"
       "'. checkbox .'\n")
      :grid-column-gap (em 2)
      :color (c/color :section-subcaption)}
     (for-media-max :computer
                    [:& {:grid-template-rows "3em 3em 4em 6em"
                         :grid-template-columns "100%"
                         :grid-template-areas
                         (str
                          "'name'\n"
                          "'dropdown'\n"
                          "'chip'\n"
                          "'radios'\n"
                          "'checkbox'\n")}
                     [:.filled
                      [:label {:display :none}]]])

     [:.name {:grid-area :name}
      [:.help-block {:height :2px}]]
     [:.options {:grid-area :dropdown}
      [:.help-block {:display :none}]]
     [:.ac-options {:grid-area :chip}]
     [:.check-group
      {;; grid box properties
       :grid-area :checkbox
       :justify-self :end
       ;; container properties
       :display :flex}
      [:.single-check
       {:display :flex
        :justify-content :center
        :align-items :center
        :padding-left (em 0.4)
        :margin-top (em 1)}
       [:.input-group

        [:input {:-webkit-appearance :none
                 :-moz-appearance :none
                 :background-color :white
                 :border "1px solid #cacece"
                 :padding (px 6)
                 :border-radius (px 3)
                 :display :inline-block
                 :position :relative}
         [:&:checked {:background-color :black}]]]
       [:label {:font-size "10px"
                :flex 1
                :margin-left (em 0.6)
                :margin-bottom (em 0.2)
                :white-space :nowrap}]]
      [:.help-block {:display :none}]]
     [:.options-group
      {:margin-top (px 10)
       :grid-area :radios
       :justify-self :end
       :width (px 573)
       :display :flex}
      (for-media-max :computer
                     [:& {:justify-self :unset
                          :width :unset}])
      [:.radio {:display :inline-block
                :margin-left (px 10)}
       (for-media-max :computer
                      [:& {:display :block
                           :margin-left 0}])
       [:label {:margin-left (px 8)
                :font-size (px 12)}]]
      [:.help-block {:display :none}]]]]
   [:.search-form
    (for-media-max :computer
                   [:.more {:background-color (c/color :new-meme-icon-bg)}])
    [:.icon {:background-color (c/color color)
             :background-image (str "url('" icon "')")}]
    [:h2 {:color (c/color color)}]
    [:.chip-input
     [:input {:background-color :transparent}]]]])
