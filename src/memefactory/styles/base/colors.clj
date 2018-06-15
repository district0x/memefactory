(ns memefactory.styles.base.colors)

(def names->colors {:pink "#ff0090"
                    :menu-text-color "#47608e"
                    :menu-text-color-hover "#5b6d8e"
                    :violet "#2e1f37"
                    :light-violet "rgba(116,100,126,.15)"
                    :light-grey "rgba(255,255,255,.7)" })

(defn color [name]
  (get names->colors name name))
