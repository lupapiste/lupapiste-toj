(ns lupapiste-toj.migrations.add-valid-from
  (:require [lupapiste-toj.domain :as d]
            [monger.collection :as mc]))

(defn ensure-valid-from [document]
  (if (not (:valid-from document))
    (assoc document :valid-from (:published document))
    document))

(defn add-valid-from [db]
  (let [collection "published"]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (ensure-valid-from document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
