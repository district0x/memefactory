(ns memefactory.ui.components.panels
  (:require [district.ui.router.events :as router-events]
            [district.ui.now.subs :as now-subs]
            [memefactory.shared.utils :as shared-utils]
            [memefactory.ui.components.tiles :as tiles]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [district.time :as time]
            [district.format :as format]
            [cljs-web3.core :as web3]
            [district.ui.web3-tx-id.subs :as tx-id-subs]
            [district.ui.component.form.input :as inputs]
            [memefactory.ui.contract.meme-auction :as meme-auction]))

(defmulti panel (fn [tab & opts] tab))

(defn selling-back-tile [address number title]
  (let [tx-id (str address "cancel" number)
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {:meme-auction/cancel tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {:meme-auction/cancel tx-id}])]
   [:div.selling-tile-back.meme-card.back
    [:div.sell
     [:div.top
      [:b (str "#" number)]
      [:img {:src "/assets/icons/mememouth.png"}]]
     [:div.bottom
      [inputs/pending-button {:pending? @tx-pending?
                              :pending-text "Cancelling..."
                              :disabled (or @tx-pending? @tx-success?)
                              :on-click (fn [e]
                                          (.stopPropagation e)
                                          (dispatch [::meme-auction/cancel {:send-tx/id tx-id
                                                                            :meme-auction/address address
                                                                            :meme/title title}]))}
       (if @tx-success?
         "Cancelled"
         "Cancel Sell")]]]]))

(defmethod panel :selling [_ state]
  [:div.selling-panel
   [:div.tiles
    (if (empty? state)
      [:div.no-items-found "No items found."]
      (doall
       (map (fn [{:keys [:meme-auction/address :meme-auction/meme-token] :as meme-auction}]
              [tiles/auction-tile {} meme-auction])
            state)))]])
