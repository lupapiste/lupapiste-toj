(ns lupapiste-toj.migrations.update-ymp-default-data-for-attachment-types-v2
  (:require [lupapiste-toj.initial-data :as initial-data]
            [monger.collection :as mc]
            [monger.operators :as mo]))

(defn update-ymp-default-data [db]
  (mc/update db
             "default-data"
             {"tos.tos-type"         "YMP"
              "tos.attachment-types" "Ymparisto-types"}
             {mo/$set {:tos initial-data/ymp-tos}} {:multi false}))
