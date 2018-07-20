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
   [district.ui.server-config.subs :as config-subs]
   [memefactory.ui.components.tiles :refer [meme-image]]
   [reagent.ratom :refer [reaction]]))

(defn header []
  [:div.submit-info
   [:div.icon]
   [:h2.title "Dank registry - Submit"]
   [:h3.title "Lorem ipsum dolor sit ..."]
   [:div.get-dank-button "Get Dank"]])

(def max-meme-issuance 500000)

(defmethod page :route.dank-registry/submit []
  (let [all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        dank-deposit (subscribe [::config-subs/config :deployer :initial-registry-params :meme-registry :deposit])
        form-data (r/atom {})
        errors (reaction {:local (let [{:keys [title issuance file-info]} @form-data]
                                   (cond-> {}
                                     (empty? title)
                                     (assoc :title "Meme title is mandatory")
                                     
                                     (not file-info)
                                     (assoc :file-info "No file selected")
                                     
                                     (not (try
                                            (< 0 (js/parseInt issuance) max-meme-issuance)        
                                            (catch js/Error e nil)))
                                     (assoc :issuance (str "Issuance should be a number between 1 and " max-meme-issuance))))}) ]
   (fn []
     [app-layout
      {:meta {:title "MemeFactory"
              :description "Description"}}
      [:div.dank-registry-submit-page
       [:section.submit
        [header]]
       [:section.upload
        [:div.image-panel
         [file-drag-input {:form-data form-data 
                           :id :file-info
                           :errors errors
                           :file-accept-pred (fn [{:keys [name type size] :as props}]
                                               (= type "image/png"))
                           :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                               (prn "Accepted " props))
                           :on-file-rejected (fn [{:keys [name type size] :as props}]
                                               (prn "Rejected " props))}]]
        [:div.form-panel
         [with-label "Title"
          [text-input {:form-data form-data
                       :id :title
                       :errors errors}]]
         [with-label "Tags"
          [chip-input {:form-data form-data
                       :chip-set-path [:search-tags]
                       :ac-options (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                       :chip-render-fn (fn [c] [:span c])
                       :on-change (fn [c])}]]
         [with-label "Issuance"
          [text-input {:form-data form-data
                       :id :issuance
                       :errors errors}]]
         [:span.max-issuance (str "Max " max-meme-issuance)]
         [:div.submit
          [:button {:on-click (fn []
                                (dispatch [::dr-events/upload-meme @form-data @dank-deposit])
                                (reset! form-data {}))
                    :disabled (not (empty? (:local @errors)))}
           "Submit"]
          [:span.dank (format/format-token @dank-deposit  {:token "DANK"})]]]]
       ]])))
