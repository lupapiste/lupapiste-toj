(ns lupapiste-toj.migrations.remove-invalid-suojaustaso
(:require [monger.collection :as mc]
  [clojure.walk :refer [postwalk]]))

(defn ensure-suojaustaso-removed [{:keys [metadata] :as form}]
  (if (and (map? form) (= "julkinen" (:julkisuusluokka metadata)) (:suojaustaso metadata))
    (update form :metadata dissoc :suojaustaso)
    form))

(defn remove-suojaustaso-from-julkinen-docs [db]
  (doseq [collection ["draft" "published" "default-data"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-suojaustaso-removed document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
