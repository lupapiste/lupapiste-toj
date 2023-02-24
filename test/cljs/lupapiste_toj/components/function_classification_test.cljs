(ns ^:figwheel-load lupapiste-toj.components.function-classification-test
  (:require [lupapiste-toj.components.function-classification :as f]
            [cljs.test :refer-macros [deftest is]]))

(deftest parse-invalid-code
  (is (nil? (f/parse-code #{1 2} "foo")) "not a number")
  (is (nil? (f/parse-code #{1 2} "1")) "taken")
  (is (nil? (f/parse-code #{1 2} 1)) "taken as number"))

(deftest parse-valid-code
  (is (= 3 (f/parse-code #{1 2} "3")) "string")
  (is (= 3 (f/parse-code #{1 2} 3)) "number"))
