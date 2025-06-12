(ns lupapiste-toj.migrations.change-lausunto-to-liite
  (:require [lupapiste-commons.tos-metadata-schema :as tms]
            [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]))

(defn ensure-lausunto-has-liite-id [form]
  (if (and (map? form) (= :asiakirja (keyword (:type form))) (= :lausunto (keyword (:id form))))
    (assoc form :id :ennakkoluvat_ja_lausunnot.lausunto)
    form))

(defn change-lausunto-to-liite-id [db]
  (doseq [collection ["draft" "published"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-lausunto-has-liite-id document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
