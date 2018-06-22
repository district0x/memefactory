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
  [:.app-bar
   {:height bar-height
    :background-color (color "white")
    :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :flex
    :align-items :center
    :justify-content :space-between}
   (for-media-max :tablet [:&
                           {:background-color (color "blue")}])
   [:.left-section
    {:align-items :left
     :width (px 250)
     :padding "0 10px"}
    [:.active-account-select
     [:i.dropdown.icon:before
      {:content "url('/assets/icons/dropdown.png')" ;;No, we can't just bg scale, thanks upstream !important
       :display :inline-flex
       :transform "scale(.5)"
       }]]
    [:.active-address-select
     [:.item
      {:white-space :nowrap
       :overflow :hidden
       :text-overflow :ellipsis}]]
    (for-media-max :tablet
                   [:&
                    {:width bar-height}
                    [:.active-address-select
                     {:display :none}]])]

   [:.middle-section
    [:.search
     {:position :relative}
     [:input
      {:background-color (color :search-input-bg)
       :border-radius (em 1)
       :border-style :none
       :padding-left (em 1)
       :height (em 2)
       :width (em 20)}
      [:&:focus {:outline :none}]]
     [:.go-button
      {:display :block
       :content "''"
       :background-size [(rem 2) (rem 2)]
       :background-repeat :no-repeat
       :background-color (color :red)
       ;; :margin-left (rem -3)
       ;; :margin-top (rem -0.2)
       :top 0
       :right (em 1)
       :position :absolute
       :height (rem 2)
       :width (rem 2)}
      ]
     ]]
   [:.right-section
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
        :background-image (str "url('/assets/icons/ethereum.png')")}
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
   [:.icon.hamburger
    {:cursor :pointer
     :font-size (px 29)
     :line-height "100%"
     :color (color "light-green")}
    (for-media-min :tablet [:&
                            {:display :none}])]])
