(ns memefactory.styles.component.account-balances
  (:require 
   [clojure.string :as s]
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [garden.units :refer [pt px em rem]]
   [memefactory.styles.base.icons :refer [icons]]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]))


(def bar-height 50) ;; px
(def account-balance-width 270) ;; px


(def account-section-style
  {:display :grid
   :grid-template-areas "'logo account-balance .'"
   :grid-template-rows (str bar-height "px")
   :grid-template-columns "1fr 1fr 1fr"
   :height (px bar-height)
   :width (px (/ account-balance-width 2))})


(defstyles core
  [:.accounts
   {:display :grid
    :grid-template-areas
    "'dank-section eth-section'
       'tx-log       tx-log'"
    :height "100%"
    :width (px account-balance-width)
    :background-color (color :ticker-background)}
   [:.dank-section
    (merge
     {:grid-area :dank-section
      :box-sizing :border-box
      :border-right-width (px 1)
      :border-right-style :solid
      :border-right-color "rgba(101,0,57,0.5)"
      :display :flex
      :justify-content :center}
     account-section-style)
    [:.dank-logo
     {:grid-area :logo
      :display :flex
      :justify-content :center}
     [:img
      {:width (em 1.8)
       :height (em 1.8)
       :margin-top (em 0.25)
       :margin-right (em 0.7)}]]]
   [:.eth-section
    (merge
     {:grid-area :eth-section
      :display :flex
      :justify-content :center}
     account-section-style)
    [:.eth-logo
     {:grid-area :logo
      :display :flex
      :justify-content :center}
     [:img
      {:width (em 1.3)
       :height (em 1.3)
       :margin-top (em 0.6)
       :margin-right (em 0.7)}]]]
   [:.tx-log.open
    {:transform "scaleY(1)"}]

   [:.tx-log
    {:grid-area :tx-log
     :z-index 99
     :transform "scaleY(0)"
     :will-change :transform
     :transition-property "transform, -webkit-transform"
     :transition-duration ".3s, .3s"
     :transition-timing-function "cubic-bezier(0.23, 1, 0.32, 1), cubic-bezier(0.23, 1, 0.32, 1)"
     :transform-origin :top}

    [:.header
     {:background (color :pink)
      :text-transform :uppercase
      :color :white
      :font-size "12px"
      :text-align :center
      :padding-top (em 0.3)
      :cursor :pointer}]

    [:.tx-content
     {:background (color :white)
      :z-index 99
      :color (color :menu-text)
      :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
      :position :relative}
     [:.settings
      {:padding "10px 25px"
       :border-bottom (str "1px solid #b2bacb")}
      [:.ui.checkbox
       [:label
        {:font-size "10px"}]]]

     [:.no-transactions
      {:width "100%"
       :height (px 347)
       :display :flex
       :justify-content :center
       :align-items :center
       :font-size (px 12)
       :color "#b2bacb"}]

     [:.transactions
      {:height (px 424)
       :overflow-y :auto
       :width "104%"
       :overflow-x :hidden}

      [:.transaction
       {:display :grid
        :grid-template-areas (str "'tx-name tx-status'\n"
                                  "'tx-created-on tx-status'\n"
                                  "'tx-gas tx-status'\n"
                                  "'tx-id tx-value'\n"
                                  "'tx-sender tx-value'\n")
        :border-bottom "1px solid lightgrey"
        :cursor :pointer
        :position :relative
        :line-height 1.5
        :font-size (px 12)
        :text-overflow :ellipsis
        :color (color :meme-tile-footer)
        :margin (em 1)}

       ["&:not(:last-child)"
        {:border-bottom (str "1px solid " (color :black))}]

       [:.tx-name {:grid-area :tx-name
                   :font-size (px 14)
                   :color (color :black)
                   :line-height "1.6"
                   :overflow :hidden
                   :text-overflow :ellipsis
                   :width (em 14)
                   :white-space :nowrap}]

       [:.tx-created-on
        {:grid-area :tx-created-on}]

       [:.tx-gas
        {:grid-area :tx-gas}]

       [:.tx-sender
        {:grid-area :tx-sender}
        [:a {:display :inline-block
             :max-width "125px"
             :overflow :hidden
             :text-overflow :ellipsis
             :white-space :nowrap
             :vertical-align :top
             :text-decoration :underline}]]

       [:.tx-id
        {:grid-area :tx-id}
        [:a {:display :inline-block
             :max-width "125px"
             :overflow :hidden
             :text-overflow :ellipsis
             :white-space :nowrap
             :vertical-align :top
             :text-decoration :underline}]]

       [:.tx-value
        {:grid-area :tx-value
         :text-align :center}]

       [:.tx-status
        {:grid-area :tx-status
         :margin-top (em 1)
         :text-transform :uppercase
         :font-size "10px"
         :font-weight :bold
         :display :flex
         :flex-direction :column
         :align-items :center
         :justify-content :space-between
         :height "45px"}
        [:i.icon {:width "20px"
                  :height "20px"
                  :line-height "20px"
                  :border-radius "100%"
                  :color (color :white)}]
        [:&.success
         {:color (color :leaderboard-curator-bg)}
         [:i.icon
          {:background-color (color :leaderboard-curator-bg)
           :font-size "8px"}
          [:&:before
           {:content (icons :check)}]]]
        [:&.error
         {:color (color :red)}
         [:i.icon
          {:background-color (color :red)
           :font-size "7px"}
          [:&:before {:content (icons :times)}]]]
        [:&.pending
         {:color (color :rank-yellow)}
         [:i.icon {:background-color (color :rank-yellow)
                   :font-size "10px"}
          [:&:before
           {:content (icons :clock2)}]]]]]]]]

   [:.active-account-balance
    {:grid-area :account-balance
     :display :flex
     :background-color (color :ticker-background)
     :flex-direction :column
     :justify-content :center
     :align-items :center}

    [:.balance
     (font :bungee)
     {:white-space :nowrap
      :color (color :ticker-color)
      :text-align :center
      :overflow :hidden
      :text-overflow :ellipsis}]
    [:.token-code
     {:white-space :nowrap
      :color (color :ticker-token-color)}]]])
