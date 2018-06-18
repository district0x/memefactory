(ns memefactory.styles.base.fonts)

(def names->fonts {:bungee {:font-family "'Bungee', cursive"}})

(defn font [name]
  (get names->fonts name name))
