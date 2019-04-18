(ns memefactory.ui.components.panels
  (:require
   [cljs-web3.core :as web3]
   [clojure.string :as str]
   [district.format :as format]
   [district.time :as time]
   [district.ui.component.form.input :as inputs]
   [district.ui.now.subs :as now-subs]
   [district.ui.router.events :as router-events]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [memefactory.shared.utils :as shared-utils]
   [memefactory.ui.components.infinite-scroll :as infinite-scroll]
   [memefactory.ui.components.search :as search]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.components.tiles :as tiles]
   [memefactory.ui.contract.meme-auction :as meme-auction]
   [taoensso.timbre :as log]
   ))

(defmulti panel (fn [tab & opts] tab))

(defn no-items-found [extra-classes]
  [:div.no-items-found {:class (str/join " " extra-classes)}
   "No items found"])

(defmethod panel :selling [_ {:keys [:state :loading-first? :loading-more? :has-more? :re-search
                                     :form-data ]}]
  (log/debug _ {:c (count state)})
  [:div.selling-panel
   [inputs/radio-group {:id :option-filters
                        :form-data form-data
                        :options search/auctions-option-filters}]
   (if (and (empty? state)
            (not loading-first?))
     [no-items-found]
     [infinite-scroll/infinite-scroll {:class "tiles"
                                       :loading? loading-more?
                                       :has-more? has-more?
                                       :loading-spinner-delegate (fn []
                                                                   [:div.spinner-container [spinner/spin]])
                                       :load-fn re-search}
      (when-not loading-first?
        (doall
         (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
                ^{:key address}
                [tiles/auction-tile {:show-cards-left? (contains? #{:only-cheapest :only-lowest-number} (:option-filters @form-data))}
                 meme-auction])
              state)))])])
