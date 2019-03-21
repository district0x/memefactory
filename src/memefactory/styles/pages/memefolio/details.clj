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
   [:.spinner-outer {:margin-left :auto
                     :margin-right :auto}]

   [:.address (of-ellipsis)]
   [:section.meme-detail
    [:.meme-info
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"
      :display :grid
      :grid-template-columns "32% 68%"
      :position :relative
      :min-height (px 480)
      :padding (em 1.4)}
     (for-media-max :large
                    [:&
                     {:grid-template-columns "40% 60%"}])
     (for-media-max :computer
                    [:&
                     {:grid-template-columns "100%"
                      :margin-bottom (em 2)}])

     [:&.loading {:display :flex
                  :align-items :center
                  :justify-content :center}]
     [:.meme-number
      (font :bungee)
      {:position :absolute
       :z-index 2
       :display :block
       :color (color :purple)
       :left (em 4.1)
       :padding (em 1)
       :font-size (em 1.1)
       :border-radius "0em 0em 1em 1em"
       :background-color (color :yellow)}
      (for-media-max :tablet
                     [:&
                      {:left (em 2.5)}])
      ]
     [:.registry
      {:padding (em 1)}
      [:h1
       (font :bungee)
       {:color (color :purple)
        :margin-bottom (em 0.2)}]
      [:.status
       {:display :flex
        :line-height (em 1)
        :margin-left (px -8)
        }
       [:&:before
        {:content "''"
         :height (em 2.25)
         :display :inline-block
         :width (em 2.25)
         :transform "scale(0.40, 0.40)"
         :background-repeat :no-repeat
         :background-position "center center";
         :background-image (str "url('/assets/icons/inregistry-icon.png')")}]
       [:label
        {:line-height (em 2.37)
         :font-weight :bold
         :color (color :menu-text)}]]
      [:.creator
       {:margin-top (em 1)}
       [">*"
        {:font-size (em 0.9)
         :color (color :menu-text)}]
       [:.address {:cursor :pointer}]]
      [:.description
       {:margin-bottom (em 1)}]
      [:.description, :.text
       {:color (color :menu-text)}]

      [:.tags
       {:margin-top (em 1)
        :margin-bottom (em 0.5)}
       [:button (tag)
        {:cursor :pointer}
        {:color (color :menu-text)}]]
      [:.buttons
       {:display :flex}
       (for-media-max :large
                      [:&
                       {:flex-direction :column}])
       [:button.search
        (button {:color :meme-buy-button
                 :height (em 2.7)})
        (for-media-max :computer
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
      :min-height (px 250)
      :border-radius "1em 1em 1em 1em"
      :padding-top (em 1)
      :padding-bottom (em 1)}
     #_(for-media-max :computer
                      [:&
                       {:max-height (em 30)
                        :overflow-y :auto
                        :overflow-x :hidden}])
     [:.spinner-outer {:margin-top (em 4)}]
     [:h1.title
      (font :bungee)
      (for-media-max :computer
                     [:&
                      {:font-size (px 19)}])
      {:white-space :nowrap
       :position :relative
       :color (color :purple)
       ;; :font-size (em 1.2)
       :margin-bottom (em 0.1)
       :text-align :center}]
     [:table
      {:overflow-y :scroll}
      (for-media-max :computer
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
       (for-media-max :computer
                      [:&
                       {:height (em 9)}])
       [:td {:padding-left (em 0.5)
             :line-height (em 2)}]]
      [:thead
       (for-media-max :computer
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
        ["&:nth-child(even)" {:background-color (color :curator-card-bg)}]
        (for-media-max :computer
                       [:&
                        ["&:not(:last-child)"
                         (border-bottom {:color (color :table-border-1)})]
                        {:display :flex
                         :min-height (em 11)
                         :flex-direction :column}])
        [:td
         (border-bottom {:color (color :table-border-1)})
         [:&.seller-address
          (of-ellipsis)
          {:cursor :pointer}]
         [:&.buyer-address
          {:cursor :pointer}
          (of-ellipsis)]
         (for-media-max :computer
                        [:&
                         {:border-bottom :none}])
         (for-media-max :computer
                        [:&:before
                         {:font-weight "bold"
                          :margin-right (em 0.2)
                          :display :inline-block}])
         (for-media-max :computer
                        [:&.meme-token:before
                         {:content "'Token ID:'"}])
         (for-media-max :computer
                        [:&.seller-address
                         (of-ellipsis)
                         {:min-height (em 2)}
                         [:&:before
                          {:content "'Seller:'"}]])
         (for-media-max :computer
                        [:&.buyer-address
                         (of-ellipsis)
                         {:min-height (em 2)}
                         [:&:before
                          {:content "'Buyer:'"}]])
         (for-media-max :computer
                        [:&.end-price:before
                         {:content "'Price:'"}])
         (for-media-max :computer
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
     (for-media-max :computer
                    [:&
                     {:font-size (px 19)}])
     {:white-space :nowrap
      :position :relative
      :color (color :purple)
      :margin-bottom (em 0.1)
      :text-align :center}]

    [:.challenge-component
     {:background (color :meme-panel-bg)
      :min-height (px 250)
      :padding-top (em 1)
      :padding-bottom (em 1)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"}
     [:.spinner-outer {:margin-left :auto
                       :margin-right :auto
                       :margin-top (em 4)}]
     [:.status {:padding-left (em 1)}
      [:li {:font-weight :bold
            :list-style-type :none}
       [:div {:font-weight :normal
              :margin-bottom (em 0.5)
              :margin-top (em 0.5)
              }]]]
     [:h4.title {:margin-left :auto
                 :margin-right :auto
                 :width (em 17)
                 :margin-top (em 1)
                 :margin-bottom (em 1)}]
     [:.challenge-component-inner-cp
      {:display :grid
       :grid-template-columns "50% 50%"
       :padding-left (em 2)
       :padding-right (em 2)}
      (for-media-max :computer
                     [:&
                      {:grid-template-columns "100%"
                       :padding-left (em 1)
                       :padding-right (em 1)}])
      [">*" {:padding (em 1)
             :color (color :menu-text)
             :margin-bottom (em 1)}]
      [:.header {:grid-column "1 / span 3"
                 :text-align :center}
       (for-media-max :computer
                      [:&
                       {:grid-column "1"}])]
      [:.input-group
       [:textarea {:background-color (color :main-content-bg)
                   :border-radius (em 1)
                   :margin-top (em 2)
                   :height (em 6)
                   :border :none
                   :resize :none
                   :width "100%"
                   :padding (em 1)
                   :color :grey}]
       [:.help-block {:display :none}]]
      [:.controls {:display :flex
                   :justify-content :flex-end
                   :margin-top (em 1)}
       [:.dank {:margin-right (em 1)
                :margin-top (em 0.7)}]
       [:button
        (button {:color :white
                 :background-color :purple
                 :width (em 11)
                 :height (em 3)})]]
      [:.status
       {:border-right "1px solid rgba(174, 175, 177, 0.5)"}
       (for-media-max :computer
                      [:&
                       {:border-right "0px"}])
       [:.lorem {:margin-top (em 1)}]]]
     [:.challenge-component-inner
      {:position :relative
       :display :grid
       :grid-template-columns "33% 33% 33%"
       :padding-left (em 2)
       :padding-right (em 2)}
      (for-media-max :computer
                     [:&
                      {:grid-template-columns "100%"
                       :margin-bottom (em 2)}])
      [">*" {:padding (em 1)
             :color (color :menu-text)
             :margin-bottom (em 1)}]
      [:.status
       {:border-right "1px solid rgba(174, 175, 177, 0.5)"
        :margin-top 0}
       (for-media-max :computer
                      [:&
                       {:border-right "0px"}])
       [:b {:display :block
            :margin-bottom (em 0.5)}]]
      [:.challenger
       {:border-right "1px solid rgba(174, 175, 177, 0.5)"
        :width "100%"}
       (for-media-max :computer
                      [:&
                       {:border-right "0px"}])
       [:.address {:cursor :pointer}]
       [:b {:display :block}]
       [">*"
        {:margin-bottom  (em 0.5)}]]

      [:.collect-reward
       {:display :grid
        :justify-items :center}
       [:.vote-info {:list-style :none
                     :text-align :center
                     :padding-left (px 0)}]
       [:button
        {:margin-top (em 1)}
        (button {:background-color :purple
                 :color :white
                 :height (em 2.5)
                 :width (em 13)})
        [:&.collect-reward {:background-color (color :pink)}]]]



      {:display :grid
       :justify-items :center}
      [:.vote-info {:list-style :none
                    :text-align :center
                    :padding-left (px 0)}]



      [:.header {:grid-column "1 / span 3"
                 :text-align :center}
       (for-media-max :computer
                      [:&
                       {:grid-column "1"}])
       [:h2.title
        (for-media-max :computer
                       [:&
                        {:white-space :normal}])
        {:white-space :nowrap
         :margin-top (em 0.1)
         :position :relative
         :color (color :section-subcaption)
         :font-size (px 15)
         :text-align :center}]]

      [:.reveal
       [:button {:margin-top (em 1)}
        (button {:color :white
                 :background-color :purple
                 :width (em 13)
                 :height (em 3)})]
       [:.no-reveal-info {:color :red
                          :margin-top (em 1)}]]

      [:.vote
       {:padding-right 0}
       (for-media-max :large
                      [:&
                       {:margin-right (em 2)
                        :margin-left (em 2)}])
       (for-media-max :computer
                      [:&
                       {:margin-right (em 0)
                        :margin-left (em 0)}])
       [:b {:margin-top (em 0.5)
            :display :block}]
       [:.outer {:display :inline-flex
                 :width "100%"
                 :margin-bottom (em 1)}
        [:.unit {:margin-left (px -40)
                 :margin-top (px 8)
                 :font-size (px 11)
                 :z-index 1}]
        [:.help-block {:display :none}]]
       [:.form {:display :inline-flex
                :margin-top (em 1)}
        [:input {:border-bottom "1px solid"
                 :border-left 0
                 :border-right 0
                 :border-top 0}]
        [:.vote-dank {:margin-right (em 2)
                      :width "50%"}
         [:.labeled-input-group {:width "100%"}]
         [:button
          {:margin-bottom (em 2)}
          (button {:background-color :rare-meme-icon-bg
                   :color :violet
                   :height (em 3)
                   :width "100%"})
          (vote-button-icon 2)]]
        [:.vote-stank
         {:width "51%"}
         [:.labeled-input-group {:width "100%"}]
         [:button
          (button {:background-color :random-meme-icon-bg
                   :color :violet
                   :height (em 3)
                   :width "100%"})
          (vote-button-icon 6)
          [:&:before {:transform "scaleX(-1) scaleY(-1)"}]]]]]]]
    [:.challenge-comment
     {:font-style :italic
      :color (color :pink)}]]

   [:section.related
    {:margin-top (em 3)}
    [:.relateds-panel
     {:min-height (px 550)}
     (panel-with-icon {:url "/assets/icons/network.svg"
                       :color (color :redish)})
     [:.spinner-outer {:margin-left :auto
                       :margin-right :auto
                       :padding-top (em 7)}]
     [:h2.title {:padding-top (em 1)}]
     [:.selling-tile-back {:height "100%"
                           :background-color (color :violet)}
      (button-tile-back)]
     [:.scroll-area
      {:background-color :white
       :color (color :menu-text)
       :border-radius (em 1)}
      [:.tiles {:box-shadow :unset}]
      ]]]])
