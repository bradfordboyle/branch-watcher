(ns branch-watcher.core
  (:gen-class)
  (:require [environ.core :refer [env]]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as str])

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

(def repo-query (slurp (io/resource "repos.graphql")))

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
  "Given a clj-http response, extract the cursor information"
  [resp path]
  (-> resp
      :data
      (get-in path)
      :pageInfo))

(defn has-next?
  [resp path]
  (-> resp
      (page-info path)
      :hasNextPage))

(defn extract-nodes
  [resp path]
  (-> resp
      :data
      (get-in path)
      :nodes))

(defn branch-info
  "Given a collection of branch ref nodes, return the name, committed date, and author of the branch"
  [node]
  {:name (-> node :name)
   :date (-> node :target :committedDate)
   :author (-> node :target :author :name)})

(defn run-query
  [query path variables]
  ; look at using `letfn` here instead
  (let [req (make-request query variables)
        exec-request-one (fn exec-request-one [req]
                           (:body (http/request req)))
        exec-request (fn exec-request [req]
                       (let [resp (exec-request-one req)
                             nodes (extract-nodes resp path)]
                         (do
                           (if (has-next? resp path)
                             (let [new-req (update-request req (:endCursor  (page-info resp path)))]
                               (lazy-cat (extract-nodes resp path) (exec-request new-req)))
                             (extract-nodes resp path)))))]
    (exec-request req)))

(defn branches
  ([owner repo-name] (branches owner repo-name 100))
  ([owner repo-name fetch-size]
   (->> {:owner owner :name repo-name :fetchSize fetch-size}
        (run-query branch-query [:repository :refs])
        (map branch-info))))

(defn repos
  ([login] (repos login 100))
  ([login fetch-size]
   (->> {:login login :fetchSize fetch-size}
        (run-query repo-query [:organization :repositories]))))

(defn repo-name-starts-with?
  [repo]
  (-> repo
      :name
      (str/starts-with? "gp")))

(defn repo-topics
  [repo]
  (get-in repo [:repositoryTopics :nodes]))

(defn has-topics?
  [repo]
  (-> repo
      repo-topics
      empty?
      not))

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
