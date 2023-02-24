(ns lupapiste-toj.migrations.add-kayttajaryhma
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]))

(defn ensure-kayttajaryhma-exists [{:keys [metadata] :as form}]
  (if (and (map? form) (#{:osittain-salassapidettava :salainen} (keyword (:julkisuusluokka metadata))) (nil? (:kayttajaryhma metadata)))
    (-> (assoc-in form [:metadata :kayttajaryhma] :viranomaisryhma)
        (assoc-in [:metadata :kayttajaryhmakuvaus] :muokkausoikeus))
    form))

(defn add-kayttajaryhma-for-secret-documents [db]
  (doseq [collection ["draft" "published" "default-data"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-kayttajaryhma-exists document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
