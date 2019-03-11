(ns memefactory.ui.dank-registry.submit-page
  (:require
   [cljs-web3.core :as web3]
   [district.format :as format]
   [district.ui.component.form.input :refer [index-by-type file-drag-input with-label chip-input text-input int-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.events :as router-events]
   [district.ui.web3-account-balances.subs :as balance-subs]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.tiles :refer [meme-image]]
   [memefactory.ui.dank-registry.events :as dr-events]
   [re-frame.core :as re-frame]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log :refer [spy]]
   ))

(defn header []
  [:div.submit-info
   [:div.icon]
   [:h2.title "Dank registry - Submit"]
   [:h3.title "Submit a new meme to the registry for consideration"]
   [:div.get-dank-button {:on-click #(dispatch [::router-events/navigate :route.get-dank/index])}
    "Get Dank"]])

(defn submit-panels [{:keys [deposit max-total-supply] :as params}]
  (let [all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        form-data (r/atom {:issuance 1})
        max-tags-allowed 6
        errors (reaction {:local (let [{:keys [title issuance file-info]} @form-data
                                       entered-tag (get @form-data "txt-:search-tags")
                                       max-issuance (or max-total-supply 1)]
                                   (cond-> {:issuance {:hint (str "Max " max-issuance)}}
                                     (empty? title)
                                     (assoc-in [:title :error] "Title cannot be empty")

                                     (not file-info)
                                     (assoc-in [:file-info :error] "No file selected")

                                     (not (try
                                            (let [issuance (js/parseInt issuance)]
                                              (and (< 0 issuance) (<= issuance max-issuance)))
                                            (catch js/Error e nil)))
                                     (assoc-in [:issuance :error] (str "Issuance should be a number between 1 and " max-issuance))

                                     (> (count (get @form-data :search-tags)) max-tags-allowed)
                                     (assoc-in [:search-tags :error] (str "Max tags allowed " max-tags-allowed))

                                     (and (not (nil? entered-tag))
                                          (not (= 0 (compare
                                                     entered-tag
                                                     (apply str (re-seq #"[a-zA-Z]" entered-tag))))))
                                     (assoc-in [:search-tags :error] "Only alphanumerics allowed")

                                     (not= 0 (compare (count (get @form-data :search-tags))
                                                   (count (filter
                                                           #(= 0 (compare
                                                                  %
                                                                  (apply str (re-seq #"[a-zA-Z]" %))))
                                                           (get @form-data :search-tags)))))
                                     (assoc-in [:search-tags :error] "Use only alphanumeric characters")))
                          :remote (let [{:keys [:file-info :search-tags]} @form-data]
                                    (cond-> {}
                                      (:error file-info)
                                      (assoc-in [:file-info] (:error file-info))

                                      (:error search-tags)
                                      (assoc-in [:search-tags] (:error search-tags))

                                      ))})
        critical-errors (reaction (index-by-type @errors :error))
        account-balance (subscribe [::balance-subs/active-account-balance :DANK])]

    (fn []

      (log/debug "@form-data" @form-data)

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
                                                (js/console.log "Veryfing acceptance of file of type : " type " and size : " size)
                                                (and (#{"image/png" "image/gif" "image/jpeg"} type)
                                                     (< size 1500000)))
                            :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                (swap! form-data update-in [:file-info] dissoc :error)
                                                (log/info (gstring/format "Accepted file %s %s %s" name type size) ::file-accepted))
                            :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                (swap! form-data assoc :file-info {:error "Non .png .jpeg or .gif file selected with size less than 1.5 Mb"})
                                                (log/warn (gstring/format "Rejected file %s %s %s" name type size) ::file-rejected))}]]
         [:div.form-panel
          [with-label "Title"
           [text-input {:form-data form-data
                        :errors errors
                        :id :title
                        :dom-id :ftitle
                        :maxLength 60}]
           {:form-data form-data
            :id :title
            :for :ftitle}]
          [with-label "Tags"
           [chip-input {:form-data form-data
                        :chip-set-path [:search-tags]
                        :ac-options (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                        :chip-render-fn (fn [c] [:span c])
                        :id :search-tags
                        :select-keycodes #{13 188 32}
                        :errors errors
                        :dom-id :search-tags}]
           {:form-data form-data
            :for :search-tags
            :id :search-tags}]
          [with-label "Issuance"
           [text-input {:form-data form-data
                        :errors errors
                        :id :issuance
                        :dom-id :issuance}]
           {:form-data form-data
            :id :issuance
            :for :issuance}]
          #_[:span.max-issuance (str "Max " max-meme-issuance)] ;; we are showing it on input focus
          [:div.submit
           [:button {:on-click (fn []
                                 (dispatch [::dr-events/upload-meme @form-data deposit])
                                 (reset! form-data {}))
                     :disabled (or (not (empty? @critical-errors))
                                   (< @account-balance deposit))}
            "Submit"]
           [:span.dank (format/format-token (web3/from-wei deposit :ether) {:token "DANK"})]]
          (when (< @account-balance deposit)
            [:div.not-enough-dank "You don't have enough DANK token to submit a meme"])]]]])))

(defmethod page :route.dank-registry/submit []
  (let [params @(subscribe [:memefactory.ui.config/memefactory-db-params])]
    (when params
      [submit-panels params])))
