(ns memefactory.server.emailer.templates
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [clojure.string :as str]
   [garden.core :as garden]))

(defn- format-token-amount [amount]
  (-> amount (web3/from-wei :ether) bn/number (.toFixed 3)))

(def link-style
  (garden/style
   {:color "#47608e"}))

(defn format-link [url text]
  (str "<a href=\"" url "\" style=\"" link-style "\">" text "</a>"))

(defn challenge-created-email-body [{:keys [:meme/title :meme-url :time-remaining]}]
  (let [link (format-link meme-url title)]
    (str "Your meme " link " was just challenged. Hurry, you have "
         (str/lower-case time-remaining) " to visit the website and vote to keep your meme in the Dank Registry!")))

(defn meme-auction-bought-email-body [{:keys [:price :meme/title :meme-url :buyer-address :buyer-url]}]
  (let [buyer-link (format-link buyer-url buyer-address)
        link (format-link meme-url title)
        price (format-token-amount price)]
    (str "Your offering of " link " was sold for " price " ETH by " buyer-link)))

(defn vote-reward-claimed-email-body [{:keys [:amount :meme/title :meme-url :vote/option]}]
  (let [link (format-link meme-url title)
        amount (format-token-amount amount)]
    (str "You received " amount " DANK as a reward for voting " option " for " link)))

(defn challenge-reward-claimed-email-body [{:keys [:amount :meme/title :meme-url]}]
  (let [link (format-link meme-url title)
        amount (format-token-amount amount)]
    (str "You received " amount " DANK as a reward for challenging " link ".")))
