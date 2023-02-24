(ns lupapiste-toj.migrations.change-to-permanent-archiving
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-commons.schema-utils :as schema-utils]
            [taoensso.timbre :as timbre]
            [lupapiste-toj.persistence :as pers]))

(defn ensure-proper-retention [{:keys [metadata] :as form}]
  (let [arkistointi (keyword (get-in metadata [:sailytysaika :arkistointi]))]
    (if (#{:määräajan :toistaiseksi :ikuisesti} arkistointi)
      (->> (-> (schema-utils/coerce-metadata-to-schema tms/AsiakirjaMetaDataMap metadata)
               (assoc-in [:sailytysaika :arkistointi] :ikuisesti)
               ; Change legal justification to the latest National Archives ruling
               (assoc-in [:sailytysaika :perustelu] "AL/17413/07.01.01.03.01/2016"))
           (tms/sanitize-metadata)
           (assoc form :metadata))
      form)))

(defn change-all-r-orgs-archived-docs-to-permanent-retention [db]
  (doseq [collection ["draft" "default-data"]]
    (doseq [document (mc/find-maps db collection {:tos.tos-type "R"})]
      (let [updated-document (postwalk ensure-proper-retention document)
            coerced-original-document (pers/coerce-to-schema document)
            coerced-updated-document (pers/coerce-to-schema updated-document)]
        (when-not (= coerced-original-document coerced-updated-document)
          (timbre/info "Updated retention periods in" (:_id document) "of" (:organization document))
          (mc/update-by-id db collection (:_id document) updated-document))))))
