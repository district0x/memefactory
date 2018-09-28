(ns memefactory.styles.component.app-bar
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [rem px em]]))

(def bar-height (px 50))

(defstyles core
  [:.app-bar-mobile
   {:position :relative
    :height (px 43)}
   [:.logo
    {:display :block
     :position :absolute
     :content "''"
     :background-size [(rem 3) (rem 3)]
     :background-position "5px 5px"
     :background-repeat :no-repeat
     ;; :margin-left (rem -3)
     :background-image (str "url('/assets/icons/mf-logo.svg')")
     }
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
      :min-height (rem 8)
      }]
    ]
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
      :color (color :pink)}]]
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
       :text-overflow "ellipsis"
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
        }]
      ]
     ]]
   [:.tracker-section
    {:cursor :pointer
     :transition "width 100ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"
     :width :transactionLogWidth
     :height "100%"
     :display :flex
     :align-items :center
     :justify-content :center}
    [:.accounts
     {:display :flex
      :height "100%"
      :align-items :center
      :justify-content :center}

     [:.active-account-balance
      {:display :block
       :height "100%"
       :min-width (em 9)
       :background-color (color :ticker-background)
       :position :relative
       ;; :font-size (px 18)
       }
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
        :background-image (str "url('/assets/icons/ethereum.svg')")}
       ]
      [:.balance
       (font :bungee)
       {:white-space :nowrap
        :color (color :ticker-color)
        :position :absolute
        :display :block
        :left 0;
        :right 0;
        :width (em 4)
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
        :left 0;
        :right 0;
        :width (em 2)
        :margin-right "auto"
        :margin-left "auto"
        :bottom (em 0.3)}]
      ]]]
   ])
