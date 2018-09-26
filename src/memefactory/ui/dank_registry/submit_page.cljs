(ns memefactory.ui.dank-registry.submit-page
  (:require
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.form.input :refer [index-by-type
                                             file-drag-input
                                             with-label
                                             chip-input
                                             text-input
                                             int-input]]
   [memefactory.ui.dank-registry.events :as dr-events]
   [re-frame.core :as re-frame]
   [district.ui.graphql.subs :as gql]
   [district.format :as format]
   [memefactory.ui.components.tiles :refer [meme-image]]
   [reagent.ratom :refer [reaction]]
   [cljs-web3.core :as web3]
   [district.graphql-utils :as graphql-utils]))



(defn header []
  [:div.submit-info
   [:div.icon]
   [:h2.title "Dank registry - Submit"]
   [:h3.title "Lorem ipsum dolor sit ..."]
   [:div.get-dank-button "Get Dank"]])

(def max-meme-issuance 500000)

(defmethod page :route.dank-registry/submit []
  (let [all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        form-data (r/atom {})
        errors (reaction {:local (let [{:keys [title issuance file-info]} @form-data]
                                   (cond-> {:issuance {:hint (str "Max " max-meme-issuance)}}
                                     (empty? title)
                                     (assoc-in [:title :error] "Meme title is mandatory")

                                     (not file-info)
                                     (assoc-in [:file-info :error] "No file selected")

                                     (not (try
                                            (< 0 (js/parseInt issuance) max-meme-issuance)
                                            (catch js/Error e nil)))
                                     (assoc-in [:issuance :error] (str "Issuance should be a number between 1 and " max-meme-issuance))))})
        critical-errors (reaction (index-by-type @errors :error))]
    (fn []
      (let [dank-deposit (get-in (look @(subscribe [::gql/query {:queries [[:search-param-changes {:key (graphql-utils/kw->gql-name :deposit)
                                                                            :db (graphql-utils/kw->gql-name :meme-registry-db)
                                                                            :group-by :param-changes.group-by/key
                                                                            :order-by :param-changes.order-by/applied-on}
                                                                            [[:items [:param-change/value]]]]]}]))
                                 [:search-param-changes :items 0 :param-change/value])]
        [app-layout
         {:meta {:title "MemeFactory"
                 :description "Description"}}
         [:div.dank-registry-submit-page
          [:section.submit-header
           [header]]
          [:section.upload
           [:div.image-panel
            [file-drag-input {:form-data form-data
                              :id :file-info
                              :errors errors
                              :label "Upload a file"
                              :file-accept-pred (fn [{:keys [name type size] :as props}]
                                                  (= type "image/png"))
                              :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                  (prn "Accepted " props))
                              :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                  (prn "Rejected " props))}]]
           [:div.form-panel
            ;; [:div (str (:local @errors))]
            ;; [:div (str @critical-errors)]
            
            [text-input {:form-data form-data
                         :placeholder "Title"
                         :errors errors
                         :id :title}]
            [chip-input {:form-data form-data
                         :chip-set-path [:search-tags]
                         :placeholder "Tags"
                         :ac-options (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                         :chip-render-fn (fn [c] [:span c])
                         :on-change (fn [c])}]
            [int-input {:form-data form-data
                        :placeholder "Issuance"
                        :errors errors
                        :id :issuance}]
            #_[:span.max-issuance (str "Max " max-meme-issuance)] ;; we are showing it on input focus
            [:div.submit
             [:button {:on-click (fn []
                                   (dispatch [::dr-events/upload-meme @form-data dank-deposit])
                                   (reset! form-data {}))
                       :disabled (not (empty? @critical-errors))}
              "Submit"]
             [:span.dank (format/format-token (web3/from-wei dank-deposit :ether) {:token "DANK"})]]]]]]))))
