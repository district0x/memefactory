(ns memefactory.ui.components.share-buttons
  (:require
    [reagent.core :as r]
    [cljsjs.react-share]))

(def twitter-share-button (r/adapt-react-class js/ReactShare.TwitterShareButton))
(def twitter-icon (r/adapt-react-class js/ReactShare.TwitterIcon))
(def facebook-share-button (r/adapt-react-class js/ReactShare.FacebookShareButton))
(def facebook-icon (r/adapt-react-class js/ReactShare.FacebookIcon))
(def telegram-share-button (r/adapt-react-class js/ReactShare.TelegramShareButton))
(def telegram-icon (r/adapt-react-class js/ReactShare.TelegramIcon))
(def reddit-share-button (r/adapt-react-class js/ReactShare.RedditShareButton))
(def reddit-icon (r/adapt-react-class js/ReactShare.RedditIcon))

(defn share-buttons [url]
  [:div.share-buttons
   [twitter-share-button {:url url} [twitter-icon {:size 32 :round true}]]
   [facebook-share-button {:url url} [facebook-icon {:size 32 :round true}]]
   [telegram-share-button {:url url} [telegram-icon {:size 32 :round true}]]
   [reddit-share-button {:url url} [reddit-icon {:size 32 :round true}]]
   ]
  )
