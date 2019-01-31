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
           :height "100%"
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
      :margin-top (em 2)}]]])

(defn thumb-icon [dir]
  (cond-> {:content "''"
           :background-image "url('/assets/icons/thumb-up.svg')"
           :background-size "15px 15px"
           :display :inline-block
           :background-repeat :no-repeat
           :width (px 15)
           :height (px 15)
           :position :relative
           :margin-left (px 10)}
    (= dir :down) (assoc :transform "scaleX(-1) scaleY(-1)")))
(defstyles core
  details/core
  [:.memefolio-page
   (search-panel {:background-panel-image "/assets/icons/mf-search.svg"
                  :color :mymemefolio-green
                  :icon "/assets/icons/portfolio2.svg"})
   [:div.search-form
    [:h2 {:max-width (em 13.5)
          :position :unset
          :text-overflow :ellipsis
          :margin-left :auto
          :margin-right :auto
          :overflow :hidden}]]
   [:.tabbed-pane
    {:max-width (px 985)
     :margin-right :auto
     :margin-left :auto}]
   [:.spinner
    {;; ovverride attr
     :color (color :black)
     :border-top-color (color :white)
     :border-width (em 0.4)
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
   [:.spinner--total
    {:border-color (color :section-subcaption)
     :border-top-color (color :white)
     :margin :auto
     :border-width (em 0.1)
     :width (em 0.7)
     :height (em 0.7)}]
   [:.spinner--rank
    {:border-color (color :purple)
     :border-top-color (color :rank-yellow)
     :margin :auto
     ;; :margin-left (em 0.5)
     :border-width (em 0.3)
     ;; :border-top-width (em 0.3)
     :width (em 1)
     :height (em 1)}]
   [:.spinner--var
    {:border-color (color :section-subcaption)
     :border-top-color (color :yellow)
     :margin :auto
     :border-width (em 0.3)
     :border-top-width (em 0.3)
     :width (em 1)
     :height (em 1)}]
   [:.total
    {:position :relative
     :left (em 41)
     :font-size (em 0.9)
     :color (color :section-subcaption)}
    (for-media-max :computer
                   [:&
                    {:margin-right (em 2)
                     :margin-top (em 2)
                     :margin-left (em 6)}])
    [">div"
     {:display :flex
      :margin :auto}
     [">.spinner--total"
      {:margin-left (em 0.5)}]]]
   [:section.stats
    {:display :flex
     :margin-top 0
     :padding-top (em 0)
     :padding-bottom (em 0)
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :background-color (color :yellow)
     :border-radius "1em 1em 0em 0em"
     :letter-spacing (em 0.01)}
    (for-media-max :computer
                   [:&
                    {:display :block}])
    [">.rank"
     {:width "100%"}]
    [".stats>*"
     {:padding-right (em 1)
      :align-items :center
      ;; :min-height (em 4)
      :padding-left (em 1)}
     [:& [:b {:white-space :nowrap}]]]
    [".rank>.stats>.rank"
     (font :bungee)
     {:color (color :purple)
      :border-radius "1em 0em 0em 0em"
      :background-color (color :rank-yellow)
      :display :flex
      :width (em 10)
      :font-size (em 1)
      ;; :min-height (em 4)
      :height (em 3.3)
      :padding-left (em 3)}
     (for-media-max :computer
                    [:&
                     {:width "100%"
                      :border-radius "1em 1em 0em 0em"}])
     [:&.rank--big
      {:line-height (em 6)
       :margin :auto
       :height "100%"
       :align-self :center}]]
    [:.stats
     {:display :flex}
     (for-media-max :computer
                    [:&
                     {:flex-direction :column}])
     [:.var
      {:color (color :section-subcaption)
       :margin-right (em 1)
       :margin-left (em 1)
       :font-size (px 12)
       :display :flex
       :white-space :nowrap}
      [">b"
       {:padding-right (em 0.3)}]]
     [:.curator
      {:display :grid
       :padding-top (em 1.2)
       :padding-bottom (em 0.9)
       :flex-grow 1}
      [">div"
       {:display :flex
        :font-size (em 0.8)}
       (for-media-max :computer
                      [:&
                       {:flex-direction :column}])
       [:.label
        {:color (color :pink)}
        (font :bungee)]
       [">*"
        {:color (color :section-subcaption)
         :display :flex
         :margin-right (em 1)
         :margin-left (em 1)}
        [:b {:margin-right (em 0.5)}]]]]]]
   [:.selling-panel {:border-radius "1em 1em 1em 1em"
                     :overflow :hidden
                     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"}]
   [:.sold-panel {:border-radius "1em 1em 1em 1em"
                  :overflow :hidden
                  :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"}]
   [:.tiles
    {:margin-top 0
     :padding-top (em 2)
     :padding-bottom (em 2)
     :background-color (color :meme-panel-bg)
     :border-radius "0 0 1em 1em"}

    [:.loading
     {:color (color :busy-grey)
      :border-top-color (color :white)
      :margin :auto
      :border-width (em 1)
      :border-top-width (em 1)
      :width (em 7)
      :height (em 7)}]
    [:.compact-tile
     ;; {:display :flex
     ;;  :flex-wrap :wrap
     ;;  :justify-content :space-evenly}
     [:.footer {:text-align :center
                :color (color :meme-tile-footer)
                :cursor :pointer
                :line-height (em 1.5)}
      [:.status {:font-weight :bold
                 :font-style :italic}]
      [:.vote-option
       [:label {:text-transform :uppercase
                :font-weight :bold
                :color :black
                :padding-bottom (em 0.4)}]
       [:label.vote-dank {:border-bottom "2px solid "
                          :border-color (color :rare-meme-icon-bg)}
        [:&:after (thumb-icon :up)]]
       [:label.not-revealed {:border-bottom "2px solid "
                             :border-color (color :header-sub-title)}]
       [:label.vote-stank {:border-bottom "2px solid "
                           :border-color (color :yellow)}
        [:&:after (thumb-icon :down)]]]]
     [:.issue-form {:text-align :center
                    :margin-top (em 1)}
      [:.field {:display :grid
                :grid-template-columns "50% 50%"
                :max-width (em 16)
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
                :padding-bottom (em 0.4)
                :height (em 3)}]]
      [:.label {:padding-top (em 0.5)
                :font-size (em 0.8)
                :color (color :meme-tile-footer)}]]]
    [:.collected-tile-back {:height "100%"
                            :background-color (color :violet)
                            :backface-visibility :hidden}
     (button-tile-back)
     [:.sell-form {:background-color :white
                   :width "100%"
                   :height "100%"
                   :padding (em 1)
                   :text-align :left
                   :border "1px solid grey"
                   :border-radius "1em"}
      [:h1 (font :bungee)
       {:color (color :purple)
        :font-size (em 1)
        :text-align :center
        :margin-bottom (em 1)
        :overflow :hidden
        :text-overflow :ellipsis
        :white-space :nowrap}]

      [:.form-panel
       [:.area
        [:.help-block {:display :none}]]]

      [:.form-panel {:color (color :darker-blue)}
       [:.outer {:display :inline-flex
                 :width "100%"}
        [:.unit {:margin-left (px -40)
                 :margin-top (px 8)
                 :z-index 1}]
        [:.labeled-input-group {:width "100%"}]
        [:.input-group {:margin-bottom (em 1)}
         [:.help-block {:height (em 1)
                        :line-height (em 1.5)}
          [:&:before {:width 0}]]]]
       [:.short-sales-pitch {:margin-bottom (em 0.4)
                             :display :block}]
       [:textarea {:background (color :white)
                   :border "1px solid #CCC"
                   :width "100%"
                   :resize :none
                   :height (em 5)
                   :padding (em 0.5)}]
       [:.buttons {:display :inline-flex
                   :justify-content :space-between
                   :font-size (px 15)
                   :margin-top (em 0.7)
                   :width "100%"}
        [:button.cancel (button {:color :white
                                 :background-color (color :cancel-button)
                                 :height (em 3.5)
                                 :width "40%"})
         {:font-size (px 11)
          :padding-top (px 3)}]
        [:button.create-offering (button {:color :white
                                          :height (em 3.5)
                                          :background-color (color :purple)
                                          :width "55%"})
         {:font-size (px 11)
          :padding (em 0.4)}]]]]]
    [:.selling-tile-back {:height "100%"
                          :background-color (color :violet)}
     (button-tile-back)]]
   [:section.tabs
    (tabs)]])
