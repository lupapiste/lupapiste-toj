(ns lupapiste-toj.migrations.change-nakyvyys-md-key
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]))

(defn ensure-nakyvyys-changed [form]
  (if (and (map? form) (#{:liite :asiakirja} (keyword (:type form))) (get-in form [:metadata :näkyvyys]))
    (let [{:keys [näkyvyys] :as metadata} (:metadata form)
          new-metadata (-> (assoc metadata :nakyvyys näkyvyys)
                           (dissoc :näkyvyys))]
      (assoc form :metadata new-metadata))
    form))

(defn change-nakyvyys-key [db]
  (doseq [collection ["draft" "published" "default-data"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-nakyvyys-changed document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
