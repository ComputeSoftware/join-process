(def project 'join-process)
(def version "1.1")

(set-env! :resource-paths #{"src"}
          :repositories #(conj % ["private" {:url "https://repo.deps.co/computesoftware/releases"}])
          :dependencies '[[compute/boot-tasks "2.2"]
                          [org.clojure/clojure "1.9.0" :scope "provided"]
                          [me.raynes/conch "0.7.0"]
                          [primrose "1.0.0"]
                          [clansi "1.0.0"]])

(require
  '[compute.boot-tasks.core :refer [inst deploy]])

(task-options!
  pom {:project     project
       :version     version
       :description "FIXME: write description"
       :url         "http://example/FIXME"
       :scm         {:url "https://github.com/yourname/join-process"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})
