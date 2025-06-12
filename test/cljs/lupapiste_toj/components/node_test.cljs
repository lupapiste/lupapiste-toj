(ns ^:figwheel-load lupapiste-toj.components.node-test
  (:require [lupapiste-toj.components.node :refer [Node]]
            [cljs.test :as test :refer-macros [deftest is]]
            cljsjs.react))

(deftest sample-test
  (is (= 1 1)))
