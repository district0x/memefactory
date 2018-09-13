(ns memefactory.styles.pages.memefolio.details
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.component.overflow :refer [of-ellipsis]]
            [memefactory.styles.component.buttons :refer [button tag vote-button-icon]]
            [memefactory.styles.base.borders :refer [border-top border-bottom]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [memefactory.styles.component.search :refer [search-panel]]
            [memefactory.styles.component.panels :refer [tabs]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.component.overflow :refer [of-ellipsis]]
            [clojure.string :as str]))

(defstyles core
  [:.meme-detail-page
   [:.address (of-ellipsis)]
   [:section.meme-detail
    {:padding (em 3)}
    [:.meme-info
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"
      :display :grid
      :grid-template-columns "34% 66%"
      :grid-template-rows "100%"
      :position :relative}
     (for-media-max :tablet
                    [:&
                     {:grid-template-columns "100%"
                      :margin-bottom (em 2)}])
     [:.meme-num
      (font :bungee)
      {:position "absolute"
       :display "block"
       :color (color :purple)
       :left (em 3.1)
       :padding (em 1)
       :font-size (em 1.1)
       :border-radius "0em 0em 1em 1em"
       :background-color (color :yellow)}]
     [:.meme-image
      {:padding (em 1)
       :border-radius "1em 1em 1em 1em"}]
     [:.registry
      {:padding (em 1)}
      [:h1
       (font :bungee)
       {:color (color :purple)}]
      [:.status
       {:display :flex
        :line-height (em 1)}
       [:&:before
        {:content "''"
         :height (em 2.2)
         :display :inline-block
         :width (em 2.2)
         :transform "scale(0.5, 0.5)"
         :background-repeat :no-repeat
         :background-position "center center";
         :background-image (str "url('/assets/icons/inregistry-icon.png')")}]
       [:label
        {:line-height (em 2.3)
         :font-weight :bold
         :color (color :menu-text)}]]
      [:.description, :.text
       {:color (color :menu-text)}]
      [:.tags
       [:button (tag)]]
      [:.buttons
       {:display :flex}
       (for-media-max :large
                      [:&
                       {:flex-direction :column}])
       [:button.search
        (button {:color :meme-buy-button})
        (for-media-max :tablet
                       [:&
                        {:min-width (em 15)}])
        {:min-width (em 18)
         :margin (em 1)}
        [:&.marketplace {:background-color (color :purple)
                         :color (color :white)}]
        [:&.memefolio {:background-color (color :pink)
                         :color (color :white)}]]]]]]

   [:section.history
    {:padding (em 3)}
    [:.history-component
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"
      :position :relative}
     [:h1.title
      (font :bungee)
      (for-media-max :tablet
                     [:&
                      {:font-size (px 19)}])
      {:white-space :nowrap
       :position :relative
       :color (color :purple)
       ;; :font-size (em 1.2)
       :padding-top (em 1)
       :margin-bottom (em 0.1)
       :text-align :center}]
     [:table
      {:border-spacing "0px"}
      [:th.down:after
       {:content "''"
        :background-repeat :no-repeat
        :height (em 1)
        :width (em 1)
        :margin-left (em 0.2)
        :display :inline-block
        :background-position "center bottom";
        :transform "scale(0.5, 0.5)"
        :background-image (str "url('/assets/icons/sort-triangle-icon.png')")}]
      [:th.up:after
       {:content "''"
        :background-repeat :no-repeat
        :height (em 1)
        :width (em 1)
        :margin-left (em 0.2)
        :display :inline-block
        :background-position "center bottom";
        :transform "scale(0.5, 0.5) rotate(180deg);"
        :background-image (str "url('/assets/icons/sort-triangle-icon.png')")}]
      [:td {:padding-left (em 0.5)
            :line-height (em 2)}]
      [:thead
       [:tr
        [:th
         {:text-align :left
          :cursor :pointer
          :padding-left (em 0.5)
          :background-color (color :table-header-bg)}]]]
      [:tbody
       [:tr
        ["&:nth-child(even)"
         {:background-color (color :tags-grey)}]
        [:td
         (border-bottom {:color (color :table-border)})]]]
      {:width "100%"
       :padding (em 1)}]]]

   [:section.challenge
    {:padding (em 3)}
    [:.challenge-component
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"
      :position :relative
      :display :grid
      :grid-template-columns "30% 30% 40%"
      :grid-template-rows "40% 60%"}
     (for-media-max :tablet
                    [:&
                     {:grid-template-columns "100%"
                      :grid-template-rows "100%"
                      :margin-bottom (em 2)}])
     [">*" {:padding (em 1)
            :color (color :menu-text)
            :margin-bottom (em 1)}
      ]
     
     [:.status
      {:border-right "1px solid #AAA"}
      (for-media-max :tablet
                     [:&
                      {:border-right "0px"}])]
     [:.challenger
      {:border-right "1px solid #AAA"}
      (for-media-max :tablet
                     [:&
                      {:border-right "0px"}])]
     [:.header {:grid-column "1 / span 3"}
      (for-media-max :tablet
                     [:&
                      {:grid-column "1"}])
      [:h1.title
       (font :bungee)
       (for-media-max :tablet
                      [:&
                       {:font-size (px 19)}])
       {:white-space :nowrap
        :position :relative
        :color (color :purple)
        ;; :font-size (em 1.2)
        :padding-top (em 1)
        :margin-bottom (em 0.1)
        :text-align :center}]
      [:h2.title
       (for-media-max :tablet
                      [:&
                       {:white-space :normal}])
       {:white-space :nowrap
        :margin-top (em 0.1)
        :position :relative
        :color (color :section-subcaption)
        :font-size (px 15)
        :text-align :center}]]
     ;; [:.challenge-component
     ;;  {}]
     ]]
   [:.vote {:display :grid
            :margin-left (em 6)
            :margin-right (em 6)}
    (for-media-max :large
                   [:&
                    {:margin-right (em 2)
                     :margin-left (em 2)}])
    (for-media-max :tablet
                   [:&
                    {:margin-right (em 0)
                     :margin-left (em 0)}])
    [:.vote-input {:display :grid
                   :grid-template-columns "80% 20%"
                   :border-bottom "1px solid"
                   :margin-bottom (em 1)}
     [:.help-block {:display :none}]]
    [:.vote-dank
     [:button
      {:margin-bottom (em 2)}
      (button {:background-color :rare-meme-icon-bg
               :color :violet
               :height (em 3)
               :width "100%"})
      (vote-button-icon)]]
    [:.vote-stank
     [:button
      (button {:background-color :random-meme-icon-bg
               :color :violet
               :height (em 3)
               :width "100%"})
      (vote-button-icon)
      [:&:before {:transform "scaleX(-1) scaleY(-1)"}]]]]])
