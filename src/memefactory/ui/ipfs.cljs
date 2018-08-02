(ns memefactory.ui.ipfs
  (:require [district0x.re-frame.ipfs-fx :as ipfs-fx]
            [mount.core :as mount :refer [defstate]]
            [re-frame.core :as re-frame]))

(defstate district-ui-ipfs
  :start (re-frame/dispatch-sync [:ipfs/init (:ipfs (mount/args))])
  :stop ::stopped)
