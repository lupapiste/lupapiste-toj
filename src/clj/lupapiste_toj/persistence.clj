(ns lupapiste-toj.persistence
  (:require [clojure.walk :refer [postwalk]]
            [lupapiste-toj.domain :as d]
            [lupapiste-commons.shared-utils :refer [dissoc-in flip]]
            [monger.collection :as mc]
            [monger.query :as mq]
            [schema.core :as s]
            [lupapiste-toj.records :as records]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-commons.schema-utils :as schema-utils])
  (:import [org.bson.types ObjectId]
           (java.util Date)))

(def Document
  {:_id ObjectId
   :organization s/Str
   :modified Date
   :tos d/TOS})

(def Publisher {:first-name s/Str :last-name s/Str})

(def PublishedDocument
  (assoc Document
         :published Date
         :valid-from Date
         :name s/Str
         :publisher Publisher))

(defn- update-values [m f]
 (reduce (fn [acc [k v]] (assoc acc k (f k v))) {} m))

(defn- keywordize-values [key-set k v]
  (if (key-set k) (keyword v) v))

(defn- to-schema-form [e]
  (cond-> e
          (seq (:metadata e)) (update :metadata #(schema-utils/coerce-metadata-to-schema tms/AsiakirjaMetaDataMap %))
          (map? e) (update-values (partial keywordize-values #{:type
                                                               :id
                                                               :application-state}))))

(defn coerce-to-schema [doc]
  (postwalk to-schema-form doc))

(defn coerce-and-validate [schema doc]
  (s/validate schema (coerce-to-schema doc)))

(defn tos-type [organization]
  (last (re-matches #".+-(R|YA|YMP)$" organization)))

(defn create-draft
  ([db organization]
  (let [tos-type (tos-type organization)
        default-tos (get (mc/find-one-as-map db "default-data" {"tos.tos-type" tos-type}) :tos)
        new-draft (coerce-and-validate Document {:_id (ObjectId.)
                                                 :organization organization
                                                 :modified (Date.)
                                                 :tos default-tos})]
    (mc/insert db "draft" new-draft)
    new-draft))
  ([db organization tos]
   (let [new-draft (coerce-and-validate Document {:_id (ObjectId.)
                                                  :organization organization
                                                  :modified (Date.)
                                                  :tos tos})]
     (mc/insert db "draft" new-draft)
     new-draft)))

(defn get-draft [db organization]
  (if-let [draft (mc/find-one-as-map db "draft" {:organization organization})]
    (coerce-and-validate Document draft)
    (when (tos-type organization)
      (create-draft db organization))))

(defn- mongo-update-draft [db organization new-tos]
  (let [new-values {:modified (Date.) :tos new-tos}]
    (s/validate d/TOS (:tos (coerce-to-schema (mc/find-and-modify db "draft" {:organization organization} {"$set" new-values} {:return-new true}))))))

(defmulti handle-edit (fn [edit-type data path tos] edit-type))

(defmethod handle-edit :set-value
  [edit-type {:keys [key value]} path tos]
  (assoc-in tos (conj path key) value))

(defmethod handle-edit :add-node
  [edit-type new-node path tos]
  (update-in tos path conj new-node))

(defmethod handle-edit :remove-node
  [edit-type _ path tos]
  (dissoc-in tos path))

(defmethod handle-edit :move-node
  [edit-type {:keys [new-path]} path tos]
  (let [new-parent (get-in tos (butlast new-path))
        node (get-in tos path)
        real-asiakirja-type-set (set records/records)
        modified-node (cond
                        (and (= :toimenpide-tarkenne (:type new-parent)) (= :liite (:type node))) (assoc node :type :asiakirja :nodes [])
                        (and (= :asiakirja (:type new-parent)) (contains? real-asiakirja-type-set (:id new-parent)) (= :asiakirja (:type node)) (not (contains? real-asiakirja-type-set (:id node)))) (-> (assoc node :type :liite) (dissoc :nodes))
                        :else node)
        new-nodes (conj (get-in tos new-path) modified-node)
        modified-tos (assoc-in tos new-path new-nodes)]
    (dissoc-in modified-tos path)))

(defn update-draft [db edit organization]
  (let [doc (coerce-to-schema (mc/find-one-as-map db "draft" {:organization organization}))
        tos (:tos doc)]
    (if (or (sequential? edit) (seq? edit))
      (mongo-update-draft db organization (reduce (fn [acc {:keys [edit-type data path]}]
                                                    (s/validate d/TOS (handle-edit edit-type data path acc)))
                                                  tos edit))
      (let [{:keys [edit-type data path]} edit]
        (mongo-update-draft db organization (s/validate d/TOS (handle-edit edit-type data path tos)))))))

(defn get-published
  ([db organization]
   (get-published db organization {}))
  ([db organization query]
    (for [doc (->> (mc/find-maps db "published" (merge {:organization organization} query))
                   (map (fn [x] [[(:valid-from x) (:published x)] x]))
                   (sort-by first)
                   (reverse)
                   (map second))]
      (assoc (s/validate PublishedDocument (coerce-to-schema doc)) :_id (.toString (:_id doc))))))

(defn publish-document [doc name valid-from publisher]
  (-> doc
      (assoc :_id (ObjectId.)
             :published (Date.)
             :valid-from valid-from
             :name name
             :publisher publisher)
      ((flip s/validate) PublishedDocument)))

(defn publish [db organization name valid-from publisher]
  (let [draft (-> (mc/find-one-as-map db "draft" {:organization organization})
                  coerce-to-schema
                  ((flip s/validate) Document))]
    (mc/insert db "published" (publish-document draft name valid-from publisher)))
  (get-published db organization))

(defn get-currently-valid [db organization]
  (when-let [doc (-> (mq/with-collection db "published"
                       (mq/find {:organization organization
                                 :valid-from   {"$lt" (Date.)}})
                       (mq/sort {:valid-from -1
                                 :published  -1})
                       (mq/limit 1))
                     first)]
    (-> (->> (coerce-to-schema doc)
             (s/validate PublishedDocument))
        (assoc :_id (.toString (:_id doc))))))

(defn get-published-by-id [db organization ^String id]
  (try (first (get-published db organization {:_id (ObjectId. id)}))
       (catch IllegalArgumentException _
         nil)))
