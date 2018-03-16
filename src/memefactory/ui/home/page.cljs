(ns memefactory.ui.home.page
  (:require
    [district.ui.component.page :refer [page]]
    [memefactory.ui.home.events]))

(defmethod page :route/home []
  [:div "Hello Memee!"])