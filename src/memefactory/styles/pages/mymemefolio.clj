(ns memefactory.styles.pages.mymemefolio
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.component.buttons :refer [button]]
            [memefactory.styles.base.borders :refer [border-top border-bottom]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [memefactory.styles.component.search :refer [search-panel]]
            [memefactory.styles.component.panels :refer [tabs]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]
            [memefactory.styles.pages.memefolio.details :as details]))

(defn button-tile-back []
  [:.sell {:display :grid
           :grid-template-rows "75% 25%"
           :width "100%"}
   [:.top {:width "30%"
           :margin-left :auto
           :margin-right :auto}
    [:b (font :bungee)
     {:font-size (em 3)
      :color (color :purple)
      :display :block
      :margin-bottom (em 0.4)
      :margin-top (em 2)}]
    [:img {:max-width "100%"
           :height :auto}]]
   [:.bottom {:background-color (color :purple)}
    [:button (button {:background-color :violet
                      :color :white
                      :height (em 3)
                      :width "75%"})
     {:margin-left :auto
      :margin-right :auto
      :margin-top (em 1.5)}]]])

(defstyles core
  details/core
  [:.memefolio-page
   (search-panel {:background-panel-image "/assets/icons/mf-search.svg"
                  :color :mymemefolio-green
                  :icon "/assets/icons/portfolio2.svg"})
   [:.loading
    {;; ovverride attr
     :color "black"
     :border-top-color "white"
     :border-width (em 0.4)
     :border-top-width (em 0.4)
     :width (em 2)
     :height (em 2)
     ;; common attr
     :border-style :solid
     :border-top-style :solid
     :border-radius "50%"
     :animation-name :spin
     :animation-duration "2s"
     :animation-iteration-count :infinite
     :animation-timing-function :linear}]
   [:.total
    {:position :absolute
     :right (em 4)
     :font-size (em 0.9)
     :color (color :section-subcaption)}
    (for-media-max :tablet
                   [:&
                    {:margin-right (em 2)
                     :margin-top (em 2)
                     :margin-left (em 6)}])

    [">div"
     {:display :flex
      :margin :auto}
     [:.loading
      {:border-color (color :section-subcaption)
       :border-top-color (color :white)
       :margin :auto
       :margin-left (em 0.3)
       :border-width (em 0.1)
       :border-top-width (em 0.1)
       :width (em 0.7)
       :height (em 0.7)}]]]
   [:section.stats
    {:display :flex
     :margin-top 0
     :padding-top (em 0)
     :padding-bottom (em 0)
     :margin-right (em 6)
     :margin-left (em 6)
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :background-color (color :yellow)
     :border-radius "1em 1em 0em 0em"
     :letter-spacing (em 0.01)}
    (for-media-max :tablet
                   [:&
                    {:margin-right (em 1)
                     :margin-left (em 1)}])
    [".stats>*"
     {:padding-right (em 1)
      :padding-left (em 1)}
     ]
    #_[">.rank"
       {:width "100%"}
       ]
    [".rank>.stats>.rank"
     (font :bungee)
     {:color (color :purple)
      :border-radius "1em 0em 0em 0em"
      :background-color (color :rank-yellow)
      :padding-right (em 1)
      :padding-left (em 1)
      ;; :display :table-cell
      :font-size (em 1)
      :display :flex
      :margin :auto}
     (for-media-max :tablet
                    [:&
                     {:border-radius "1em 1em 0em 0em"}])
     [:&.big
      {:line-height (em 6)}]
     [:.loading
      {:border-color (color :purple)
       :border-top-color (color :rank-yellow)
       :margin :auto
       :margin-left (em 0.5)
       :border-width (em 0.3)
       :border-top-width (em 0.3)
       :width (em 1)
       :height (em 1)}]]
    [:.stats
     {:display :flex
      :line-height (em 3)}
     (for-media-max :tablet
                    [:&
                     {:flex-direction :column}])
     [:.var
      {:color (color :section-subcaption)
       :margin-right (em 1)
       :margin-left (em 1)
       :display :flex
       :margin :auto
       }
      [">b"
       {:padding-right (em 1)}]
      [:.loading
       {:border-color (color :section-subcaption)
        :border-top-color (color :yellow)
        :margin :auto
        :border-width (em 0.3)
        :border-top-width (em 0.3)
        :width (em 1)
        :height (em 1)}]]
     [:.curator
      {:display :grid
       :flex-grow 1
       :grid-cols "100%"}
      [">div"
       {:display :flex}
       [:.label
        {:color (color :pink)}
        (font :bungee)]
       [">*"
        {:color (color :section-subcaption)
         :margin-right (em 1)
         :line-height (em 2)
         :margin-left (em 1)}
        [:b {:margin-right (em 0.2)}]]]]]]
   [:.tiles
    {:margin-top 0
     :padding-top (em 2)
     :padding-bottom (em 2)
     :margin-right (em 6)
     :margin-left (em 6)
     :background-color (color :meme-panel-bg)
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     }
    (for-media-max :tablet
                   [:&
                    {:margin-right (em 1)
                     :margin-left (em 1)}])
    [:.loading
     {:color (color :busy-grey)
      :border-top-color (color :white)
      :margin :auto
      :border-width (em 1)
      :border-top-width (em 1)
      :width (em 7)
      :height (em 7)}]
    [">div>div>div"
     {:display :flex
      :flex-wrap :wrap
      :justify-content :space-evenly}
     [:.footer {:text-align :center
                :color (color :meme-tile-footer)
                :cursor :pointer}]
     [:.issue-form {:text-align :center
                    :margin-top (em 1)}
      [:.field {:display :grid
                :grid-template-columns "50% 50%"
                :margin-left :auto
                :margin-right :auto
                :overflow :hidden
                :width "60%"
                :height (em 2.5)
                :border-radius (em 2)}
       [:button
        (font :bungee)
        {:display "block"
         :bottom (em 2)
         :border-radius 0
         :height (em 3)
         :width "100%"
         :border-style "none"
         :color (color :white)
         :background-color (color :purple)}
        [:&:disabled
         {:opacity 0.3}]]
       [:input {:background-color (color :search-input-bg)
                :text-align :center
                :height (em 3)}]]]]
    [:.collected-tile-back {:height "100%"
                            :background-color (color :violet)}
     (button-tile-back)
     [:.sell-form {:background-color :white
                   :width "100%"
                   :padding (em 1)
                   :text-align :left}
      [:h1 (font :bungee)
       {:color (color :purple)
        :font-size (em 1)
        :text-align :center
        :margin-bottom (em 1)}]
      [:.form-panel {}
       [:textarea {:background (color :tags-grey)
                   :border :none
                   :border-radius (em 0.4)}]
       [:.buttons {:display :inline-flex
                   :margin-top (em 1)}
        [:button.cancel (button {:color :white
                                 :background-color (color :tags-grey)
                                 :height (em 3)})
         {:font-size (em 0.8)}]
        [:button.create-offering (button {:color :white
                                          :height (em 3)
                                          :background-color (color :purple)
                                          :width "55%"})
         {:font-size (em 0.7)
          :padding (em 0.4)}]]]]]
    [:.selling-tile-back {:height "100%"
                          :background-color (color :violet)}
     (button-tile-back)]]
   [:section.tabs
    (tabs)]])
