(ns memefactory.ui.components.challenge-list
  (:require
   [cljs-time.core :as t]
   [cljs-time.extend]
   [district.format :as format]
   [district.time :as time]
   [district.ui.component.form.input :refer [select-input with-label]]
   [district.ui.graphql.subs :as gql]
   [goog.string :as gstring]
   [district.ui.now.subs]
   [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.components.tiles :refer [meme-image]]
   [memefactory.ui.utils :as mf-utils]
   [memefactory.ui.utils :as utils]
   [memefactory.ui.components.spinner :as spinner]
   [district.graphql-utils :as gql-utils]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [district.ui.router.events :as router-events]))

(def page-size 3)

(defn build-challenge-query [{:keys [data after include-challenger-info? query-params active-account]}]
  (let [{:keys [:order-by :order-dir]} data]
    [:search-memes
     (cond-> (merge {:first page-size} query-params)
       after (assoc :after after)
       order-by (assoc :order-by (keyword "memes.order-by" order-by))
       order-dir (assoc :order-dir (keyword order-dir)))
     [:total-count
      :end-cursor
      :has-next-page
      [:items (cond-> [:reg-entry/address
                       :reg-entry/created-on
                       :reg-entry/challenge-period-end
                       :challenge/commit-period-end
                       :challenge/reveal-period-end
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
                                                                       :user/challenger-rank
                                                                       :user/total-created-challenges
                                                                       :user/total-created-challenges-success]])
                active-account (into [[:challenge/vote {:vote/voter active-account}
                                       [:vote/secret-hash
                                        :vote/revealed-on
                                        :vote/option
                                        :vote/amount]]
                                      [:challenge/vote-winning-vote-option {:vote/voter active-account}]
                                      [:challenge/all-rewards {:user/address active-account}
                                       [:challenge/reward-amount
                                        :vote/reward-amount]]]))]]]))

(defn creator-info [user]
  [:ol {:class :creator}
   [:li "Rank: " [:span (gstring/format "#%d" (or (:user/creator-rank user) 0))]]
   [:li "Success Rate: " [:span (let [tcmw (or (:user/total-created-memes-whitelisted user) 0)
                                      tcm (or (:user/total-created-memes user) 0)]
                                  (gstring/format "%d/%d (%d%%)"
                                                  tcmw
                                                  tcm
                                                  (if (pos? tcm) (/ (* 100 tcmw) tcm) 0)))]]
   [:li "Address: " [:span.address {:on-click #(dispatch [::router-events/navigate :route.memefolio/index
                                                          {:address (:user/address user)}
                                                          {:tab :created}])
                                    :class "creator"}
                     (-> user :user/address)]]])

(defn challenger-info [user]
  [:ol {:class :challenger}
   [:li "Rank: " [:span (gstring/format "#%d" (or (:user/challenger-rank user) 0))]]
   [:li "Success Rate: " [:span (let [tccs (or (:user/total-created-challenges-success user) 0)
                                      tcc (or (:user/total-created-challenges user) 0)]
                                  (gstring/format "%d/%d (%d%%)"
                                                  tccs
                                                  tcc
                                                  (if (pos? tcc) (/ (* 100 tccs) tcc) 0)))]]
   [:li "Address: " [:span.address (-> user :user/address)]]])

(defn current-period-ends [label end-date]

  (let [{:keys [days hours minutes seconds] :as time-remaining} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                                     (utils/gql-date->date end-date))]
    (if (= 0 days hours minutes seconds)
      [:li (str label " period ended.")]

      [:li (str label " period ends in: ")
       [:span (-> time-remaining
                  format/format-time-units)]])))

(defn challenge [{:keys [:entry :include-challenger-info? :action-child]}]
  (let [{:keys [:reg-entry/address :reg-entry/created-on :reg-entry/challenge-period-end
                :meme/total-supply :meme/image-hash :reg-entry/creator :meme/title
                :meme/tags :challenge/challenger :challenge/comment :reg-entry/status :challenge/commit-period-end :challenge/reveal-period-end]} entry]

    (let [status (gql-utils/gql-name->kw status)
          [period-label period-end] (case status
                                      :reg-entry.status/challenge-period ["Challenge" challenge-period-end]
                                      :reg-entry.status/commit-period    ["Voting" commit-period-end]
                                      :reg-entry.status/reveal-period    ["Reveal" reveal-period-end]
                                      nil)
          {:keys [days hours minutes seconds] :as time-remaining} (time/time-remaining @(subscribe [:district.ui.now.subs/now])
                                                                                       (utils/gql-date->date period-end))]
      [:div.challenge
       (cond-> [:div.info
                [:h2 {:on-click #(dispatch [::router-events/navigate :route.meme-detail/index
                                            {:address address}
                                            nil])}
                 title]

                [:ol.meme
                 [:li "Created: " [:span (let [formated-time (-> (time/time-remaining (t/date-time (utils/gql-date->date created-on)) (t/now))

                                                                 (dissoc :seconds)
                                                                 format/format-time-units)]
                                           (if-not (empty? formated-time)
                                             (str formated-time " ago")
                                             "less than a minute ago"))]]


                 (if period-end
                   (if (= 0 days hours minutes seconds)
                     [:li (str period-label " period ended.")]

                     [:li (str period-label " period ends in: ")
                      [:span (-> time-remaining
                                 format/format-time-units)]])
                   [:li ""])
                 [:li "Issued: " [:span total-supply]]]
                [:h3 "Creator"]
                [creator-info creator]]
         include-challenger-info? (into [[:h3.challenger "Challenger"]
                                         [challenger-info challenger]])
         true                     (into [[:span.challenge-comment (when-not (empty? comment)
                                                                    (str "\""comment "\""))]
                                         [:ol.tags
                                          (for [{:keys [:tag/name]} tags]
                                            [:li.tag {:key name}
                                             name])]]))

       [:div.meme-tile
        [tiles/meme-image image-hash {:rejected? (= status :reg-entry.status/blacklisted)}]]
       [:div.action
        (if (and (= 0 days hours minutes seconds)
                 (not (#{:reg-entry.status/whitelisted :reg-entry.status/blacklisted} status)))
          [:span.period-ended (str period-label " period ended.")]
          [action-child entry])]])))

(defn challenge-list [{:keys [include-challenger-info? query-params action-child active-account key sort-options]}]
  (let [form-data (r/atom {:order-by (-> sort-options first :key)
                           :order-dir (-> sort-options first :dir (or :desc))})]
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
                           (remove #(nil? (:reg-entry/address %))))]

        (log/debug "All Rendering here" all-memes ::challenge-list)
        (log/debug "All memes" {:memes (map :reg-entry/address all-memes)} ::challenge-list)

        [:div.challenges.panel
         [:div.controls
          [select-input {:form-data form-data
                         :class :white-select
                         :id :order-by
                         :options sort-options
                         :on-change #(re-search nil)}]]
         [:div.scroll-area
          [:div.memes
           (if (and (empty? all-memes)
                    (not (:graphql/loading? (last @meme-search))))
             [:div.challenge.no-items "No items found."]
             (doall
              (for [{:keys [:reg-entry/address] :as meme} all-memes]
                ^{:key address}
                [challenge {:entry meme
                            :include-challenger-info? include-challenger-info?
                            :action-child action-child}])))
           (when (:graphql/loading? (last @meme-search))
             [:div.challenge
              [:div.spinner-container [spinner/spin]]])]
          [infinite-scroll {:load-fn (fn []
                                       (when-not (:graphql/loading? @meme-search)
                                         (let [ {:keys [has-next-page end-cursor] :as r} (:search-memes (last @meme-search))]

                                           (log/debug "Scrolled to load more" {:has-next-page has-next-page :end-cursor end-cursor} ::challenge-list)

                                           (when has-next-page
                                             (re-search end-cursor)))))}]]]))))
