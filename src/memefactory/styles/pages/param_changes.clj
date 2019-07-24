(ns memefactory.styles.pages.param-changes
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
            [memefactory.styles.component.panels :refer [panel-with-icon tabs]]
            [memefactory.styles.component.buttons :refer [button get-dank-button vote-button-icon]]))

(defstyles core
  [:.param-change-page
   {:display :grid
    :grid-row-gap (px 50)}
   [:h2 {:color (color :redish)
          :text-transform :uppercase
         :font-size (px 19)
         :margin-bottom (px 0)}
    (font :bungee)]
   [:h3 {:font-size (px 14)
         :margin-top (px 5)}]

   [:.panel {:box-shadow "0.3em 0.3em 0 0 rgba(0,0,0,0.05)"
             :border-radius (em 1)
             :background-color :white}
    [:&.open   {:max-height (px 10000)}]
    [:&.closed {:max-height (px 70)}]]

   [:.body {:font-size (px 15)
            :padding (em 2)}]

   [:.header-box
    (panel-with-icon {:url "/assets/icons/parameters.svg"
                      :color (color :sky-blue)})
    [:&
     [:h2.title
      {:color (color :redish)}]]]

   [:.param-table-panel {:overflow :hidden}
    [:.param-table
     {:border-spacing 0
      :border-collapse :collapse
      :width "100%"
      :color (color :menu-text)}
     [:thead
      (for-media-max :tablet
                     [:th.optional {:display :none }])
      [:th {:background (color :yellow)
            :padding-left (px 15)
            :padding-top (px 20)
            :padding-bottom (px 20)
            :text-align :left}

       [:.collapse-icon
        {:background-color :white
         :background-position-x (px 8)
         :background-size [(em 1) (em 1)]
         :background-position-y :center
         :width (px 30)
         :height (px 30)
         :background-image (str "url('/assets/icons/dropdown.png')")
         :border-radius (em 10)
         :background-repeat :no-repeat}
        [:&.flipped {:transform "scaleY(-1)"}]]]
      ]
     [:tbody
      {:background :white}
      [:&:before
       #_{:content "'@'"
          :display :block
          :line-height (px 20)
          :text-indent (px -999999)}]
      ["tr:nth-child(odd)" {:background (color :light-light-grey)}]
      ["tr:not(:last-child)" {:border-bottom "1px solid lightgrey"}]
      [:tr
       (for-media-max :tablet
                      [:&
                       {:display :flex
                        :flex-direction :column}])
       [:td {:padding (px 15)}
        (for-media-max :tablet [:& {:padding-bottom (px 5)
                                    :padding-top (px 5)}])
        [:.param-h {:font-weight :bold
                    :display :none}
         (for-media-max :tablet
                        [:&
                         {:display :block}])]
        [:.param-title {:font-weight :bold}]]]]]]


   [:.change-submit-form
    [:.help-block {:display :none}]
    [:.options
     {:max-width (px 300)}]

    [:.form {:color (color :menu-text)
             :display :grid
             :margin-top (px 10)
             :margin-bottom (px 30)
             :grid-template-rows "50px 50px"
             :grid-column-gap (px 10)
             :grid-template-columns "50% 50%"
             :grid-template-areas (str "'input-old textarea'\n"
                                       "'input-new textarea'\n")}
     (for-media-max :tablet
                    [:&
                     {:display :block}])
     [:.input-old {:grid-area :input-old}
      [:.current-value {:color (color :grey)
                        :border-bottom "1px solid grey"
                        :width "100%"
                        :padding-top (px 5)
                        :padding-bottom (px 5)
                        :display :inline-block}]]
     [:.input-new {:grid-area :input-new}
      [:.input-group {:display :inline-block
                      :width "100%"
                      :border-bottom "1px solid grey"}]]
     [:.textarea {:grid-area :textarea}
      [:.input-group {:border-bottom :unset}
       [:textarea {:resize :none
                   :padding (em 0.5)
                   :height (em 6)
                   :width "100%"}]]
      [:label {:margin-bottom (px 5)
               :display :block}]
      [:.dank {:text-align :right}
       [:span.dank {:margin-right (px 5)}]]]

     [:.unit {:position :absolute
              :font-size (px 13)
              :right (px 44)
              }]
     [:.label-under {:font-size (px 12)}]]

    [:.footer
     (get-dank-button)
     {:width "100%"
      :border :none}
     [:&:disabled
      {:opacity 0.3}]]]


   [:.tabs-titles
    (tabs)]

   [:ul {:list-style :none :padding-left 0}]

   [:.proposal-list

    [:li {:margin-bottom (px 15)}

     [:.proposed-change-panel {}
      [:.header {:display :flex
                 :justify-content :flex-end
                 :min-height (px 30)}
       [:.icon {:background-position-x (px 8)
                :background-size [(em 1) (em 1)]
                :background-position-y :center
                :width (px 30)
                :height (px 30)
                :border-radius (em 10)
                :background-repeat :no-repeat
                :position :relative
                :right (px -10)
                :top (px -10)}
        [:&.applied {:background-image (str "url('/assets/icons/check.svg')")
                     :background-color (color :rare-meme-icon-bg)}]
        [:&.not-applied {:background-image (str "url('/assets/icons/cross.svg')")
                         :background-color (color :pink)
                         :color :white}]]]
      [:.proposed-change {:color (color :menu-text)
                          :padding-left (em 2)
                          :padding-right (em 2)
                          :padding-bottom (em 2)
                          :display :grid
                          :grid-template-columns "70% 30%"
                          :min-height (px 200)}
       (for-media-max :tablet [:& {:display :block}])
       [:h4 {:font-weight :bold :margin-bottom (px 5)}]
       [:.info
        [:li {:margin-bottom (px 5)}]
        [:.comment {:font-style :italic}
         [:&:before {:content "'\"'"}]
         [:&:after {:content "'\"'"}]]

        [:.info-body
         {:margin-top (px 15)
          :padding-right (px 30)
          :display :grid
          :grid-template-columns "50% 50%"}
         (for-media-max :tablet [:& {:display :block}])
         [:.attr [:label {:margin-right (px 4)}]]]
        [:.section2 [:span {:overflow :hidden
                            :display :inline-block}]
         [:.address {:text-overflow :ellipsis
                     :width "50%"}]
         [:.challenger {:margin-top (px 10)}]]]
       [:.action
        {:border-left "1px solid"
         :padding (em 1)}
        (for-media-max :tablet [:& {:border-left :none}])]


       [:.challenge-action
        [:textarea {:resize :none
                   :padding (em 0.5)
                   :height (em 7)
                    :width "100%"}]
        [:.help-block {:display :none}]
        [:.footer {:display :flex
                   :justify-content :space-evenly
                   :align-items :center}
         [:button (button {:background-color :purple
                           :color :white
                           :width (em 12)
                           :height (em 3.3)})]
         [:.dank {:display :inline-block}
          [:.dank {:margin-right (px 5)}]]]]



       [:.reveal-action {:display :flex
                         :align-items :center
                         :flex-flow :column}
        [:.icon {:background-size [(em 6) (em 6)]
                 :background-position-y :center
                 :width (px 100)
                 :height (px 100)
                 :background-repeat :no-repeat
                 :background-image (str "url('/assets/icons/mememouth.png')")}]
        [:button {:display :block}
         (button {:background-color :purple
                  :color :white
                  :width (em 12)
                  :height (em 3.3)})]]

       [:.vote-action {:display :grid
                       :grid-template-rows "100px 100px 100px"
                       :text-align :center
                       :max-width (px 200)
                       :margin :auto}
        [:.vote-input {:display :grid
                       :height (px 30)
                       :grid-template-columns "80% 20%"
                       :border-bottom "1px solid"
                       :margin-bottom (em 1)}
         [:span {:margin-top (px 8)}]
         [:.help-block {:display :none}]]
        [:.vote-yes
         [:button
          {:margin-bottom (em 2)}
          (button {:background-color :rare-meme-icon-bg
                   :color :violet
                   :height (em 3)
                   :line-height (em 3)
                   :width "100%"})
          (vote-button-icon -1 -4)
          {:white-space :nowrap
           :padding-right (em 1)
           :padding-bottom (em 0.4)
           :padding-left (em 1)}]]
        [:.vote-no
         [:button
          (button {:background-color :random-meme-icon-bg
                   :color :violet
                   :height (em 3)
                   :line-height (em 3)
                   :width "100%"})
          (vote-button-icon 0 -3)
          {:white-space :nowrap
           :padding-right (em 1)
           :padding-bottom (em 0.4)
           :padding-left (em 1)}
          [:&:before {:transform "scaleX(-1) scaleY(-1)"}]]]]


       [:.apply-change-action {:display :flex
                               :align-items :center
                               :flex-flow :column}
        [:.info {:text-align :center
                 :margin-top (px 20)}]
        [:button
         {:margin-top (px 20)}
         (button {:background-color :purple
                  :color :white
                  :width (em 12)
                  :height (em 3.3)})]]


       [:.claim-action {:display :flex
                        :flex-flow :column
                        :align-items :center}
        [:ul {:margin-top (px 15)
              :margin-bottom (px 15)}
         [:li {:text-align :center
               :margin-bottom (px 2)}]]
        [:button (button {:background-color :purple
                          :color :white
                          :width (em 12)
                          :height (em 3.3)})]]]]]]])
