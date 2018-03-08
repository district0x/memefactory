;; This ns is temporary here, will be made into district library later
(ns memefactory.shared.graphql-utils
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [clojure.string :as string]
    [venia.core :as v]))

(defn clj->graphql [k]
  (str
    (when (string/starts-with? (name k) "__")
      "__")
    (when (and (keyword? k)
               (namespace k))
      (str (string/replace (cs/->camelCase (namespace k)) "." "_") "_"))
    (cs/->camelCase (name k))))


(defn graphql->clj [k]
  (let [k (name k)]
    (if (string/starts-with? k "__")
      (keyword k)
      (let [parts (string/split k "_")
            parts (if (< 2 (count parts))
                    [(string/join "." (butlast parts)) (last parts)]
                    parts)]
        (apply keyword (map cs/->kebab-case parts))))))


(defn transform-resolvers [resolver]
  (if (map? resolver)
    (clj->js (into {} (map (fn [[k v]]
                             [(clj->graphql k)
                              (if (fn? v)
                                (fn [params context schema]
                                  (let [params (transform-keys graphql->clj (js->clj params))]
                                    (transform-resolvers (v params context schema))))
                                v)])
                           resolver)))
    (if (sequential? resolver)
      (clj->js (map transform-resolvers resolver))
      resolver)))

(defn transform-response [resp]
  (let [resp (js->clj resp :keywordize-keys true)]
    (update resp :data (partial transform-keys graphql->clj))))

