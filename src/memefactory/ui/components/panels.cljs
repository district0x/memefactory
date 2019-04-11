(ns memefactory.ui.components.panels
  (:require [cljs-web3.core :as web3]
            [clojure.string :as str]
            [district.format :as format]
            [district.time :as time]
            [district.ui.component.form.input :as inputs]
            [district.ui.now.subs :as now-subs]
            [district.ui.router.events :as router-events]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [memefactory.shared.utils :as shared-utils]
            [memefactory.ui.components.infinite-scroll :refer [infinite-scroll]]
            [memefactory.ui.components.tiles :as tiles]
            [memefactory.ui.contract.meme-auction :as meme-auction]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [taoensso.timbre :as log]))

(defmulti panel (fn [tab & opts] tab))

(defn no-items-found [extra-classes]
  [:div.no-items-found {:class (str/join " " extra-classes)}
   "No items found"])

(defmethod panel :selling [_ {:keys [:state :loading-first? :loading-more? :has-more? :re-search]}]
  (log/debug _ {:c (count state)})
  ;;:div.selling-panel
  (if (and (empty? state)
           (not loading-first?))
    [no-items-found]
    [infinite-scroll {:class "tiles"
                      :loading? loading-more?
                      :has-more? has-more?
                      :load-fn re-search}
     (when-not loading-first?
       (doall
        (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
               ^{:key address} [tiles/auction-tile {} meme-auction])
             state)))]))
