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

(def bar-height 50) ;; px


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
     :background-image (str "url('/assets/icons/mf-logo.svg')")}
    [:&:before
     (font :bungee)
     {:content "'MEME FACTORY'"
      :font-size (px 18)
      :line-height (em 1.2)
      :color (color :menu-logo)
      :width (rem 5)
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
      :line-height "100%"
      :color (color :pink)
      :z-index 100}]]
   (for-media-min :tablet [:&
                           {:display :none}])]
  [:.app-bar
   {:height (px bar-height)
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
     :padding "0 10px"}
    (for-media-max :computer
                   [:&
                    {:width (px 250)}])
    [:.active-account
     {:overflow "hidden"
      :white-space "nowrap"
      :text-overflow :ellipsis
      :width "100%"}
     [:i.dropdown.icon:before
      {:content "url('/assets/icons/dropdown.png')" ;;No, we can't just bg scale, thanks upstream !important
       :display :inline-flex
       :transform "scale(.5)"}]]]

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
       :background-image "url('/assets/icons/search.png')"
       :top (em 0.4)
       :right (em 0.6)
       :position :absolute
       :height (em 1.3)
       :width (em 1.3)}]]]
   [:.tracker-section
    {:cursor :pointer
     :transition "width 100ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"
     :width (px 320)
     :height "100%"
     :display :flex
     :align-items :center
     :justify-content :center}]])
