(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [garden.selectors :as sel]
            [garden.units :refer [px em]]))

(def menu-gutter (px 15))

(defstyles core
  [:.app-container
   [:.app-menu
    {:overflow-x :hidden
     :overflow-y :scroll
     :width (px 190)
     :background (color :white)}
    [(sel/> :.menu-content :.node :.node-content :.item)
     (border-top {:color (color :light-violet)})]
    [:ul.node {:padding-right (em 0)}]
    [:ul.node [:ul.node {:padding-right (em 2)}]]
    [:ul.node
     {:list-style :none}
     [:.item
      {:display :flex
       :align-items :center
       :padding-top menu-gutter
       :padding-bottom menu-gutter}
      [:a
       {:color (color :menu-text-color)}
       [:&:hover
        {:color (color :menu-text-color-hover)}]]
      [:&:before
       {:display :block
        :background-size [(em 2) (em 2)]
        :background-repeat :no-repeat
        :height (em 2)
        :width (em 2)
        :float :left}]
      (let [icons [[:dankregistry "dankregistry"]
                   [:about "about"]
                   [:marketplace "marketplace"]
                   [:how-it-works "howitworks"]
                   [:leaderboard "leaderboard"]
                   [:my-meme-folio "mymemefolio"]
                   [:my-settings "mysettings"]]]
        (mapv (fn [[cls img]]
                [(keyword (str "&." (name cls) ":before"))
                 {:content "''"
                  :background-image (str "url('/assets/icons/" img ".png')")}])
              icons))]]]])
