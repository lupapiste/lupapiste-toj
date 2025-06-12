(defproject lupapiste-toj "1.5.0-SNAPSHOT"
  :description "Lupapisteen tiedonohjausjärjestelmä"
  :url "https://www.lupapiste.fi"
  :license {:name         "European Union Public Licence v. 1.2"
            :url          "https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12"
            :distribution :manual}
  :dependencies [;; Clojure
                 [org.clojure/clojure "1.12.0"]
                 [http-kit "2.8.0"]
                 [commons-io "2.18.0"]
                 [ring/ring-core "1.13.0"]
                 [compojure "1.7.1" :exclusions [commons-io]]
                 [com.stuartsierra/component "1.1.0"]
                 [prismatic/schema "1.4.1"]
                 [com.cognitect/transit-clj "1.0.333"]
                 [ring-transit "0.1.6"]
                 [ring/ring-json "0.5.1"]
                 ;; 3.x should be compatible with monger, 3.12 is also tested as being MongoDB 5.0 compatible
                 [org.mongodb/mongodb-driver "3.12.14"]
                 [com.novemberain/monger "3.6.0" :exclusions [com.google.guava/guava]]
                 [com.taoensso/timbre "6.6.1"]
                 [viesti/timbre-json-appender "0.2.14"]
                 ;; Logback 1.2 is not compatible with SLF4J 2.0+
                 [org.slf4j/slf4j-api "1.7.36" :upgrade false]
                 [org.slf4j/jcl-over-slf4j "1.7.36" :upgrade false]
                 [org.slf4j/jul-to-slf4j "1.7.36" :upgrade false]
                 [org.slf4j/log4j-over-slf4j "1.7.36" :upgrade false]
                 ;; Logback 1.3.0+ is not compatible with wunderboss LogbackUtil which immutant depends on
                 [ch.qos.logback/logback-classic "1.2.13" :exclusions [org.slf4j/slf4j-api] :upgrade false]
                 [org.clojure/data.xml "0.1.0-beta1"]
                 [com.cemerick/url "0.1.1"]
                 [clj-time "0.15.2"]

                 ;; Explicit dep to this version to avoid conflicts from buddy and others:
                 [com.fasterxml.jackson.core/jackson-databind "2.18.2"]
                 [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.18.2"]

                 ;; ClojureScript
                 [org.clojure/clojurescript "1.10.879"]

                 ;; The last-ever om works with React 15
                 [org.omcljs/om "1.0.0-beta4"]
                 [cljsjs/react "15.6.2-5" :upgrade false]
                 [cljsjs/react-dom "15.6.2-5" :upgrade false]
                 [cljsjs/create-react-class "15.6.3-1" :upgrade false]
                 ;; Sablono version must be compatible with Om and React 15.6
                 [sablono "0.8.2" :exclusions [cljsjs/react] :upgrade false]

                 [prismatic/om-tools "0.5.0"]
                 [com.cognitect/transit-cljs "0.8.280"]
                 [cljs-ajax "0.8.4"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]

                 ;; Resources
                 [org.flatland/ordered "1.15.10"] ; 1.15.12 causes `lein figwheel` to crash.
                 [lupapiste/commons "5.3.12" :exclusions [org.flatland/ordered]]
                 [lupapiste/pdfa-generator "1.1.1" :exclusions [org.slf4j/slf4j-log4j12]]

                 [net.java.dev.jna/jna "5.16.0"]
                 [net.java.dev.jna/jna-platform "5.16.0"]]
  :min-lein-version "2.5.1"
  :jvm-opts ["-Xmx2g" "-Djava.awt.headless=true"]
  :main ^:skip-aot lupapiste-toj.app
  :uberjar-name "lupapiste-toj.jar"
  :target-path "target/%s/"
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj"]
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-shell "0.5.0"]
            [lein-figwheel "0.5.20" :exclusions [org.clojure/clojure]]]
  :cljsbuild {:builds {:dev  {:source-paths ["src/cljs" "src/cljc"]
                              :compiler     {:main         lupapiste-toj.app
                                             :output-to    "resources/public/main.js"
                                             :asset-path   "/tiedonohjaus/out"
                                             :language-in  :es6
                                             :language-out :es-next}
                              ;; Automatic JS reload seems to crash with a StackOverflowError at least on modern JVM and
                              ;; ARM Macs. When this is commented out, Figwheel automatically compiles CLJS changes
                              ;; but you need to reload the page in browser to see the changes.
                              ;:figwheel {:on-jsload "lupapiste-toj.app/main"}
                              }
                       :prod {:source-paths ["src/cljs" "src/cljc"]
                              :compiler     {:main                      lupapiste-toj.app
                                             :output-to                 "resources/public/main.js"
                                             :output-dir                "resources/public/out"
                                             :source-map                "resources/public/main.js.map"
                                             :elide-asserts             true
                                             :pretty-print              false
                                             :language-in               :es6
                                             :language-out              :es-next
                                             :optimizations             :advanced
                                             :closure-extra-annotations ["api" "observable"]}}
                       :test {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
                              :compiler     {:output-to     "target/cljs/test/test.js"
                                             :output-dir    "target/cljs/test"
                                             :optimizations :whitespace
                                             :language-out  :es-next
                                             :pretty-print  true
                                             :source-map    "target/cljs/test/test.js.map"}}}}
  :figwheel {:server-port 3451
             :css-dirs    ["resources/public/css"]}
  :clean-targets ^{:protect false} ["resources/public/main.js"
                                    "resources/public/main.js.map"
                                    "resources/public/out"
                                    :target-path]
  :profiles {:uberjar {:prep-tasks ^:replace ["clean"
                                              ["shell" "npm" "run" "less-build"]
                                              ["cljsbuild" "once" "prod"]
                                              "javac"
                                              "compile"]
                       :aot        :all}
             :dev     {:dependencies [;; Development dependencies
                                      [reloaded.repl "0.2.4"]
                                      [org.clojure/tools.namespace "1.5.0"]
                                      [enlive "1.1.6"]
                                      [ring/ring-mock "0.4.0"]

                                      ;; ClojureScript testing dependencies
                                      [prismatic/dommy "1.1.0"]
                                      [hipo "0.5.2"]]
                       :aliases      {"extract-strings" ["run" "-m" "lupapiste-commons.i18n.extract/extract-strings" "t"]}
                       :plugins      [[test2junit "1.1.0"]]
                       :repl-options {:init-ns user}
                       :source-paths ["dev/clj" "test/clj" "src/cljs"]}}
  :manifest {:build-info {"git-commit" ~(fn [_] (.trim (:out (clojure.java.shell/sh "git" "rev-parse" "--verify" "HEAD"))))
                          "build"      ~(fn [_] (or (System/getenv "BUILD_TAG") "unknown"))}})
