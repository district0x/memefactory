(ns memefactory.ui.subs
  (:require
   [district.ui.mobile.subs :as mobile-subs]
   [memefactory.ui.mobile :as ui.mobile]
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::menu-drawer-open?
 (fn [db _]
   (get-in db [:menu-drawer :open?])))

(re-frame/reg-sub
 ::active-page
 (fn [db _]
   (::active-page db)))

(re-frame/reg-sub
 ::settings
 (fn [db [_ account]]
   (get-in db [:settings account])))

(re-frame/reg-sub
 ::votes
 (fn [db [_ account]]
   (get-in db [:votes account])))

(re-frame/reg-sub
 ::dank-faucet-spinner
 (fn [db _]
   (:memefactory.ui.get-dank.events/spinner db)))

(re-frame/reg-sub
 ::dank-faucet-succeeded
 (fn [db _]
   (:memefactory.ui.get-dank.events/succeeded db)))

(re-frame/reg-sub
 ::mobile-coinbase-appstore-link
 :<- [::mobile-subs/android?]
 :<- [::mobile-subs/ios?]
 (fn [[android? ios?]]
   (cond
     android? (:android-mobile-link ui.mobile/coinbase-appstore-links)
     ios? (:ios-mobile-link ui.mobile/coinbase-appstore-links)
     :else (:main-mobile-link ui.mobile/coinbase-appstore-links))))

(re-frame/reg-sub
 ::nsfw-switch
 (fn [db _]
   (:nsfw-switch db)))
