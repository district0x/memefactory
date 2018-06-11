(ns memefactory.ui.marketplace.events
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::buy-meme-auction
 (fn [cofx [_ auciton-address]]
   ))
