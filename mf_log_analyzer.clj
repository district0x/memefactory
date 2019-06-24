#!/bin/sh
#_(
   exec clojure "$0" "$@"
   )

(require '[clojure.string :as str])

(def resync-start-delim "WARN Started")
(def event-line-identifier ":event :")
(def event-capture-regex #".+:event :([a-zA-Z]+)")

(defn analyze-restart-groups
  "Given log lines return a list of group lines"
  [lines]
  (->> lines
       (partition-by #(str/starts-with? % resync-start-delim))
       (remove #(= % (list resync-start-delim)))))

(defn analyze-group
  "Given a group lines return a map with event frequencies"
  [lines]
  (->> lines
       (filter #(.contains % event-line-identifier))
       (map #(second (re-find event-capture-regex %)))
       frequencies))

(defn warn-group-event
  "Given a groups pair, returns the second group evs warned if needed"
  [[g1 g2]]
  (map (fn [[ev cnt]]
         (with-meta [ev cnt] {:warning? (< cnt (get g1 ev 0))}))
       g2))

(defn add-warnings
  "Given a list of group maps, warns them by comparing with the previous if needed"
  [[first-group :as groups]]
  (->> (partition 2 1 groups)
       (map warn-group-event)
       (into [first-group])))

(if-let [log-file-name (first *command-line-args*)]
  (let [analyzed-groups (->> log-file-name
                             slurp
                             str/split-lines
                             analyze-restart-groups
                             (map analyze-group)
                             add-warnings)]
    (doseq [g analyzed-groups]
      (println "\nRE SYNC\n")
      (doseq [[ev cnt :as gline] g]
        (println ev ":" cnt
                 (if (-> gline meta :warning?)
                   "<---- WARNING value shouldn't be lower than previous grounp"
                   "ok")))))

  (println "\n Usage : \n\n"
           "  mf_log_analyzer.cl mf.log \n\n"
           "To obtain production log run : \n\n"
           "  ssh ubuntu@district0x.io 'docker logs prod_memefactory-server' > mf.log\n"))
