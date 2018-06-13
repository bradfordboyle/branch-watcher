(ns branch-watcher.core
  (:gen-class)
  (:require [environ.core :refer [env]])
  (:require [tentacles.repos :as repos])
  (:use [clojure.pprint :only (print-table)]))

(def token
  (env :github-token))

(defn commit-date
  [commit]
  (get-in commit [:commit :author :date]))

(defn commit-author
  [commit]
  (get-in commit [:commit :author :name]))

(defn get-all-branches
  [user repo]
  (map (fn [branch]
         (let [branch-commit-sha (get-in branch [:commit :sha])
               branch-name (:name branch)
               commit (repos/specific-commit user repo branch-commit-sha {:oauth-token token})]

           {:name branch-name :date (commit-date commit) :author (commit-author commit)}))
       (repos/branches user repo {:oauth-token token :all-pages true})))

(defn sort-branch-info
  [branch-info]
  (let [by-author (group-by :author branch-info)
        by-count (sort-by #(count (second %)) > by-author)]
    (reduce (fn [acc [k v]] (into acc (sort-by :date v))) [] by-count)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [org (first args)
        repo (second args)
        data ((comp sort-branch-info get-all-branches) org repo)]
    (print-table [:author :name :date] data)))
