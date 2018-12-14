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
            [memefactory.styles.component.panels :refer [panel-with-icon tabs]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))

(def radius 170)
(def outer-radius radius)
(def inner-radius (/ outer-radius 4))

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
      :margin-top (em 1.5)}]]])

(defstyles core
  [:.meme-detail-page
   [:.spinner
    {;; ovverride attr
     :border-color (color :black)
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
   [:.spinner--number
    {:border-color (color :purple)
     :border-top-color (color :yellow)
     :border-width (em 0.3)
     :width (em 0.9)
     :height (em 0.9)}]
   [:.spinner--info
    {:border-color (color :busy-grey)
     :border-top-color (color :white)
     :border-width (em 1)
     :width (em 7)
     :height (em 7)}]
   [:.spinner--challenge
    {:border-color (color "#04ffcc")
     :border-top-color (color :white)
     :width (px outer-radius)
     :height (px outer-radius)
     :border-width (px inner-radius)}]
   [:.address (of-ellipsis)]
   [:section.meme-detail

    [:.meme-info
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"
      :display :grid
      :grid-template-columns "30% 70%"
      :grid-template-rows "100%"
      :position :relative
      :padding (em 1.4)}
     (for-media-max :tablet
                    [:&
                     {:grid-template-columns "100%"
                      :margin-bottom (em 2)}])
     [">.spinner--info"
      {:margin :auto}]
     [:.meme-number
      (font :bungee)
      {:position :absolute
       :z-index 2
       :display :block
       :color (color :purple)
       :left (em 3.1)
       :padding (em 1)
       :font-size (em 1.1)
       :border-radius "0em 0em 1em 1em"
       :background-color (color :yellow)
       :box-shadow ".3em .3em .3em 0px #eff5ff"}]
     [:.registry
      {:padding (em 1)}
      [:h1
       (font :bungee)
       {:color (color :purple)}]
      [:.status
       {:display :flex
        :line-height (em 1)
        :margin-left (px -8)}
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
      [:.creator
       {:margin-top (em 1)}
       [">*"
        {:font-size (em 0.9)
         :color (color :menu-text)}]]
      [:.description
       {:margin-bottom (em 1)}]
      [:.description, :.text
       {:color (color :menu-text)}]
      [:.tags
       {:margin-top (em 1)
        :margin-bottom (em 0.5)}
       [:button (tag)
        {:color (color :menu-text)}]]
      [:.buttons
       {:display :flex}
       (for-media-max :large
                      [:&
                       {:flex-direction :column}])
       [:button.search
        (button {:color :meme-buy-button
                 :height (em 2.7)})
        (for-media-max :tablet
                       [:&
                        {:min-width (em 15)}])
        {:min-width (em 18)
         :margin-right (em 1)
         :margin-top (em 1)}
        [:&.marketplace {:background-color (color :purple)
                         :color (color :white)}]
        [:&.memefolio {:background-color (color :pink)
                       :color (color :white)}]]]]]]
   [:section.history
    {:color (color :menu-text)
     :margin-top (em 3)}
    [:.history-component
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"}
     #_(for-media-max :tablet
                      [:&
                       {:max-height (em 30)
                        :overflow-y :auto
                        :overflow-x :hidden}])
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
      {:overflow-y :scroll}
      (for-media-max :tablet
                     [:&
                      {:max-height (em 30)
                       :overflow-y :auto
                       :overflow-x :hidden
                       :display :block}
                      [:tbody {:display :block}]])
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
      [:tr
       {:height (em 3)}
       (for-media-max :tablet
                      [:&
                       {:height (em 9)}])
       [:td {:padding-left (em 0.5)
             :line-height (em 2)}]]
      [:thead
       (for-media-max :tablet
                      [:&
                       {:display :none}])
       [:tr
        {:height (em 2.2)}
        [:th
         {:text-align :left
          :padding-top 0
          :cursor :pointer
          :padding-left (em 0.5)
          :background-color (color :table-header-bg)}]]]
      [:tbody
       [:tr
        ["&:nth-child(even)"
         {:background-color (color :tags-grey)}]
        (for-media-max :tablet
                       [:&
                        ["&:not(:last-child)"
                         (border-bottom {:color (color :table-border-1)})]
                        {:display :flex
                         :flex-direction :column}])
        [:td
         (border-bottom {:color (color :table-border-1)})
         [:&.seller-address
          (of-ellipsis)]
         [:&.buyer-address
          (of-ellipsis)]
         (for-media-max :tablet
                        [:&
                         {:border-bottom :none}])
         (for-media-max :tablet
                        [:&:before
                         {:font-weight "bold"
                          :margin-right (em 0.2)
                          :display :inline-block}])
         (for-media-max :tablet
                        [:&.meme-token:before
                         {:content "'Token ID:'"}])
         (for-media-max :tablet
                        [:&.seller-address
                         (of-ellipsis)
                         [:&.before
                          {:content "'Seller:'"}]])
         (for-media-max :tablet
                        [:&.buyer-address
                         (of-ellipsis)
                         [:&.before
                          {:content "'Buyer:'"}]])
         (for-media-max :tablet
                        [:&.end-price:before
                         {:content "'Price:'"}])
         (for-media-max :tablet
                        [:&.time:before
                         {:content "'Time Ago:'"}])]]]
      {:border-spacing "0px"
       :table-layout :fixed
       :width "100%"
       :padding-right (em 3)
       :padding-left (em 3)
       :padding-top (em 1)
       :padding-bottom (em 1)}]]]
   [:section.challenge
    {:margin-top (em 3)}
    [:h1.title
     (font :bungee)
     (for-media-max :tablet
                    [:&
                     {:font-size (px 19)}])
     {:white-space :nowrap
      :position :relative
      :color (color :purple)
      :padding-top (em 1)
      :margin-bottom (em 0.1)
      :text-align :center}]
    [">.spinner--challenge"
     {:margin :auto
      :margin-top (em 3)}]
    [:.challenge-component
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"}
     [:.challenge-component-inner
      {:position :relative
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
             :margin-bottom (em 1)}]
      [:.status
       {:border-right "1px solid #AAA"}
       (for-media-max :tablet
                      [:&
                       {:border-right "0px"}])]
      [:.challenger
       {:border-right "1px solid #AAA"}
       (for-media-max :tablet
                      [:&
                       {:border-right "0px"}])
       [">*"
        {:margin-bottom  (em 0.5)}]]
      [:.votes
       {:display :grid
        :grid-template-columns "50% 50%"
        :grid-template-rows "100%"}
       (for-media-max :tablet
                   [:&
                    {:grid-template-rows "50% 50%"
                     :grid-template-columns "100%"
                     :text-align :center}
                    [:button {:margin :auto}]
                    ])
       (for-media-max :large
                   [:&
                    [:.votes-inner {:padding-left (em 1)}]])

       [">div>*"
        {:margin-bottom  (em 0.5)}]

       [:button (button {:color :white
                         :background-color :purple
                         :width (em 14)
                         :height (em 3)})
        (for-media-max :large
                       [:&
                        {:width (em 10)}])

        {:margin-top (em 1)}]]
      [:.header {:grid-column "1 / span 3"}
       (for-media-max :tablet
                      [:&
                       {:grid-column "1"}])
       [:h2.title
        (for-media-max :tablet
                       [:&
                        {:white-space :normal}])
        {:white-space :nowrap
         :margin-top (em 0.1)
         :position :relative
         :color (color :section-subcaption)
         :font-size (px 15)
         :text-align :center}]]]]]
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
      [:&:before {:transform "scaleX(-1) scaleY(-1)"}]]]]
   [:section.related
    {:margin-top (em 3)}
    [:.relateds-panel
     (panel-with-icon {:url "/assets/icons/network.svg"
                       :color (color :redish)})
     [:.selling-tile-back {:height "100%"
                           :background-color (color :violet)}
      (button-tile-back)]
     [:.scroll-area
      {:box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
       :background-color :white
       :color (color :menu-text)
       :border-radius (em 1)}]]]])
