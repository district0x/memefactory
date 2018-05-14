(ns memefactory.styles.component.app-bar
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [px]]))

(def bar-height (px 50))

(defstyles core
  [:.app-bar
   {:height bar-height
    :background-color (color "white")
    :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :flex
    :align-items :center
    :justify-content :space-between}
   (for-media-min :tablet [:&
                           {:background-color (color "blue")}])
   [:.left-section
    {:align-items :left
     :width (px 250)
     :padding "0 10px"}
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

   [:.right-section
    {:background-color (color "purple")
     :color (color "whiteTextColor")
     :cursor :pointer
     :transition "width 100ms cubic-bezier(0.23, 1, 0.32, 1) 0ms"
     :width :transactionLogWidth
     :height "100%"
     :display :flex
     :align-items :center
     :justify-content :center}
    [:.active-address-balance
     {:font-size (px 18)}]
    [:i.icon
     {:font-size (px 24)
      :margin-left (px 24)}]]

   [:.icon.hamburger
    {:cursor :pointer
     :font-size (px 29)
     :line-height "100%"
     :color (color "light-green")}
    (for-media-min :tablet [:&
                            {:display :none}])]])
