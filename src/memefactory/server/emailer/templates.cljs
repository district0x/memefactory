(ns memefactory.server.emailer.templates
  (:require [goog.string :as gstring]))

(defn challenge-created-email-body [{:keys [:meme/title :meme-url :reveal-period-end]}]
  (gstring/format "Your meme <a href=\"%s\">%s</a> was just challenged. Hurry, you have time until %s to visit the website and vote to keep your meme in the Dank Registry!"
                  meme-url
                  title
                  reveal-period-end))

(defn meme-auction-bought-email-body [{:keys [:meme/title :meme-url]}]
  (gstring/format "Your <a href=\"%s\">%s meme auction</a> has been bought"
                  meme-url
                  title))

(defn vote-reward-claimed-email-body [{:keys [:amount :meme/title :meme-url]}]
  (gstring/format "You received %d as a reward for voting on <a href=\"%s\">%s</a>"
                  amount
                  meme-url
                  title))

(defn challenge-reward-claimed-email-body [{:keys [:amount :meme/title :meme-url]}]
  (gstring/format "You received %d as a reward for challenging <a href=\"%s\">%s</a>"
                  amount
                  meme-url
                  title))
