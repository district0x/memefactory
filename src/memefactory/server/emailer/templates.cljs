(ns memefactory.server.emailer.templates
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [garden.core :as garden]
   [hiccups.runtime :as hiccupsrt]))

(defn- format-dank-token-amount [amount]
  (-> amount (web3/from-wei :ether) bn/number (.toFixed 3)))

(def link-style
  (garden/style
   {:color "#47608e"}))

(defn challenge-created-email-body [{:keys [:meme/title :meme-url :time-remaining]}]
  (html
   [:span "Your meme "
    [:a {:href meme-url} title]
    (str " was just challenged. Hurry, you have "
         time-remaining
         " to visit the website and vote to keep your meme in the Dank Registry!")]))

(defn meme-auction-bought-email-body [{:keys [:meme/title :meme-url]}]
  (html [:span "Your auction " [:a {:href meme-url :style link-style} title] " has been bought."]))

(defn vote-reward-claimed-email-body [{:keys [:amount :meme/title :meme-url :vote/option]}]
  (let [amount (format-dank-token-amount amount)]
    (html [:span "You received " amount " DANK as a reward for voting " option " for " [:a {:href meme-url :style link-style} title] "."])))

(defn challenge-reward-claimed-email-body [{:keys [:amount :meme/title :meme-url]}]
  (let [amount (format-dank-token-amount amount)]
    (html [:span "You received " amount " DANK as a reward for challenging " [:a {:href meme-url :style link-style} title] "."])))
