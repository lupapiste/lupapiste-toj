(defproject lupapiste-toj "2023.1"
  :description "Lupapisteen tiedonohjausjärjestelmä"
  :url "https://www.lupapiste.fi"
  :license {:name         "European Union Public Licence v. 1.2"
            :url          "https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12"
            :distribution :manual}
  :dependencies [;; Clojure
                 [org.clojure/clojure "1.10.3"]
                 [http-kit "2.5.3"]
                 [commons-io "2.11.0"]
                 [compojure "1.7.0" :exclusions [commons-io]]
                 [com.stuartsierra/component "1.0.0"]
                 [prismatic/schema "1.2.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [ring-transit "0.1.6"]
                 [ring/ring-json "0.5.1"]
                 ;; 3.x should be compatible with monger, 3.12 is also tested as being MongoDB 5.0 compatible
                 [org.mongodb/mongodb-driver "3.12.10"]
                 [com.novemberain/monger "3.5.0" :exclusions [com.google.guava/guava]]
                 [com.taoensso/timbre "5.1.2"]
                 [viesti/timbre-json-appender "0.2.3"]
                 [org.slf4j/slf4j-api "1.7.32"]
                 [org.slf4j/log4j-over-slf4j "1.7.32" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.32" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jcl-over-slf4j "1.7.32" :exclusions [org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.2.9" :exclusions [org.slf4j/slf4j-api]]
                 [org.clojure/data.xml "0.1.0-beta1"]
                 [com.cemerick/url "0.1.1"]
                 [clj-time "0.15.2"]

                 ;; Explicit dep to this version to avoid conflicts from buddy and others:
                 [com.fasterxml.jackson.core/jackson-databind "2.12.5"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.12.5"]

                 ;; ClojureScript
                 [org.clojure/clojurescript "1.10.879"]

                 ;; The last-ever om theoretically works with React 15, but it logs a lot of warnings
                 [org.omcljs/om "1.0.0-beta4"]
                 [cljsjs/react "15.6.2-5"]
                 [cljsjs/react-dom "15.6.2-5"]
                 [cljsjs/create-react-class "15.6.3-1"]
                 ;; Sablono version must be compatible with Om and React 15.6
                 [sablono "0.8.2" :exclusions [cljsjs/react]]

                 [prismatic/om-tools "0.5.0"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 [cljs-ajax "0.8.4"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]

                 ;; Resources
                 [lupapiste/commons "3.1.10"]
                 [lupapiste/pdfa-generator "1.1.0" :exclusions [org.slf4j/slf4j-log4j12]]]
  :min-lein-version "2.5.1"
  :jvm-opts ["-Xmx2g" "-Djava.awt.headless=true"]
  :main ^:skip-aot lupapiste-toj.app
  :uberjar-name "lupapiste-toj.jar"
  :target-path "target/%s/"
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj"]
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-figwheel "0.5.20" :exclusions [org.clojure/clojure]]
            [lein-less "1.7.5"]]
  :cljsbuild {:builds {:dev {:source-paths ["src/cljs" "src/cljc" "test/cljs" "dev/cljs" "checkouts/lupapiste-commons/src"]
                             :compiler {:main lupapiste-toj.app
                                        :output-to "resources/public/main.js"
                                        :asset-path "/tiedonohjaus/out"}
                             :figwheel {:on-jsload "lupapiste-toj.app/main"}}
                       :prod {:source-paths ["src/cljs" "src/cljc"]
                              :compiler {:main lupapiste-toj.app
                                         :output-to "resources/public/main.js"
                                         :output-dir "resources/public/out"
                                         :source-map "resources/public/main.js.map"
                                         :elide-asserts true
                                         :pretty-print false
                                         :language-in  :ecmascript6
                                         :rewrite-polyfills true
                                         :language-out :ecmascript5
                                         :optimizations :advanced
                                         :closure-extra-annotations ["api" "observable"]}}
                       :test {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
                              :compiler {:output-to "target/cljs/test/test.js"
                                         :output-dir "target/cljs/test"
                                         :optimizations :whitespace
                                         :language-out :ecmascript5
                                         :pretty-print true
                                         :source-map "target/cljs/test/test.js.map"}
                              :notify-command ["./run-karma.sh"]}}
              :test-commands {"ci" ["./node_modules/karma/bin/karma" "start" "karma.ci.conf.js"]}}
  :figwheel {:server-port 3451
             :css-dirs ["resources/public/css"]}
  :less {:source-paths ["src/less"]
         :target-path "resources/public/css"}
  :clean-targets ^{:protect false} ["resources/public/main.js"
                                    "resources/public/main.js.map"
                                    "resources/public/out"
                                    :target-path]
  :profiles {:uberjar {:prep-tasks ^:replace ["clean"
                                              ["less" "once"]
                                              ["cljsbuild" "once" "prod"]
                                              "javac"
                                              "compile"]
                       :aot :all}
             :dev {:dependencies [;; Development dependencies
                                  [reloaded.repl "0.2.4"]
                                  [org.clojure/tools.namespace "1.1.0"]
                                  [enlive "1.1.6"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [ring/ring-mock "0.4.0"]

                                  ;; ClojureScript testing dependencies
                                  [prismatic/dommy "1.1.0"]
                                  [hipo "0.5.2"]]
                   :node-dependencies [[karma "3.0.0"]
                                       [karma-chrome-launcher "2.2.0"]
                                       [karma-phantomjs-launcher "1.0.4"]
                                       [es5-shim "4.5.10"]
                                       [console-polyfill "0.3.0"]
                                       [karma-junit-reporter "1.2.0"]]
                   :aliases {"extract-strings" ["run" "-m" "lupapiste-commons.i18n.extract/extract-strings" "t"]}
                   :plugins [[lein-npm "0.5.0"]
                             [com.jakemccrary/lein-test-refresh "0.25.0"]
                             [test2junit "1.1.0"]]
                   :test-refresh {:notify-command ["./notify.sh"]}
                   :repl-options {:init-ns user}
                   :source-paths ["dev/clj" "test/clj" "src/cljs"]}}
  :manifest {:build-info {"git-commit" ~(fn [_] (.trim (:out (clojure.java.shell/sh "git" "rev-parse" "--verify" "HEAD"))))
                          "build" ~(fn [_] (or (System/getenv "BUILD_TAG") "unknown"))}})
