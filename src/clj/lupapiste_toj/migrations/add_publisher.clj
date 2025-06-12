(ns lupapiste-toj.migrations.add-publisher
  (:require [lupapiste-toj.domain :as d]
            [monger.collection :as mc]))

(defn ensure-publisher [document]
  (if (not (:publisher document))
    (assoc document :publisher {:first-name "" :last-name ""})
    document))

(defn add-publisher [db]
  (let [collection "published"]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (ensure-publisher document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
