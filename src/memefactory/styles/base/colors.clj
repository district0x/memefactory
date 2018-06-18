(ns memefactory.styles.base.colors)

(def names->colors {:pink "#ff0090"
                    :ticker-background "#ff0090"
                    :ticker-color "#ffFFFF"
                    :ticker-token-color "#ff80c8"
                    :border-line "#eff1f5"
                    :menu-text "#47608e"
                    :menu-text-hover "#5b6d8e"
                    :main-content-bg "#f2f4fb"
                    :violet "#2e1f37"
                    :light-violet "rgba(116,100,126,.15)"
                    :light-grey "rgba(255,255,255,.7)" })

(defn color [name]
  (get names->colors name name))
