(ns lupapiste-toj.migrations.core-test
  (:require [clojure.test :refer :all]
            [lupapiste-toj.migrations.core :as core]
            [lupapiste-toj.test-helpers :refer [with-mongo-db mongo with-collections timbre-logging-disabled]]
            [monger.collection :as mc]))

(def +collection+ "foobar")

(defn with-my-collections [f]
  (with-collections [+collection+ "migrations"]
    (f)))

(use-fixtures :each with-my-collections with-mongo-db timbre-logging-disabled)

(defn assert-migrations [expected actual]
  (is (= (count expected) (count actual)))
  (doseq [{:keys [_id description]} actual]
    (is (contains? expected _id))
    (is (= (get-in expected [_id :description])
           description))))

(deftest run-migrations
  (let [{:keys [:db]} @mongo
        run-count (atom 0)
        sample-migrations {1 {:description "set count to two"
                              :op (fn [db]
                                    (swap! run-count inc)
                                    (mc/update-by-id db +collection+ 1 {:count 2}))}}]

    (is (zero? (mc/count db "migrations")) "No migrations run at start")
    (mc/insert db +collection+ {:_id 1 :count 1})

    (core/run-migrations db sample-migrations)

    (let [updated-document (mc/find-one-as-map db +collection+ {:_id 1})
          migrations (mc/find-maps db "migrations")]
      (is (= 2 (:count updated-document)) "Migration done")
      (testing "Migration bookkeeping done"
        (assert-migrations sample-migrations migrations))

      (core/run-migrations db sample-migrations)

      (testing "Migration isn't run twice"
        (is (= migrations (mc/find-maps db "migrations")))
        (is (= 1 @run-count))))))
