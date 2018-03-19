;; This ns is temporary here, will be made into district library later
(ns memefactory.shared.graphql-utils
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [cljsjs.graphql]
    [clojure.string :as string]
    [clojure.walk :as walk]
    [contextual.core :as contextual]
    [district.cljs-utils :as cljs-utils]
    [venia.core :as v]))

(def GraphQL (if (exists? js/GraphQL)
               js/GraphQL
               (js/require "graphql")))

(def visit (aget GraphQL "visit"))

(defn clj->graphql [k]
  (str
    (when (string/starts-with? (name k) "__")
      "__")
    (when (and (keyword? k)
               (namespace k))
      (str (string/replace (cs/->camelCase (namespace k)) "." "_") "_"))
    (let [k (name k)
          first-letter (first k)]
      (if (and (not= first-letter "_")
               (= first-letter (string/upper-case first-letter)))
        (cs/->PascalCase k)
        (cs/->camelCase k)))))


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
                                  (let [parsed-params (transform-keys graphql->clj (js->clj params))
                                        result (transform-resolvers (v parsed-params context schema))]
                                    result))
                                v)])
                           resolver)))
    (if (sequential? resolver)
      (clj->js (map transform-resolvers resolver))
      resolver)))

(defn- js-obj->clj [obj]
  (reduce (fn [acc key]
            (assoc acc (keyword key) (aget obj key)))
          {}
          (js->clj (js-keys obj))))

(defn- transform-result-vals [res]
  (walk/prewalk (fn [x]
                  (if (and (nil? (type x))
                           (seq (js-keys x)))
                    (js-obj->clj x)
                    (js->clj x)))
                (js->clj res :keywordize-keys true)))

(def transform-response-keys (partial transform-keys graphql->clj))

(defn transform-response [resp]
  (let [resp (transform-result-vals resp)]
    (update resp :data transform-response-keys)))


(defn create-field-node [name]
  {:kind "Field"
   :name {:kind "Name"
          :value name}})

(defn create-name-node [name]
  {:kind "Name"
   :value name})

(def id-field (create-field-node "id"))
(def typename-field (create-field-node "__typename"))

(defn selection-set-has-field? [SelectionSet Field]
  (.some (aget SelectionSet "selections")
         (fn [Selection]
           (and (= (aget Selection "kind") "Field")
                (= (aget Selection "name" "value")
                   (aget Field "name" "value"))))))


(defn add-field-to-query [query-ast field]
  (let [Field (clj->js (if (string? field)
                         (create-field-node field)
                         field))]
    (visit query-ast
           #js {:leave (fn [node key parent path ancestors]
                         (condp = (aget node "kind")
                           "SelectionSet"
                           (do
                             (when (and (not= (aget parent "kind") "OperationDefinition")
                                        (not (selection-set-has-field? node Field)))
                               (.push (aget node "selections") Field))
                             node)
                           js/undefined))})))

(defn- map-entry? [node]
  (and (vector? node)
       (= 2 (count node))
       (let [[key value] node]
         (and (keyword? key)
              (or (sequential? value)
                  (map? value))))))

(defn- entity? [node {:keys [:typename-field :id-field]}]
  (and (map? node)
       (get node typename-field)
       (get node id-field)))

(defn- get-ref [node {:keys [:typename-field :id-field :typename-transform-fn] :as opts
                      :or {typename-transform-fn identity}}]
  (when (entity? node opts)
    {:id (get node id-field)
     :type (typename-transform-fn (get node typename-field))
     :graphql/ref? true}))

(defn ref? [x]
  (boolean (:graphql/ref? x)))

(defn update-entity [state {:keys [:id :type]} item]
  (update-in state [type id] cljs-utils/merge-in item))

(defn get-entity [entities {:keys [:id :type]}]
  (get-in entities [type id]))

(defn normalize-aliases [data query-clj]
  (walk/postwalk (fn [node]
                   (try
                     (let [dectx-node (contextual/decontextualize node)
                           path (seq (remove number? (contextual/context node)))
                           {:keys [:name :args :alias?]} (when path (meta (get-in query-clj path)))]

                       (if (map? dectx-node)
                         (reduce
                           (fn [acc [key value]]
                             (let [{:keys [:name :args]} (meta (get-in query-clj (concat path [key])))]
                               (assoc-in acc (remove nil? [(or name key) args]) value)))
                           {}
                           dectx-node)
                         dectx-node))
                     (catch :default _
                       node)))
                 (contextual/contextualize data)))


(defn ref-entities [data opts]
  (let [entities (atom {})
        query-results (walk/postwalk (fn [node]
                                       (if (entity? node opts)
                                         (let [ref (get-ref node opts)]
                                           (swap! entities #(update-entity % ref node))
                                           ref)
                                         node))
                                     data)]
    {:entities @entities
     :query-results query-results}))


(defn normalize-response [data query-clj opts]
  (let [opts (merge {:id-field :id :typename-field :__typename} opts)
        query-clj (second (first query-clj))]

    (-> data
      (normalize-aliases query-clj)
      (ref-entities opts))))

(defn- into-map-or-nil [coll]
  (when-let [coll (seq (vec coll))]
    (into {} coll)))

(defn query-ast->query-clj [query-ast {:keys [:transform-name-fn :variables]
                                       :or {transform-name-fn identity}}]
  (let [t transform-name-fn
        m into-map-or-nil]
    (visit query-ast
           #js {:leave (fn [node key parent path ancestors]
                         (condp = (aget node "kind")
                           "Document" (m (aget node "definitions"))
                           "Name" (t (aget node "value"))
                           "Argument" {(aget node "name") (aget node "value")}
                           "OperationDefinition" {(or (aget node "name") :query)
                                                  (with-meta (aget node "selectionSet")
                                                             {:operation (keyword (aget node "operation"))})}
                           "SelectionSet" (m (aget node "selections"))
                           "Field" (let [selection (m (aget node "selectionSet"))
                                         meta (cond-> {:name (aget node "name")}
                                                (seq (vec (aget node "arguments")))
                                                (assoc :args (m (aget node "arguments")))

                                                (boolean (aget node "alias"))
                                                (assoc :alias? true))]
                                     {(or (aget node "alias")
                                          (aget node "name"))
                                      (with-meta selection meta)})
                           "IntValue" (js/parseInt (aget node "value"))
                           "FloatValue" (js/parseFloat (aget node "value"))
                           "NullValue" (aget node "value")
                           "StringValue" (aget node "value")
                           "BooleanValue" (boolean (aget node "value"))
                           "ListValue" (vec (aget node "values"))
                           "ObjectValue" (aget node "value")
                           "EnumValue" (aget node "value")
                           "Variable" (get variables (aget node "name"))
                           "FragmentDefinition" {(keyword :fragment (aget node "name")) (aget node "selectionSet")}
                           "FragmentSpread" {(keyword :fragment (aget node "name")) nil}
                           js/undefined))})))


(defn- query-resolver [args context document]
  (print.foo/look document)
  (if (= (aget document "fieldName") "searchMemes")
    {:total-count 9
     :has-next-page true
     :end-cursor "0x3f8cc2f7a4ed8c386b1339712366465c61073e8c",
     :items
     (constantly
       [{:id "0x3f8cc2f7a4ed8c386b1339712366465c61073e8c",
         :reg-entry/address "0x3f8cc2f7a4ed8c386b1339712366465c61073e8c",
         :reg-entry/challenge-period-end 1520371829,
         :reg-entry/created-on 1520371229,
         :reg-entry/creator "0xd98afc1dd919ee845d9f07dd6e9f31a2d00c0866"
         :reg-entry/deposit 1000,
         :reg-entry/version 1,
         :challenge/challenger "0xd98afc1dd919ee845d9f07dd6e9f31a2d00c0866",
         :challenge/claimed-reward-on 0,
         :challenge/commit-period-end 1520371349,
         :challenge/created-on 1520371229,
         :challenge/comment "Not a good meme",
         :challenge/reveal-period-end 1520371409,
         :challenge/reward-pool 500,
         :challenge/votes-against 0,
         :challenge/votes-for 9.99984e+26,
         :challenge/votes-total 9.99984e+26,
         :challenge/voting-token "0x8f450e039605ff48d32bcdab11dc111b98854577",
         :meme/image-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH",
         :meme/meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH",
         :meme/offering-duration 0,
         :meme/offering-start-price 929572691330084000,
         :meme/offering-supply 2341,
         :meme/offering-earnings 0
         :meme/offering-rank 3
         :meme/sold-for-total 0,
         :meme/title "HapplyHarambe"
         :meme/number 2
         :meme/token "0xe1aeac8013624b928051ccec98517b6a455da68d",
         :meme/total-supply 2342
         :meme/tags [{:tag/name "Harambe"} {:tag/name "Someno Tag"}]
         :challenge/vote (fn [{:keys [:vote/voter]}]
                           {:id voter
                            :vote/voter voter
                            :vote/amount (rand-int 3000)})}])}
    nil))


(defn create-field-resolver [{:keys [:transform-name-fn]
                              :or {transform-name-fn identity}}]
  (fn [{:keys [:query-results :entities]} args _ info]
    (let [name (aget info "fieldName")
          args (js->clj args)
          args (when (seq args) (transform-keys transform-name-fn args))
          value (get-in query-results (remove nil? [(transform-name-fn name) args]))]
      (cond
        (map? value)
        {:query-results (if (ref? value) (get-entity entities value) value)
         :entities entities}

        (sequential? value)
        (let [array (js/Array.)]
          (doseq [item value]
            (let [item (if (ref? item) (get-entity entities item) item)]
              (.push array {:query-results item :entities entities})))
          array)

        :else value))))

(defn merge-queries [query-asts]
  (let [query-asts (js->clj query-asts :keywordize-keys true)]
    (clj->js
      (reduce (fn [acc query]
                (let [variables (get-in query [:definitions 0 :variableDefinitions])
                      selections (->> (get-in query [:definitions 0 :selectionSet :selections])
                                   (map (fn [sel]
                                          (if-not (:alias sel)
                                            (assoc sel :alias (create-name-node
                                                                (str (:value (:name sel)) (rand-int 99999))))
                                            sel))))]
                  (-> acc
                    (update-in [:definitions 0 :selectionSet :selections] concat selections)
                    (update-in [:definitions 0 :variableDefinitions] concat variables))))
              (first query-asts)
              (rest query-asts)))))