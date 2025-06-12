(ns lupapiste-toj.migrations.update-ymp-drafts-for-attachment-types-v2
  (:require [clojure.walk :as walk]
            [lupapiste-toj.records :as records]
            [monger.collection :as mc]))

(def ^:private current-record-set (->> records/all-record-types-ymp
                                       (map name)
                                       set))

(defn- update-attachment-types
  [document]
  (assoc-in document [:tos :attachment-types] "Ymparisto-types-v2"))

(defn- remove-deprecated-nodes
  [document]
  (let [removable-node? (fn [form]
                          (and (map? form)
                               (contains? #{"asiakirja" "liite"} (:type form))
                               (some? (:id form))
                               (not (contains? current-record-set (:id form)))))]
    (walk/postwalk (fn [form]
                     (if (and (map? form)
                              (contains? #{"toimenpide-tarkenne" "asiakirja"} (:type form)))
                       (assoc form :nodes (->> (:nodes form)
                                               (remove #(removable-node? %))
                                               vec))
                       form))
                   document)))

(defn- update-document
  [document]
  {:pre [(and (= (-> document :tos :tos-type) "YMP")
              (= (-> document :tos :attachment-types) "Ymparisto-types"))]}
  (-> document
      update-attachment-types
      remove-deprecated-nodes))

(defn update-ymp-drafts [db]
  (let [collection "draft"
        documents  (mc/find-maps db
                                 collection
                                 {"tos.tos-type"         "YMP"
                                  "tos.attachment-types" "Ymparisto-types"})]
    (doseq [document documents
            :let [updated-document (update-document document)]
            :when (not= document updated-document)]
      (mc/update-by-id db collection (:_id document) updated-document))))
