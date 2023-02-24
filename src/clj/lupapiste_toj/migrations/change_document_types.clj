(ns lupapiste-toj.migrations.change-document-types
  (:require [clojure.walk :refer [postwalk]]
             [monger.collection :as mc]))

(defn change-child-ids-or-remove [form]
  (if-let [nodes (:nodes form)]
    (let [updated-nodes (remove nil? (map (fn [child]
                                       (if (and (map? child) (= "asiakirja" (:type child)))
                                         (case (:id child)
                                           "rakennuslupahakemus" (assoc child :id "hakemus")
                                           "neuvontapyyntö" (assoc child :id "ilmoitus")
                                           "ympäristölupahakemus" nil
                                           child)
                                         child)) nodes))]
      (assoc form :nodes updated-nodes))
    form))

(defn update-asiakirja-ids [db]
  (doseq [collection ["draft" "published"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk change-child-ids-or-remove document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
