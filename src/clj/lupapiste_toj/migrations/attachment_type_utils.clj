(ns lupapiste-toj.migrations.attachment-type-utils
  (:require [monger.collection :as mc]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as s]))

(def used-ids (atom #{}))

(defn replacement-id [{:keys [id]} attachment-mapping]
  (let [[type-group type-id] (s/split (name id) #"\.")]
    (if (and type-group type-id)
      (let [old-key {:type-group (keyword type-group)
                     :type-id (keyword type-id)}
            new-key (get attachment-mapping old-key old-key)]
        (keyword (str (name (:type-group new-key)) "." (name (:type-id new-key)))))
      (keyword id))))

(defn process-attachments [attachments attachment-mapping]
  (->> attachments
       (map (fn [att]
              (let [r-id (replacement-id att attachment-mapping)]
                (when-not (@used-ids r-id)
                  (swap! used-ids conj r-id)
                  (assoc att :id r-id)))))
       (doall)
       (remove nil?)))

(defn convert-to-asiakirja [attachment]
  (assoc attachment :type :asiakirja :nodes []))

(defn process-node [attachment-mapping {:keys [type nodes] :as node}]
  (cond
    (= "toimenpide-tarkenne" type) (->> nodes
                                        (reduce
                                          (fn [acc document]
                                            (let [r-id (replacement-id document attachment-mapping)]
                                              (if (@used-ids r-id)
                                                ;; Remove document, raise attachment one level up
                                                (->> (process-attachments (:nodes document) attachment-mapping)
                                                     (map convert-to-asiakirja)
                                                     (doall)
                                                     (concat acc))
                                                (do
                                                  (swap! used-ids conj r-id)
                                                  (concat acc
                                                          [(assoc document :id r-id
                                                                           :nodes (process-attachments (:nodes document) attachment-mapping))])))))
                                          [])
                                        (assoc node :nodes))
    (seq nodes) (->> (doall (map (partial process-node attachment-mapping) nodes))
                     (assoc node :nodes))
    :else node))

(defn process-asia [attachment-mapping {:keys [nodes] :as asia}]
  (reset! used-ids #{})
  (->> (doall (map (partial process-node attachment-mapping) nodes))
       (assoc asia :nodes)))

(defn migrate-attachment-types [db collections attachment-mapping]
  (doseq [collection collections]
    (doseq [document (mc/find-maps db collection)]
      (let [asiat (get-in document [:tos :nodes 0 :nodes 0 :nodes])
            updated-asiat (doall (map (partial process-asia attachment-mapping) asiat))
            updated-document (assoc-in document [:tos :nodes 0 :nodes 0 :nodes] updated-asiat)]
        (when-not (= document updated-document)
          (mc/update-by-id db collection (:_id document) updated-document))))))
