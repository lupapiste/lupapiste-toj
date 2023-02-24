;; Note: this does not work yet

(set-env!
 :src-paths    #{"src/clj" "src/cljs"}
 :rsc-paths    #{"resources/public"}
 :dependencies '[[adzerk/boot-cljs "0.0-2371-27" :scope "test"]
                 [adzerk/boot-cljs-repl "0.1.6" :scope "test"]
                 [adzerk/boot-reload "0.1.7" :scope "test"]
                 [reloaded.repl "0.1.0" :scope "test"]

                 [org.clojure/clojure "1.7.0-alpha4"]
                 [http-kit "2.1.18"]
                 [compojure "1.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [prismatic/schema "0.3.3"]])

(require
 '[adzerk.boot-cljs      :refer :all]
 '[adzerk.boot-cljs-repl :refer :all]
 '[adzerk.boot-reload    :refer :all]
 '[boot.task-helpers     :refer [once]])

(task-options!
 pom [:project 'lupapiste-toj
      :version "0.1.0"]
 aot [:all true]
 jar [:manifest {"git-commit" (.trim (:out (clojure.java.shell/sh "git" "rev-parse" "--verify" "HEAD")))}
      :main 'lupapiste-toj.system])

(deftask build "Build uberjar" []
  (comp (pom) (aot) (add-src) (uber) (jar)))

(deftask dev "Start deving" []
  (once
   (with-pre-wrap
     (set-env! :src-paths #(conj % "dev")))))

(deftask dev-live "Start live deving" []
  (comp (dev)
        (watch)
        (cljs-repl)
        (cljs :optimizations :none
              :unified true
              :unified true)
        (reload)))
