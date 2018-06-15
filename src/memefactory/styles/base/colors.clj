(ns memefactory.styles.base.colors)

(def names->colors {:pink "#ff0090"
                    :border-line "#eff1f5"
                    :menu-text "#47608e"
                    :menu-text-hover "#5b6d8e"
                    :violet "#2e1f37"
                    :light-violet "rgba(116,100,126,.15)"
                    :light-grey "rgba(255,255,255,.7)" })

(defn color [name]
  (get names->colors name name))
