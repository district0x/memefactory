(ns memefactory.server.graphql-resolvers
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]))

(enable-console-print!)

(def GraphQLEnumType (aget (js/require "graphql") "GraphQLEnumType"))

(defn transform-resolvers [resolver]
  (if (map? resolver)
    (clj->js (into {} (map (fn [[k v]]
                             [(cs/->camelCase (name k))
                              (if (fn? v)
                                (fn [params context schema]
                                  (let [params (transform-keys cs/->kebab-case-keyword (js->clj params))]
                                    (transform-resolvers (v params context schema))))
                                v)])
                           resolver)))
    (if (sequential? resolver)
      (clj->js (map transform-resolvers resolver))
      resolver)))

(def graphql-resolvers
  #_{:searchMemes (fn [params]
                    (clj->js [{:address "0x123"
                               :vote (fn []
                                       (clj->js {:amount 2
                                                 :voteOption "NoVote"}))}]))}

  (transform-resolvers
    {:search-memes (fn [params _ info]
                     #_(print.foo/look
                         (:selections (:selectionSet (first (:fieldNodes (js->clj info :keywordize-keys true))))))
                     [{:reg-entry/address "0x123"
                       :reg-entry/created-on "123"
                       :vote (fn [{:keys [:voter]} _ info]
                               #_(print.foo/look (map
                                                   (comp :value :name)
                                                   (:selections (:selectionSet (first (:fieldNodes (js->clj info :keywordize-keys true)))))))
                               {:amount (fn []
                                          (println "keket")
                                          voter)
                                :vote/option "NoVote"})}])}))
