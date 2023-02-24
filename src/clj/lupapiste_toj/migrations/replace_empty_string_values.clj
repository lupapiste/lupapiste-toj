(ns lupapiste-toj.migrations.replace-empty-string-values
  (:require [clojure.walk :refer [postwalk]]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [monger.collection :as mc]
            [clojure.string :as string]))

(def default-value "<Arvo puuttuu>")

(defn replace-blank-value [metadata ks]
  (if-let [value (get-in metadata ks)]
    (if (string/blank? value)
      (assoc-in metadata ks default-value)
      metadata)
    metadata))

(defn ensure-non-blank-values [form]
  (if (and (map? form) (seq (:metadata form)))
    (-> (:metadata form)
        (replace-blank-value [:salassapitoperuste])
        (replace-blank-value [:sailytysaika :perustelu])
        (replace-blank-value [:tila])
        (->> (assoc form :metadata)))
    form))

(defn set-values-to-empty-required-fields [db]
  (doseq [collection ["draft" "published"]]
    (doseq [document (mc/find-maps db collection)]
      (let [updated-document (postwalk ensure-non-blank-values document)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
