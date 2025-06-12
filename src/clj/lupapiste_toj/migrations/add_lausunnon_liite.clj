(ns lupapiste-toj.migrations.add-lausunnon-liite
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-commons.schema-utils :as schema-utils]
            [taoensso.timbre :as timbre]
            [lupapiste-toj.persistence :as pers]))

(defn add-attachment-to-statement-doc [{:keys [type id metadata] :as form}]
  (if (and (= :asiakirja (keyword type))
           (= :ennakkoluvat_ja_lausunnot.lausunto (keyword id)))
    (let [att {:type :liite
               :id :ennakkoluvat_ja_lausunnot.lausunnon_liite
               :metadata (schema-utils/coerce-metadata-to-schema tms/AsiakirjaMetaDataMap metadata)}]
      (update form :nodes concat [att]))
    form))

(defn add-lausunnon-liite-attachment [db]
  (doseq [collection ["draft" "default-data"]]
    (doseq [document (mc/find-maps db collection {:tos.tos-type "R"})]
      (let [updated-document (postwalk add-attachment-to-statement-doc document)
            coerced-original-document (pers/coerce-to-schema document)
            coerced-updated-document (pers/coerce-to-schema updated-document)]
        (when-not (= coerced-original-document coerced-updated-document)
          (timbre/info "Added \"lausunnon liite\" attachment to" (:_id document) "of" (:organization document))
          (mc/update-by-id db collection (:_id document) updated-document))))))
