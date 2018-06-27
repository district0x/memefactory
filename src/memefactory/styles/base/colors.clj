(ns memefactory.styles.base.colors)

(def names->colors {:pink "#ff0090"
                    :ticker-background "#ff0090"
                    :ticker-color "#FFFFFF"
                    :ticker-token-color "#ff80c8"
                    :section-caption "#ff0090"
                    :section-subcaption "#47608e"
                    :border-line "#eff1f5"
                    :search-input-bg "#f2f4fb"
                    :menu-text "#47608e"
                    :menu-text-hover "#5b6d8e"
                    :main-content-bg "#f2f4fb"
                    :meme-panel-bg "#FFFFFF"
                    :new-meme-icon-bg "#FF0090"
                    :rare-meme-icon-bg "#1ff2c8"
                    :random-meme-icon-bg "#ffcc00"
                    :violet "#2e1f37"
                    :light-violet "rgba(116,100,126,.15)"
                    :light-grey "rgba(255,255,255,.7)" })

(defn color [name]
  (get names->colors name name))
