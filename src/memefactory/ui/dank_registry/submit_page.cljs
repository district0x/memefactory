(ns memefactory.ui.dank-registry.submit-page
  (:require
   [cljs-web3.core :as web3]
   [district.format :as format]
   [district.graphql-utils :as graphql-utils]
   [district.ui.component.form.input :refer [index-by-type file-drag-input with-label chip-input text-input int-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.tiles :refer [meme-image]]
   [memefactory.ui.dank-registry.events :as dr-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log]
   ))

(defn header []
  [:div.submit-info
   [:div.icon]
   [:h2.title "Dank registry - Submit"]
   [:h3.title "Lorem ipsum dolor sit ..."]
   [:div.get-dank-button "Get Dank"]])

(defn param-search-query [param]
  [:search-param-changes {:key (graphql-utils/kw->gql-name param)
                          :db (graphql-utils/kw->gql-name :meme-registry-db)
                          :group-by :param-changes.group-by/key
                          :order-by :param-changes.order-by/applied-on}
   [[:items [:param-change/value :param-change/key]]]])

(defmethod page :route.dank-registry/submit []
  (let [all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        dank-deposit-s (subscribe [::gql/query {:queries [(param-search-query :deposit)]}])
        max-issuance-s (subscribe [::gql/query {:queries [(param-search-query :max-total-supply)]}])
        form-data (r/atom {})
        errors (reaction {:local (let [{:keys [title issuance file-info]} @form-data
                                       max-issuance (get-in @max-issuance-s [:search-param-changes :items 0 :param-change/value] 1)]
                                   (cond-> {:issuance {:hint (str "Max " max-issuance)}}
                                     (empty? title)
                                     (assoc-in [:title :error] "Title cannot be empty")

                                     (not file-info)
                                     (assoc-in [:file-info :error] "No file selected")

                                     (not (try
                                            (let [issuance (js/parseInt issuance)]
                                              (and (< 0 issuance) (<= issuance max-issuance)))
                                            (catch js/Error e nil)))
                                     (assoc-in [:issuance :error] (str "Issuance should be a number between 1 and " max-issuance))))
                          :remote (let [{:keys [file-info]} @form-data]
                                    (cond-> {}
                                      (:error file-info)
                                      (assoc-in [:file-info] (:error file-info))))})
        critical-errors (reaction (index-by-type @errors :error))]
    (fn []
      (let [dank-deposit (get-in @dank-deposit-s [:search-param-changes :items 0 :param-change/value])]
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
                                                 (swap! form-data update-in [:file-info] dissoc :error)
                                                 (log/info "Accepted file" props ::file-accepted))
                             :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                 (swap! form-data assoc :file-info {:error "Non .png file selected"})
                                                 (log/warn "Rejected file" props ::file-rejected))}]]
          [:div.form-panel
           [with-label "Title"
            [text-input {:form-data form-data
                         :errors errors
                         :id :title}]
            {:form-data form-data
             :id :title}]
           [with-label "Tags"
            [chip-input {:form-data form-data
                         :chip-set-path [:search-tags]
                         :ac-options (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                         :chip-render-fn (fn [c] [:span c])
                         :on-change (fn [c])}]
            {:form-data form-data
             :id :search-tags}]
           [with-label "Issuance"
            [int-input {:form-data form-data
                        :errors errors
                        :id :issuance}]
            {:form-data form-data
             :id :issuance}]
           #_[:span.max-issuance (str "Max " max-meme-issuance)] ;; we are showing it on input focus
           [:div.submit
            [:button {:on-click (fn []
                                  (dispatch [::dr-events/upload-meme @form-data dank-deposit])
                                  (reset! form-data {}))
                      :disabled (not (empty? @critical-errors))}
             "Submit"]
            [:span.dank (format/format-token (web3/from-wei dank-deposit :ether) {:token "DANK"})]]]]]]))))
