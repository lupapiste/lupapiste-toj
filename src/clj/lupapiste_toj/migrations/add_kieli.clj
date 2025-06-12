(ns lupapiste-toj.migrations.add-kieli
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]))

(defn ensure-kieli-exists [{:keys [metadata] :as form}]
  (if (and (map? form) metadata (nil? (:kieli metadata)))
    (assoc-in form [:metadata :kieli] :fi)
    form))

(defn add-kieli-to-metadata [db]
  (doseq [collection ["draft" "published" "default-data"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-kieli-exists document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
