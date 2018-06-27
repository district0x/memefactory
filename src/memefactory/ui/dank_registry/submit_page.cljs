(ns memefactory.ui.dank-registry.submit-page
  (:require
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.form.input :refer [file-drag-input]]))

(defn header []
  [:div.header
   [:h2 "Dank registry - Submit"]
   [:h3 "Lorem ipsum dolor sit ..."]
   [:div [:div "Get Dank"]]])

(defmethod page :route.dank-registry/submit []
  (fn []
    (let [form-data (r/atom {})]
     [app-layout
      {:meta {:title "MemeFactory"
              :description "Description"}}
      [:div.dank-registry-submit
       [header] 
       [file-drag-input {:form-data form-data
                         :file-accept-pred (fn [{:keys [name type size] :as props}]
                                             (prn "Do we accept " props)
                                             (= type "image/png"))
                         :on-file-accepted (fn [{:keys [name type size url-data file] :as props}]
                                             (prn "Accepted " props))
                         :on-file-rejected (fn [{:keys [name type size] :as props}]
                                             (prn "Rejected " props))}]]])))
