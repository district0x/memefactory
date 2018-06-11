(ns memefactory.ui.utils
  (:require [district.ui.router.utils :as router-utils]))

(defn path [& args]
  (str "#" (apply router-utils/resolve args)))
