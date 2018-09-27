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
    (for-media-max :tablet
                   [:&
                    {:z-index 2
                     :display :block
                     :right (px 50)
                     :left 0
                     :min-height (px 1800)
                     :position :absolute}])
    [:&.closed
     (for-media-max :tablet
                    [:&
                     {:display :none}])]
    {:position "relative"}

    [:.menu-content
     [:.mf-logo 
      {:cursor :pointer
       :padding-top (em 3)
       :padding-bottom (em 1)
       :padding-left (em 1.7)
       :padding-right (em 1.7)
       :display :grid
       :grid-template-columns "50% 50%"}
      [:img
       {:width (em 4)}]
      [:span
       (font :bungee)
       {:font-size (px 18)
        :line-height (em 1.4)
        :color (color :menu-logo)}]]
     
     [(sel/> :.menu-content :.node :.node-content :.item)
      (border-top {:color (color :border-line)})
      {:padding-top (em 1)
       :padding-bottom (em 1)}]
     
     [:ul.node {:padding-left (em 0)}]

     [:ul.node
      [:.item {:font-size (pt 13)}]
      [:ul.node {:padding-left (em 0)}
       [:.item {:font-size (pt 11)}]]]
     
     [:ul.node
      {:list-style :none}
      [:.item
       {:display :flex
        :align-items :center
        :padding-top menu-gutter
        :padding-bottom menu-gutter}
       [:a
        {:color (color :menu-text)
         :margin-left (rem 4)}
        [:&:hover
         {:color (color :menu-text-hover)}]
        [:&:before
         {:display :block
          :background-size [(rem 2) (rem 2)]
          :background-repeat :no-repeat
          :margin-left (rem -3)
          :margin-top (rem -0.2)
          :position :absolute
          :height (rem 2)
          :width (rem 2)}]]
       (let [icons [[:dankregistry "dankregistry"]
                    [:about "about"]
                    [:marketplace "marketplace"]
                    [:how-it-works "howitworks"]
                    [:leaderboard "leaderboard"]
                    [:my-meme-folio "mymemefolio"]
                    [:my-settings "mysettings"]]]
         (mapv (fn [[cls img]]
                 [(keyword (str "&." (name cls)))
                  [:a:before
                   {:content "''"
                    :background-image (str "url('/assets/icons/" img ".png')")}]])
               icons))]]]
    [:.district0x-banner
     (font :filson)
     {:padding-left (em 3)
      :color (color :menu-text)
      :font-weight :bold
      :line-height (em 1.4)
      :position :absolute
      :overflow :visible
      :display :block
      :height (em 10)
      :right 0
      :left 0
      :padding-bottom (em 20)
      :bottom (em 0)
      :background-size ["100%"  "100%"]
      :background-position "right bottom";
      :background-repeat :no-repeat
      :background-image "url('/assets/icons/conveyer.png')"}
     [:.logo {:content "url('/assets/icons/district0x-footer-logo.png')"
              :height (em 2)
              ;; :background-position "left bottom";
              ;; :background-repeat :no-repeat
              ;; :background-image ""
              :margin-top (em -5)
              :margin-bottom (em 1)}]
     
     ]]])
