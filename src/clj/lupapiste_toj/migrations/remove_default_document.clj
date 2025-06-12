(ns lupapiste-toj.migrations.remove-default-document
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]))

(defn ensure-default-document-removed [form]
  (if-let [nodes (:nodes form)]
    (let [updated-nodes (remove nil? (map (fn [child]
                                            (when-not (and (map? child)
                                                           (#{:asiakirja :liite}(keyword (:type child)))
                                                           (= :default-document (keyword (:id child))))
                                              child)) nodes))]
      (assoc form :nodes updated-nodes))
    form))

(defn remove-default-documents [db]
  (doseq [collection ["draft" "published" "default-data"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-default-document-removed document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
