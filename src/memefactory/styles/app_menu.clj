(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [garden.selectors :as sel]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [garden.units :refer [pt px em rem]]))

(def menu-gutter (px 8))

(defstyles core
  [:.app-container
   [:.app-menu
    {:transition "left 0.3s"}
    (for-media-max :tablet
                   [:&
                    {:z-index 10
                     :display :block
                     :left 0
                     :width (px 270)
                     :background-color (color :white)
                     :min-height (px 1800)
                     :position :absolute}])
    [:&.closed
     (for-media-max :tablet
                    [:&
                     {:left (px -270)}])]
    {:position "relative"}
    [(sel/> :.menu-content :.node :.node-content :.item)
     (border-top {:color (color :border-line)})
     {:padding-top (em 1)
      :padding-bottom (em 1)}]
    [:.menu-content
     [:.mf-logo
      {:cursor :pointer
       :padding-top (em 3)
       :padding-bottom (em 1)
       :padding-left (em 1.7)
       :padding-right (em 1.7)
       :display :grid
       :grid-template-columns "50% 50%"}
      (for-media-max :tablet
                   [:&
                    {:grid-template-columns "30% 50%"}])
      [:img
       {:width (em 4)}]
      [:span
       (font :bungee)
       {:font-size (px 18)
        :line-height (em 1.4)
        :color (color :menu-logo)}]]



     [:ul.node {:padding-left (em 0)}
      [:.item.active
       [:a {:color (color :pink)
            :cursor :pointer}]]
      [:.item.disabled
       [:a {:opacity 0.5}]]]

     [:ul.node
      [:.item {:font-size (pt 12)}]
      [:ul.node {:padding-left (em 0)}
       [:.item {:font-size (pt 10)}]]]

     [:ul.node
      {:list-style :none}
      [:.item
       {:display :flex
        :align-items :center
        :padding-top menu-gutter
        :padding-bottom menu-gutter}
       [:a
        {:color (color :menu-text)
         :margin-left (rem 4)
         :cursor :pointer}
        [:&:hover
         {:color (color :pink)}]
        [:&:before
         {:display :block
          :background-size [(rem 1.8) (rem 1.8)]
          :background-repeat :no-repeat
          :margin-left (rem -3)
          :margin-top (rem -0.2)
          :position :absolute
          :height (rem 1.8)
          :width (rem 1.8)}]]

       (let [icons [[:dankregistry "dankregistry"]
                    [:about "about"]
                    [:marketplace "marketplace"]
                    [:how-it-works "how-it-works"]
                    [:leaderboard "leaderboard"]
                    [:memefolio "mymemefolio"]
                    [:my-settings "mysettings"]
                    [:faucet "dank-logo"]]]
         (mapv (fn [[cls img]]
                 [(keyword (str "&." (name cls)))
                  [:a:before
                   {:content "''"
                    :background-image (str "url('/assets/icons/" img ".svg')")}]])
               icons))]
      [:.item.faucet
       [:a:before {:background-size "3.1rem"
                   :background-position (px -7)
                   :height (rem 2.1)
                   :width (rem 2.2)
                   :margin-top (px -6)}]]]]
    [:.district0x-banner
     (font :filson)
     {:padding-left (em 3)
      :color (color :menu-text)
      ;; :font-weight :bold
      :line-height (em 1.8)
      :font-size (pt 8)
      :position :absolute
      :overflow :visible
      :display :block
      :height (em 8)
      :right 0
      :left 0
      :padding-bottom (em 20)
      :bottom (em 0)
      :background-size ["100%"  "100%"]
      :background-position "right bottom";
      :background-repeat :no-repeat
      :background-image "url('/assets/icons/conveyer.png')"}
     [:a {:text-decoration :underline}]
     [:.logo {:content "url('/assets/icons/district0x.svg')"
              :height (em 2)
              ;; :background-position "left bottom";
              ;; :background-repeat :no-repeat
              ;; :background-image ""
              :margin-top (em -5)
              :margin-bottom (em 1)}]]]])
