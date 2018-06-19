(ns memefactory.ui.utils
  (:require [district.ui.router.utils :as router-utils]
            [cemerick.url :as url]))

(defn path [& args]
  (str "#" (apply router-utils/resolve args)))

(defn path-with-query [path query-params-map]
  (str path "?" (url/map->query query-params-map)))
