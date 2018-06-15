(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]))

(def menu-gutter (px 15))

(defstyles core
  [:.app-container
   [:.app-menu
    {:overflow-x :hidden
     :overflow-y :scroll
     :width (rem 20)
     :background (color :white)}
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
              icons))]]]
   ])
