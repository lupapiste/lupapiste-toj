(ns lupapiste-toj.migrations.add-myyntipalvelu-and-nakyvyys
  (:require [clojure.walk :refer [postwalk]]
            [monger.collection :as mc]))

(defn- ensure-myyntipalvelu-nakyvyys [form]
  (if (and (map? form) (#{:liite :asiakirja} (keyword (:type form))))
    (->> (merge (:metadata form) {:myyntipalvelu true
                                  :näkyvyys :julkinen})
         (assoc form :metadata))
    form))

(defn add-myyntipalvelu-and-nakyvyys-metadata [db]
  (doseq [collection ["draft" "published"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-myyntipalvelu-nakyvyys document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
