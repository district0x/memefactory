(ns memefactory.ui.components.panels
  (:require [district.ui.router.events :as router-events]
            [cljs-web3.core :as web3]
            [district.format :as format]
            [district.time :as time]
            [district.ui.component.form.input :as inputs]
            [district.ui.now.subs :as now-subs]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [memefactory.shared.utils :as shared-utils]
            [memefactory.ui.components.tiles :as tiles]
            [memefactory.ui.contract.meme-auction :as meme-auction]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [memefactory.ui.components.search :refer [auctions-option-filters]]))

(defmulti panel (fn [tab & opts] tab))

(defn no-items-found [extra-classes]
  [:div.no-items-found {:class (str/join " " extra-classes)}
   "No items found"])

(defmethod panel :selling [_ state {:keys [loading-first? loading-last? form-data]}]

  [:div.selling-panel
   [inputs/radio-group {:id :option-filters
                        :form-data form-data
                        :options auctions-option-filters}]
   [:div.tiles
    (if (and (empty? state)
             (not loading-last?))
      [no-items-found]
      (when-not loading-first?
        (doall
         (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
                ^{:key address} [tiles/auction-tile {:show-cards-left? true} meme-auction])
              state))))]])
