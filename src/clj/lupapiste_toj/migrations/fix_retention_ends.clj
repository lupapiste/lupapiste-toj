(ns lupapiste-toj.migrations.fix-retention-ends
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-commons.schema-utils :as schema-utils]))

(defn ensure-laskentaperuste-ok [{:keys [metadata] :as form}]
  (let [laskentaperuste (get-in metadata [:sailytysaika :laskentaperuste])
        arkistointi (keyword (get-in metadata [:sailytysaika :arkistointi]))]
    (if (and (= :toistaiseksi arkistointi) (nil? laskentaperuste))
      (->> (-> (schema-utils/coerce-metadata-to-schema tms/AsiakirjaMetaDataMap metadata)
               (assoc-in [:sailytysaika :laskentaperuste] :rakennuksen_purkamispäivä))
           (tms/sanitize-metadata)
           (assoc form :metadata))
      form)))

(defn add-missing-laskentaperuste [db]
  (doseq [collection ["draft" "default-data" "published"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-laskentaperuste-ok document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
