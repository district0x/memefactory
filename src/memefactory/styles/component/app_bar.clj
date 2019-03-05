(ns memefactory.styles.component.app-bar
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.base.icons :refer [icons]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [memefactory.styles.component.overflow :refer [of-ellipsis]]
   [garden.units :refer [rem px em]]))

(def bar-height (px 50))

(defstyles core
  [:.app-bar-mobile
   {:position :relative
    :height (px 65)
    :padding (em 1)}
   [:.logo
    {:display :block
     :position :absolute
     :content "''"
     :background-size [(rem 3) (rem 3)]
     :background-position "5px 5px"
     :background-repeat :no-repeat
     ;; :margin-left (rem -3)
     :background-image (str "url('/assets/icons/mf-logo.svg')")}
    [:&:before
     (font :bungee)
     {:content "'MEME FACTORY'"
      :font-size (px 18)
      :line-height (em 1.2)
      :color (color :menu-logo)
      ;; :margin-right (em 5)
      :width (rem 5)
      ;; :padding-top (rem 2.8)
      :padding-left (rem 4)
      :display :block
      :min-height (rem 8)}]]
   [:.menu-selection
    {:position :absolute
     :width (px 33)
     :right 0}
    [:.icon.hamburger
     {:cursor :pointer
      :position :absolute
      :top (px 7)
      :right (px 7)
      :font-size (px 29)
      ;; :padding (px 8)
      :line-height "100%"
      :color (color :pink)
      :z-index 100}]]
   (for-media-min :tablet [:&
                           {:display :none}])]
  [:.app-bar
   {:height bar-height
    :background-color (color "white")
    :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :flex
    :align-items :center
    :justify-content :space-between}
   (for-media-max :tablet
                  [:&
                   {:display :none}])
   [:.account-section
    {:align-items :left
     :width (px 250)
     :padding "0 10px"}
    [:.active-account ;;-select
     [:i.dropdown.icon:before
      {:content "url('/assets/icons/dropdown.png')" ;;No, we can't just bg scale, thanks upstream !important
       :display :inline-flex
       :transform "scale(.5)"}]
     [:span.text
      {:overflow "hidden"
       :white-space "nowrap"
       :text-overflow :ellipsis
       :width "100%"
       :display "inline-block"}]]]

   [:.search-section
    [:.search
     {:position :relative}
     [:input
      {:background-color (color :search-input-bg)
       :border-radius (em 1)
       :border-style :none
       :padding-left (em 1)
       :height (em 2)
       :width (em 26)}
      [:&:focus {:outline :none}]]
     [:.go-button
      {:display :block
       :content "''"
       :background-size [(em 1.3) (em 1.3)]
       :background-repeat :no-repeat
       ;; :background-color (color :yellow)
       :background-image "url('/assets/icons/search.png')"
       ;; :margin-left (rem -3)
       ;; :margin-top (rem -0.2)
       :top (em 0.4)
       :right (em 0.6)
       :position :absolute
       :height (em 1.3)
       :width (em 1.3)}
      [:&.hover
       {
        ;; :background-color (color :yellow)
        }]]]]
   [:.tracker-section
    {:cursor :pointer
     :transition "width 100ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"
     :width :transactionLogWidth
     :height "100%"
     :display :flex
     :align-items :center
     :justify-content :center}
    [:.accounts
     {:display :grid
      :grid-template-areas
      (str
       "'dank eth'\n"
       "'tx-log tx-log'\n")
      :height "100%"
      :align-items :center
      :justify-content :center
      :grid-template-rows (em 3.6)}
     [">div" {:height "100%"}]
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
       :transform-origin :top
       :width (px 300)}

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
         :overflow-x :hidden
         }

        [:.transaction
         {:display :grid
          :grid-template-areas (str "'tx-name tx-status'\n"
                                    "'tx-created-on tx-status'\n"
                                    "'tx-gas tx-status'\n"
                                    "'tx-sender tx-value'\n"
                                    "'tx-id tx-value'\n")
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
                     :width (em 11.3)
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

     [:&.dank
      {:grid-area :dank}]

     [:&.eth
      {:grid-area :eth}]

     [:.active-account-balance
      {:display :block
       :height "100%"
       :min-width (em 9)
       :background-color (color :ticker-background)
       :position :relative
       ;; :font-size (px 18)
       }
      ["&:not(:last-child)"
       {:border-right (px 1)
        :border-right-color (color :black)
        :border-right-style :solid}]
      [:&:before
       {:display :block
        :background-size [(em 1) (em 2)]
        :background-repeat :no-repeat
        :left (em 0.4)
        :top (em 0.3)
        :position :relative
        :height (em 2)
        :width (em 1)
        :content "''"
        :background-image (str "url('/assets/icons/ethereum.svg')")}]
      [:.balance
       (font :bungee)
       {:white-space :nowrap
        :color (color :ticker-color)
        :position :absolute
        :display :inline-block
        :text-align :center
        :left 0;
        :right 0;
        :margin-right "auto"
        :margin-left "auto"
        :top (em 0.3)
        :overflow :hidden
        :text-overflow :ellipsis}]
      [:.token-code
       {:white-space :nowrap
        :color (color :ticker-token-color)
        :position :absolute
        :display :block
        :left 0
        :right 0
        :top (em 1.5)
        :width (em 2)
        :margin-right "auto"
        :margin-left "auto"
        :bottom (em 0.3)}]]]]])
