(ns lupapiste-toj.test-helpers
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            [taoensso.timbre :as timbre]
            [lupapiste-toj.db :as db]
            [lupapiste-toj.migrations.add-default-data-collection :refer [create-new-default-data]]
            [lupapiste-toj.migrations.add-ya-tos-default-data :refer [create-ya-default-data]]
            [lupapiste-toj.migrations.add-ymp-tos-default-data :refer [create-ymp-default-data]]))

(def mongo (atom {}))

(def ^:dynamic *collections* ["draft" "published" "default_data" "default-data"])

(defn- setup [db collections]
  (db/silence-mongo-logging)
  (let [{:keys [db conn] :as result} (mg/connect-via-uri (str "mongodb://localhost/" db))]
    (doseq [collection collections]
      (mc/drop db collection))
    (reset! mongo (assoc result :collection collections))
    (create-new-default-data db)
    (create-ya-default-data db)
    (create-ymp-default-data db)))

(defn- teardown [conn]
  (.close conn))

(defn with-mongo-db [f]
  (setup "test-db" *collections*)
  (f)
  (teardown (:conn @mongo)))

(defmacro with-collections [collections & body]
  `(binding [*collections* ~collections]
     ~@body))

(defn timbre-logging-disabled [f]
  (timbre/merge-config! {:appenders {:println {:enabled? false}}})
  (f)
  (timbre/merge-config! {:appenders {:println {:enabled? true}}}))
