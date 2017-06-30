(defproject org.clojars.cognesence/ops-search "1.0.0"
  :description "A simple, partially optimised implementation of a breadth-first search mechanism for applying simple STRIPS-style operators."
  :url "https://github.com/cognesence/ops-search"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
               [org.clojars.cognesence/matcher "1.0.1"]]
  :repl-options {:init [(use 'org.clojars.cognesence.matcher.core)
                        (use 'org.clojars.cognesence.ops-search.core)]})
