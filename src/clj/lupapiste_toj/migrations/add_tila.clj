(ns lupapiste-toj.migrations.add-tila
  (:require [clojure.walk :refer [postwalk]]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [monger.collection :as mc]))

(defn ensure-tila [form]
  (if (and (map? form) (#{:liite :asiakirja} (keyword (:type form))))
    (update-in form [:metadata] merge tms/asiakirja-default-metadata)
    form))

(defn add-tila-metadata [db]
  (doseq [collection ["draft" "published"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-tila document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
