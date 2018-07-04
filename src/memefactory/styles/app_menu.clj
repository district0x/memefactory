(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]))

(def menu-gutter (px 15))

(defstyles core
  [:.app-container
   [:.app-menu
    {:position "relative"}
    [:&:before
     {:content "''"
      :background-size [(rem 4) (rem 4)]
      :background-repeat :no-repeat
      ;; :margin-left (rem -3)
      :top (rem 3)
      :left (rem 2)
      :position :absolute
      :height (rem 4)
      :width (rem 4)
      :background-image (str "url('/assets/icons/mememouth.png')")}]
    
    {:overflow-x :hidden
     :overflow-y :auto
     :background (color :white)}
    [:.menu-content
     [:&:before
      (font :bungee)
      {:content "'MEME FACTORY'"
       :font-size (px 21)
       :line-height (em 1.2)
       :color (color :menu-logo)
       ;; :margin-right (em 5)
       :width (rem 5)
       :padding-top (rem 2.8)
       :padding-left (rem 7)
       :position :relative
       :display :block
       :min-height (rem 8)
       }]]
    [(sel/> :.menu-content :.node :.node-content :.item)
     (border-top {:color (color :border-line)})
     ]
    [:ul.node {:padding-left (em 0)}]
    [:ul.node
     [:.item {:font-size (pt 16)}]
     [:ul.node {:padding-left (em 0)}
      [:.item {:font-size (pt 14)}]]]
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
              icons))]]
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
              :margin-bottom (em 1)
              }]
     #_[:&:after
      {;;:content "url('/assets/icons/conveyer.png')"
       :content "''"
       :background-size ["100%"  "100%"]
       :background-repeat :no-repeat
       ;; :margin-left (rem -3)
       ;; :top (rem 3)
       ;; :left (rem 2)
       :display :block
       :position :absolute
       ;; :bottom 0
       ;; :left 0
       :right 0
       :bottom 0
       ;; :height (em 14)
       :background-position :bottom;
       :width "100%";;(em 24)
       :background-image "url('/assets/icons/conveyer.png')"}]
     ]
    ]])
