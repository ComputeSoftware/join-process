(def project 'join-process)
(def version "1.0")

(set-env! :resource-paths #{"src"}
          :dependencies '[[org.clojure/clojure "1.9.0"]
                          [me.raynes/conch "0.7.0"]
                          [primrose "1.0.0"]
                          [clansi "1.0.0"]])

(task-options!
  pom {:project     project
       :version     version
       :description "FIXME: write description"
       :url         "http://example/FIXME"
       :scm         {:url "https://github.com/yourname/join-process"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})
