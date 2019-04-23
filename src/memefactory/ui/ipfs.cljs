(ns memefactory.ui.ipfs
  (:require
    [district0x.re-frame.ipfs-fx :as ipfs-fx]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :as re-frame]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-fx
 ::init-ipfs
 [interceptors]
 (fn [_ [config]]
   {:ipfs/init config}))

(defstate district-ui-ipfs
  :start (re-frame/dispatch-sync [::init-ipfs (:ipfs (mount/args))])
  :stop ::stopped)
