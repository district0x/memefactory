(ns memefactory.server.ranks-cache
  (:require [district.server.config :refer [config]]
            [district.server.db :as db]
            [mount.core :as mount :refer [defstate]]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [merge-where merge-order-by merge-left-join defhelper]]
            [cljs.cache :as cache]
            [memefactory.server.db]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Our users ranks cache is a TTL cache by ranks type [:creator-rank :challenger-rank :voter-rank :curator-rank] ;;
;; stored into an atom                                                                                           ;;
;;                                                                                                               ;;
;; Cache example :                                                                                               ;;
;; {:creator-rank    {"USER_ADDR1" 13                                                                            ;;
;;                    "USER_ADDR2" 23}                                                                           ;;
;;  :challenger-rank {"USER_ADDR1" 23                                                                            ;;
;;                    "USER_ADDR2" 43}                                                                           ;;
;;  :voter-rank      {"USER_ADDR1" 12                                                                            ;;
;;                    "USER_ADDR2" 23}                                                                           ;;
;;  :curatorr-rank   {"USER_ADDR1" 23                                                                            ;;
;;                    "USER_ADDR2" 23}}                                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(declare start)
(declare stop)

(defstate ranks-cache
  :start (start (merge (:ranks-cache @config)
                       (:ranks-cache (mount/args))))
  :stop (stop))


(defn start [opts]
  (atom (cache/ttl-cache-factory {nil nil} ;; Do we want to start with cold cache?
                                           ;; fails with empty map for some reason
                                 ;; something long here maybe an hour?
                                 :ttl 10000))) 

(defn stop [])

(defn get-rank
  "Rank can be any of [:creator-rank :challenger-rank :voter-rank :curator-rank]
  recalculate-fn is used to recalculate the value for the cache entry in
  case of a cache miss."
  [rank recalculate-fn]
  (let [entry (if (cache/has? @@ranks-cache rank)
                (swap! @ranks-cache cache/hit rank)
                (swap! @ranks-cache cache/miss rank (recalculate-fn)))] 
    (-> entry
        .-cache
        (get rank))))

