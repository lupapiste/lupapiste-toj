(ns lupapiste-toj.migrations.change-paatos-to-liite
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]))

(defn change-paatos-to-paatosote-liite [form]
  (if (and (map? form) (#{:asiakirja :liite} (keyword (:type form))) (#{:päätös :päätösote} (keyword (:id form))))
    (let [new-id (if (= :päätös (keyword (:id form))) :muut.paatos :muut.paatosote)]
      (assoc form :id new-id))
    form))

(defn change-paatos-to-liite [db]
  (doseq [collection ["draft" "published" "default-data"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk change-paatos-to-paatosote-liite document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
