(ns memefactory.ui.spec
  (:require [cljs.spec.alpha :as s]))

(s/def ::str string?)
(s/def ::not-empty? (complement empty?))
(s/def ::int integer?)
(s/def ::pos pos?)

(s/def ::pos-int (s/and ::int ::pos))
(s/def ::challenge-comment (s/and ::str ::not-empty?))

(defn check [type data]
  (s/valid? type data))
