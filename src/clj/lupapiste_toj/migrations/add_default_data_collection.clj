(ns lupapiste-toj.migrations.add-default-data-collection
  (:require [monger.collection :as mc]
            [lupapiste-toj.initial-data :as initial-data])
  (:import (java.util Date)
           (org.bson.types ObjectId)))

(defn create-collection-default-data [db]
  (let [collection "default_data"]
    (when-not (mc/exists? db collection)
      (mc/create db collection {:capped true :max 1 :size 32000000})
      (mc/insert db collection {:_id (ObjectId.)
                                :organization "default"
                                :modified (Date.)
                                :tos initial-data/tos}))))

(defn create-new-default-data
  ([db]
   (create-new-default-data db "default-data"))
  ([db collection]
   (when-not (mc/exists? db collection)
     (mc/create db collection {})
     (mc/insert db collection {:_id (ObjectId.)
                               :organization "default"
                               :modified (Date.)
                               :tos initial-data/tos}))))

(defn drop-default-data-and-recreate-as-normal-collection [db]
  (let [old-collection "default_data"
        new-collection "default-data"]
    (when (mc/exists? db old-collection)
      (mc/drop db old-collection))
    (create-new-default-data db new-collection)))
