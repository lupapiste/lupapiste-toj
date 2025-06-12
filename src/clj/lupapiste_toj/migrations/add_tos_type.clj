(ns lupapiste-toj.migrations.add-tos-type
  (:require [lupapiste-toj.domain :as d]
            [monger.collection :as mc]))

(defn ensure-tos-type [document]
  (if (not (get-in document [:tos :tos-type]))
    (assoc-in document [:tos :tos-type] "R")
    document))

(defn ensure-attachment-types [document]
  (if (not (get-in document [:tos :attachment-types]))
    (assoc-in document [:tos :attachment-types] "Rakennusluvat-v2")
    document))

(defn ensure-document [document]
  (ensure-attachment-types (ensure-tos-type document)))

(defn add-tos-type [db]
    (doseq [collection ["default-data" "draft" "published"]]
      (doseq [document (mc/find-maps db collection)]
        (let [updated-document (ensure-document document)]
          (when-not (= document updated-document)
            (mc/update-by-id db collection (:_id document) updated-document))))))