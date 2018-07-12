(ns memefactory.ui.dank-registry.submit-page
  (:require
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.form.input :refer [file-drag-input with-label chip-input text-input]]
   [memefactory.ui.dank-registry.events :as dr-events]
   [re-frame.core :as re-frame]
   [district.ui.graphql.subs :as gql]
   [district.format :as format]
   [district.ui.server-config.subs :as config-subs]))

(defn header []
  [:div.header
   [:h2 "Dank registry - Submit"]
   [:h3 "Lorem ipsum dolor sit ..."]
   [:div [:div "Get Dank"]]])

(defmethod page :route.dank-registry/submit []
  (let [all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        dank-deposit (subscribe [::config-subs/config :deployer :initial-registry-params :meme-registry :deposit])
        form-data (r/atom {})]
   (fn [] 
     [app-layout
      {:meta {:title "MemeFactory"
              :description "Description"}}
      [:div.dank-registry-submit
       [header]
       [:div.panels
        [:div.image-panel
         [file-drag-input {:form-data form-data
                           :id :file-info
                           :file-accept-pred (fn [{:keys [name type size] :as props}]
                                               (= type "image/png"))
                           :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                               (prn "Accepted " props))
                           :on-file-rejected (fn [{:keys [name type size] :as props}]
                                               (prn "Rejected " props))}]]
        [:div.form-panel
         [with-label "Title"
          [text-input {:form-data form-data
                       :id :title}]]
         [with-label "Tags"
          [chip-input {:form-data form-data
                       :chip-set-path [:search-tags]
                       :ac-options (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                       :chip-render-fn (fn [c] [:span c])
                       :on-change (fn [c])}]]
         [with-label "Issuance"
          [text-input {:form-data form-data
                       :id :issuance}]]
         [:span.max-issuance "Max 500.000"]
         [:div.submit
          [:button {:on-click (fn []
                                (dispatch [::dr-events/upload-meme @form-data @dank-deposit]))}
           "Submit"]
          [:span.dank (format/format-token @dank-deposit  {:token "DANK"})]]]]]])))
