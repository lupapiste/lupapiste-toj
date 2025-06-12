(ns ^:figwheel-always lupapiste-toj.test-runner
  (:require [cljs.test :refer-macros [run-all-tests]]
            lupapiste-toj.app-test
            lupapiste-toj.components.node-test
            lupapiste-toj.components.function-classification-test
            lupapiste-toj.shared-utils-test))

(defn run []
  (run-all-tests #"lupapiste.*-test$"))
