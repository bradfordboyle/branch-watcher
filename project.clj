(defproject branch-watcher "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.9.0"]
                 [cheshire "5.8.0"]
                 [environ "1.1.0"]]
  :main ^:skip-aot branch-watcher.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
