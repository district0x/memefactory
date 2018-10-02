(ns memefactory.ui.components.challenge-list
  (:require [district.time :as time]
            [reagent.core :as r]
            [react-infinite]
            [district.ui.graphql.subs :as gql]
            [goog.string :as gstring]
            [district.time :as time]
            [cljs-time.extend]
            [district.format :as format]
            [cljs-time.core :as t]
            [district.ui.component.form.input :refer [select-input with-label]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [print.foo :refer [look] :include-macros true]
            [memefactory.ui.utils :as mf-utils]
            [memefactory.ui.components.tiles :refer [meme-image]]
            [memefactory.ui.components.tiles :as tiles]
            [memefactory.ui.utils :as utils]))

(def react-infinite (r/adapt-react-class js/Infinite))

(def page-size 1)

(defn build-challenge-query [{:keys [data after include-challenger-info? query-params active-account]}]
  (let [{:keys [:order-by :order-dir]} data]
    (look [:search-memes
      (cond-> (merge {:first page-size} query-params)
        after                   (assoc :after after)
        order-by                (assoc :order-by (keyword "memes.order-by" order-by))
        order-dir               (assoc :order-dir (keyword order-dir)))
      [:total-count
       :end-cursor
       :has-next-page
       [:items (cond-> [:reg-entry/address
                        :reg-entry/created-on
                        :reg-entry/challenge-period-end
                        :reg-entry/status
                        :challenge/comment
                        :meme/total-supply
                        :meme/image-hash
                        :meme/title
                        [:meme/tags [:tag/name]]
                        [:reg-entry/creator [:user/address
                                             :user/creator-rank
                                             :user/total-created-memes
                                             :user/total-created-memes-whitelisted]]]
                 include-challenger-info? (conj [:challenge/challenger [:user/address
                                                                        :user/creator-rank
                                                                        :user/total-created-memes
                                                                        :user/total-created-memes-whitelisted]])
                 active-account (into [[:challenge/vote {:vote/voter active-account}
                                        [:vote/secret-hash
                                         :vote/revealed-on
                                         :vote/option
                                         :vote/amount]]
                                       [:challenge/vote-winning-vote-option {:vote/voter active-account}]
                                       [:challenge/all-rewards {:user/address active-account}]]))]]])))

(defn user-info [user class]
  [:ol {:class class}
   [:li "Rank: " [:span (gstring/format "#%d (?)" (or (:user/creator-rank user) 0))]]
   [:li "Success Rate: " [:span (gstring/format "%d/%d (%d%%)"
                                                          (:user/total-created-memes-whitelisted user)
                                                          (:user/total-created-memes user)
                                                          (/ (* 100 (:user/total-created-memes-whitelisted user))
                                                             (:user/total-created-memes user)))]] 
   [:li "Address: " [:span.address (-> user :user/address)]]])

(defn challenge [{:keys [:entry :include-challenger-info? :action-child] }]
  (let [{:keys [:reg-entry/address :reg-entry/created-on :reg-entry/challenge-period-end
                :meme/total-supply :meme/image-hash :reg-entry/creator :meme/title
                :meme/tags :challenge/challenger :challenge/comment]} entry]
    [:div.challenge
     #_[:div (str "ENTRY " address)] ;; TODO remove (only for debugging)
     (cond-> [:div.info
              [:h2 title]
              [:ol.meme
               [:li "Created: " [:span (-> (time/time-remaining (t/date-time (utils/gql-date->date created-on)) (t/now))
                                                     format/format-time-units
                                                     (str " ago"))]]
               [:li "Challenge period ends in: " [:span (-> (time/time-remaining (t/now) (utils/gql-date->date challenge-period-end))
                                                                      format/format-time-units)]]
               [:li "Issued: " [:span total-supply]]]
              [:h3 "Creator"]
              [user-info creator :creator]
              [:span.challenge-comment comment]
              [:ol.tags
               (for [{:keys [:tag/name]} tags]
                 [:li.tag {:key name}
                  name])]]
       include-challenger-info? (into [[:h3 "Challenger"]
                                       [user-info challenger :challenger]]))
     
     [:div.meme-tile
      [tiles/meme-front-tile {} entry]]
     [:div.action
      [action-child entry]]]))

(defn challenge-list [{:keys [include-challenger-info? query-params action-child active-account key sort-options]}]
  (let [form-data (r/atom {:order-by (-> sort-options first :key)})]
    (fn [{:keys [include-challenger-info? query-params action-child active-account key]}]
      (let [params {:data @form-data
                    :include-challenger-info? include-challenger-info?
                    :query-params query-params
                    :active-account active-account}
            re-search (fn [after]
                        (dispatch [:district.ui.graphql.events/query
                                   {:query {:queries [(build-challenge-query (assoc params :after after))]}
                                    :id {:params params :key key}}]))
            meme-search (subscribe [::gql/query {:queries [(build-challenge-query params)]}
                                    {:id {:params params :key key}}])
            all-memes (->> @meme-search
                           (mapcat (fn [r] (-> r :search-memes :items)))
                           ;; TODO remove this, don't know why subscription is returning nil item

                           (remove #(nil? (:reg-entry/address %))))]
        (.log js/console "ALL Rendering here" all-memes)
         (println "All memes " (map :reg-entry/address all-memes))
        (if (:graphql/loading? @meme-search)
          [:div "Loading..."]
          [:div.challenges.panel
           [:div.controls
            [select-input {:form-data form-data
                           :id :order-by
                           :options sort-options
                           :on-change #(re-search nil)}]]
           [:div.memes
            [react-infinite {:element-height 400
                             :infinite-load-begin-edge-offset 100
                             :use-window-as-scroll-container true
                             :on-infinite-load (fn []
                                                 (when-not (:graphql/loading? @meme-search)
                                                   (let [ {:keys [has-next-page end-cursor] :as r} (:search-memes (last @meme-search))]
                                                     (.log js/console "Scrolled to load more" has-next-page end-cursor)
                                                     (when has-next-page 
                                                       (re-search end-cursor)))))}
             (doall
              (for [{:keys [:reg-entry/address] :as meme} all-memes]
                ^{:key address}
                [challenge {:entry meme
                            :include-challenger-info? include-challenger-info?
                            :action-child action-child}]))]]])))))
