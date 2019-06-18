(defproject badge "0.3.0"
  :description "badge caching service"
  :url "https://github.com/jaemk/badge"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/core.match "0.3.0"]
                 [nrepl "0.6.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [aleph "0.4.6"]
                 [manifold "0.1.8"]
                 [byte-streams "0.2.4"]
                 [byte-transforms "0.1.4"]
                 [ring/ring-core "1.6.3"]
                 [compojure "1.6.1"]
                 [orchestra "2019.02.06-1"]
                 [hiccup "1.0.5"]
                 [commons-codec/commons-codec "1.11"]
                 [cheshire "5.8.0"]]
  :main ^:skip-aot badge.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-binplus "0.6.5"]
                             [lein-midje "3.2.1"]]
                   :dependencies [[midje "1.9.8"]]
                   :source-paths ["dev"]
                   :main user}}
  :bin {:name "badge"
        :bin-path "bin"
        :jvm-opts ["-server" "-Dfile.encoding=utf-8" "$JVM_OPTS"]
        :custom-preamble "#!/bin/sh\nexec java  -jar $0 \"$@\"\n"})
