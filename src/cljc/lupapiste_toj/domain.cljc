(ns lupapiste-toj.domain
  (:require [schema.core :as s :include-macros true]
            [lupapiste-toj.records :as records]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-commons.states :as states]
            [lupapiste-commons.attachment-types :as attachment-types]))

(defn child-codes-unique? [{:keys [nodes]}]
  (if nodes
    (apply distinct? (map :code nodes))
    true))

(defn document-types-unique? [node]
  (if (:nodes node)
    (if-let [ids (->> (tree-seq :nodes :nodes node)
                      (map :id)
                      (remove nil?)
                      (seq))]
      (apply distinct? ids)
      true)
    true))

(def Liite
  {:type (s/eq :liite)
   :id s/Keyword
   :metadata tms/TojAsiakirjaMetaDataMap})

(def Asiakirja
  {:type (s/eq :asiakirja)
   :id s/Keyword
   :metadata tms/TojAsiakirjaMetaDataMap
   :nodes [Liite]})

(def ToimenpideTarkenne
  {:text s/Str
   :type (s/eq :toimenpide-tarkenne)
   (s/optional-key :application-state) (apply s/enum (keys states/all-transitions-graph))
   :nodes [Asiakirja]})

(def Toimenpide
  {:text s/Str
   :type (s/eq :toimenpide)
   (s/optional-key :application-state) (apply s/enum (keys states/all-transitions-graph))
   :nodes [ToimenpideTarkenne]})

(def Asia
  {:text s/Str
   :type (s/eq :asia)
   :code s/Int
   :metadata tms/ConstrainedMetadataMap
   :nodes [Toimenpide]})

(def Alitehtava
  {:text s/Str
   :code s/Int
   :type (s/eq :alitehtava)
   :nodes [(s/constrained Asia document-types-unique? "Childs have unique :id, if any")]})

(def Tehtava
  {:text s/Str
   :code s/Int
   :type (s/eq :tehtava)
   :nodes [(s/constrained Alitehtava child-codes-unique? "Childs have unique :code")]})

(def max-paatehtava 14)

(def Paatehtava
  {:text s/Str
   :code (apply s/enum (range (inc max-paatehtava)))
   :type (s/eq :paatehtava)
   :tos-type s/Str
   :attachment-types s/Str
   :nodes [(s/constrained Tehtava child-codes-unique? "Childs have unique :code")]})

(def TOS Paatehtava)

(defn childs
  "Hackish way to hop onto the 'Alitehtava' child schema from 'Tehtava'.

  If a node has :nodes, return it, otherwise check if the node is a
  composite schema with :schema (a Constrained schema) and return :nodes of
  that schema."
  [node]
  (if-let [schema (:schema node)]
    (:nodes schema)
    (:nodes node)))

(defn schema-seq []
  (for [schema (tree-seq childs childs TOS)]
    (or (:schema schema) schema)))

(defn parent-child-pairs [] (partition 2 1 (map (comp :v :type) (schema-seq))))

(defn type-of-child [type-of-parent]
  (when-first [[parent child] (filter (fn [[parent child]] (= parent type-of-parent))
                                      (parent-child-pairs))]
    child))

(defn types-with-field [field]
  (set (map #(.-v (:type %)) (filter field (schema-seq)))))

(defn has-field? [type field]
  ((types-with-field field) type))

(defn has-metadata-key? [type key]
  (let [type-schema (first (filter #(= type (.-v (:type %))) (schema-seq)))
        metadata (get-in type-schema [:metadata :schema] (:metadata type-schema))]
    (contains? metadata key)))

(defn metadata-for-type [type]
  (cond-> [:function-classification]
    (has-field? type :metadata) (concat tms/common-metadata-fields)
    (#{:asiakirja :liite} type) (concat tms/toj-asiakirja-metadata-fields)))

(defn adding-child-allowed? [type]
  (not (#{:paatehtava :tehtava} type)))

(defn editable? [type]
  (not (#{:paatehtava :tehtava :alitehtava} type)))

(defn selectable? [type]
  ((types-with-field :id) type))

(defn default-metadata-for-type [type]
  (if (has-metadata-key? type :tila)
    tms/asiakirja-default-metadata
    tms/default-metadata))

(defn find-parent-of-type [path type tree]
  (letfn [(rec [path type]
            (let [parent (get-in tree path)]
              (if (= type (:type parent))
                parent
                (let [parent-path (drop-last 2 path)]
                  (when-not (= path parent-path)
                    (rec parent-path type))))))]
    (rec path type)))

(defn children-with-id [parent]
  (filter :id (tree-seq :nodes :nodes parent)))

(defn child-options [tree parent parent-path]
  (let [parent-asia (find-parent-of-type parent-path :asia tree)
        family (children-with-id parent-asia)
        taken-options (set (map :id family))]
    (filter #(not (taken-options %))
            (records/allowed-values (:type parent) (:attachment-types tree)))))

(defn can-add-child? [tree {:keys [type] :as node} path]
  (when-let [child-type (type-of-child type)]
    (and (adding-child-allowed? type)
         (if (selectable? child-type)
           (not (empty? (child-options tree node path)))
           true))))

(defn nodes-of-type [type tree]
  (filter #(= type (:type %))
          (tree-seq :nodes :nodes tree)))

(defn next-code [tree type]
  (let [codes (map :code (nodes-of-type type tree))]
    (if (empty? codes)
      0
      (inc (apply max codes)))))

(defn children-cloneable? [type]
  (= :asia type))

(defn cloneable-nodes [tree]
  (vec (filter #(seq (:nodes %)) (nodes-of-type :asia tree))))
