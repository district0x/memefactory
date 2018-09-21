(ns memefactory.styles.base.fonts)

(def names->fonts {:bungee {:font-family "'Bungee', cursive"}
                   :filson {:font-family "filson-soft,sans-serif"}})

(defn font [name]
  (get names->fonts name name))
