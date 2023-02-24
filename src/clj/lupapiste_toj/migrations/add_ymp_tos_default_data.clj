(ns lupapiste-toj.migrations.add-ymp-tos-default-data
  (:require [monger.collection :as mc]
            [lupapiste-toj.initial-data :as initial-data])
  (:import (java.util Date)
           (org.bson.types ObjectId)))

(defn create-ymp-default-data [db]
  (mc/insert db "default-data" {:_id (ObjectId.)
                                :organization "default"
                                :modified (Date.)
                                :tos initial-data/ymp-tos}))
