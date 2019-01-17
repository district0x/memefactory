(ns memefactory.ui.subs
  (:require
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
