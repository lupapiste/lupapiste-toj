(ns ^:figwheel-load  lupapiste-toj.components.editor-test
    (:require [lupapiste-toj.components.editor :refer [full-states-in-presentation-order]]
              [lupapiste-commons.states :as states]
              [cljs.test :refer-macros [deftest is testing]]))

(deftest state-order-is-complete-ya
  (is (= (set (keys states/full-ya-application-state-graph))
         (set (full-states-in-presentation-order "YA")))))

(deftest state-order-is-complete-r
  (is (= (set (keys states/r-and-tj-transitions))
         (set (full-states-in-presentation-order "R")))))

(deftest state-order-is-complete-ymp
  (testing "Currently (2022-08-29) all YMP operation uses default-application-state-graph."
        (is (= (set (keys states/default-application-state-graph))
               (set (full-states-in-presentation-order "YMP"))))))
