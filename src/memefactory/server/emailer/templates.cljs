(ns memefactory.server.emailer.templates
  (:require [goog.string :as gstring]))

(defn challenge-created-email-body [{:keys [:meme/title]}]
  (let [link (str "http://")]
    (gstring/format "Your meme <a href=\"%s\">%s</a> has been challenged."
                    link
                    title)))

(defn meme-auction-bought-email-body [{:keys []}]
  (let [link (str "https://")]
    (gstring/format "Auction bought %s"
                    link)))

(defn vote-reward-claimed-email-body [{:keys [:amount :title]}]
  (gstring/format "You received %d as a reward for voting on %s"
                  amount
                  title))

(defn challenge-reward-claimed-email-body [{:keys [:amount :title]}]
  (gstring/format "You received %d as a reward for challenging %s"
                  amount
                  title))
