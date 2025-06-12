(ns lupapiste-toj.migrations.change-tila-to-enum
  (:require [lupapiste-commons.tos-metadata-schema :as tms]
            [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]))

(defn ensure-tila-enum [form]
  (if (and (map? form) (#{:liite :asiakirja} (keyword (:type form))))
    (let [{:keys [tila]} (:metadata form)
          new-tila (if (.equalsIgnoreCase tila "valmis") :valmis :luonnos)]
      (assoc-in form [:metadata :tila] new-tila))
    form))

(defn change-tila-metadata-to-enum [db]
  (doseq [collection ["draft" "published" "default_data"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-tila-enum document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
