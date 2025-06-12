(ns lupapiste-toj.migrations.remove-invalid-retention-ends
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-commons.schema-utils :as schema-utils]))

(defn ensure-laskentaperuste-ok [{:keys [metadata] :as form}]
  (if-let [laskentaperuste (keyword (get-in metadata [:sailytysaika :laskentaperuste]))]
    (->> (cond-> (schema-utils/coerce-metadata-to-schema tms/AsiakirjaMetaDataMap metadata)
                 (#{:lupapäätöspäivä :päätöksen_lainvoimaisuuspäivä} laskentaperuste) (assoc-in [:sailytysaika :laskentaperuste] :rakennuksen_purkamispäivä))
         (tms/sanitize-metadata)
         (assoc form :metadata))
    form))

(defn remove-invalid-laskentaperuste [db]
  (doseq [collection ["draft" "default-data" "published"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-laskentaperuste-ok document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
