(ns branch-watcher.core
  (:gen-class)
  (:require [environ.core :refer [env]]
            [clj-http.client :as http]
            [clojure.java.io :as io])

  (:use [clojure.pprint :only (print-table)]))

(def token
  (env :github-token))

(defn sort-branch-info
  [branch-info]
  (->> branch-info
       (group-by :author)
       (sort-by #(count (second %)) >)
       (mapcat (fn [[name refs]] (sort-by :date refs)))))

(def url "https://api.github.com/graphql")

(def branch-query (slurp (io/resource "query.graphql")))

(defn make-request
  [query variables]
  {:method :post
   :url url
   :form-params {:query query :variables variables}
   :content-type :json
   :oauth-token token
   :as :json})

(defn update-request
  "Given a clj-http request and a 'cursor', merge the cursor into the request"
  [req cursor]
  (assoc-in req [:form-params :variables :cursor] cursor))

(defn page-info
  [resp]
  (get-in resp [:data :repository :refs :pageInfo]))

(defn has-next?
  "Given a clj-http resp, check if there are more results"
  [resp]
  (-> resp
      page-info
      :hasNextPage))

(defn extract-nodes
  [resp]
  (-> resp :data :repository :refs :edges))

(defn branch-info
  "Given a collection of branch ref edges, return the name, committed date, and author of the branch"
  [edge]
  (let [node (:node edge)]
    {:name (-> node :name)
     :date (-> node :target :committedDate)
     :author (-> node :target :author :name)}))

(defn run-query
  [query variables]
  ; look at using `letfn` here instead
  (let [req (make-request query variables)
        exec-request-one (fn exec-request-one [req]
                           (:body (http/request req)))
        exec-request (fn exec-request [req]
                       (let [resp (exec-request-one req)]
                         (if (has-next? resp)
                           (let [new-req (update-request req (-> resp page-info :endCursor))]
                             (lazy-cat (extract-nodes resp) (exec-request new-req)))
                           (extract-nodes resp))))]
    (exec-request req)))

(defn branches
  ([owner repo-name] (branches owner repo-name 10))
  ([owner repo-name fetch-size]
   (->> {:owner owner :name repo-name :fetchSize fetch-size}
        (run-query branch-query)
        (map branch-info))))

(defn display-branches
  ([owner repo-name] (display-branches owner repo-name 100))
  ([owner repo-name fetch-size]
   (->> (branches owner repo-name 50)
        (sort-by :date)
        (print-table [:author :name :date]))))

(defn -main
  "Print a table of branches, sorted by author with highest number of outstanding branches and then date"
  [& args]
  (let [org (first args)
        repo (second args)]
    (display-branches org repo)))
