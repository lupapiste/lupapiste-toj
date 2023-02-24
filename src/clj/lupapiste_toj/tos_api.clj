(ns lupapiste-toj.tos-api
  (:require [lupapiste-toj.persistence :as pers]
            [lupapiste-toj.i18n :as i18n]
            [clojure.string :as string]
            [ring.util.response :refer [response not-found]]
            [lupapiste-toj.records :as records]
            [lupapiste-toj.pdf.pdf-export :as pdf]
            [clojure.string :as s]
            [clojure.data.xml :as xml])
  (:import (java.text SimpleDateFormat)
           (java.util Date)))

(defn- is-parent-of [parent child]
  (if-let [children (:nodes parent)]
    (if (some #(= % child) children)
      true
      (some (fn [node] (is-parent-of node child)) children))
    false))

(defn- find-next-node [nodes target-node]
  (if (some #(= % target-node) nodes)
    target-node
    (first (filter #(is-parent-of % target-node) nodes))))

(defn- build-function-code [codes current-node target-node]
  (let [new-codes (conj codes (:code current-node))]
    (if (or (= current-node target-node) (nil? (:code current-node)))
      (string/join " " (map #(format "%02d" %) (if (nil? (:code current-node)) codes new-codes)))
      (build-function-code new-codes (find-next-node (:nodes current-node) target-node) target-node))))

(defn- asia-names [tree]
  (let [asia-nodes (filter #(= :asia (:type %))
                           (tree-seq :nodes :nodes tree))]
    (map (fn [node]
           {:code (build-function-code [] tree node)
            :name (:text node)}) asia-nodes)))

(def tos-not-found-response
  (not-found {:error "Organisaatiolla ei ole voimassaolevaa tiedonohjaussuunnitelmaa."}))

(defn- tokenize-function-code [function-code]
  (map #(Long/parseLong %) (string/split function-code #" ")))

(defn- find-function-by-code [codes node]
  (when (and (> (count codes) 0) (not (nil? node)) (= (first codes) (:code node)))
    (if (= 1 (count codes))
      node
      (let [next-node (first (filter #(= (second codes) (:code %)) (:nodes node)))]
        (find-function-by-code (rest codes) next-node)))))

(defn- find-document-from-tree [document-id tree]
  (first (filter #(and (contains? #{:asiakirja :liite} (:type %)) (= document-id (:id %)))
                 (tree-seq :nodes :nodes tree))))

(defn- find-metadata-for-document-id [function-code tos document-id]
  (when-let [asia (find-function-by-code (tokenize-function-code function-code) tos)]
    (if-let [document (find-document-from-tree document-id asia)]
      (:metadata document)
      (when (contains? records/all-record-types-including-old-set document-id)
        (:metadata (find-document-from-tree :muut.muu asia))))))

(defn get-asiat [db organization]
  (if-let [tos (:tos (pers/get-currently-valid db organization))]
    (response (sort-by :code (asia-names tos)))
    tos-not-found-response))

(defn get-metadata-for-document-type [db organization function-code document-id]
  (if-let [tos (:tos (pers/get-currently-valid db organization))]
    (if-let [metadata (find-metadata-for-document-id function-code tos (keyword document-id))]
      (response metadata)
      (not-found {:error "Asiakirjaa ei löytynyt."}))
    tos-not-found-response))

(defn get-metadata-for-function [db organization function-code]
  (if-let [tos (:tos (pers/get-currently-valid db organization))]
    (if-let [metadata (:metadata (find-function-by-code (tokenize-function-code function-code) tos))]
      (response metadata)
      (not-found {:error "Käsittelyprosessia ei löytynyt."}))
    tos-not-found-response))

(defn toimenpide-name-for-state [{:keys [application-state text nodes type]} state]
  (cond
    (= state application-state) text
    (and (= :toimenpide type) (seq nodes)) (when-let [sub-tp-name (some #(toimenpide-name-for-state % state) nodes)]
                                             (str text " / " sub-tp-name))))

(defn get-toimenpide-name-for-state [db organization function-code state]
  (if-let [tos (:tos (pers/get-currently-valid db organization))]
    (if-let [asia (find-function-by-code (tokenize-function-code function-code) tos)]
      (if-let [toimenpide-name (some #(toimenpide-name-for-state % (keyword state)) (:nodes asia))]
        (response {:name toimenpide-name})
        (not-found {:error "Tilaa vastaavaa toimenpidettä ei löytynyt."}))
      (not-found {:error "Käsittelyprosessia ei löytynyt."}))
    tos-not-found-response))

(defn format-date [date]
  (let [formatter (SimpleDateFormat. "yyyy-MM-dd")]
    (.format formatter date)))

(defn format-tos-name [tos]
  (-> (str (format-date (or (:valid-from tos) (:published tos))) "-" (:name tos) ".pdf")
      (s/replace #"\s" "_")))

(defn get-tos-pdf [db org-id org-name lang version]
  (if-let [tos (if (= "draft" version)
                 (:tos (pers/get-draft db org-id))
                 (pers/get-published-by-id db org-id version))]
    (let [processed-tos (if (= "draft" version)
                          {:tos       tos
                           :name      (i18n/t "Luonnos" (keyword lang))
                           :published (Date.)
                           :organization org-name}
                          (assoc tos :organization org-name))]
      {:status 200
       :headers {"Content-Type" "application/pdf"
                 "Content-Disposition" (str "attachment;filename=" (format-tos-name processed-tos))}
       :body (pdf/export-tos-pdf processed-tos lang)})
    (not-found {:error "Tiedonohjaussuunnitelmaa ei löydy."})))

(xml/declare-ns "lp" "http://www.lupapiste.fi/onkalo/schemas/sahke2-case-file/2016/6/1")
(xml/alias-ns :n :lp)

(def type-to-xml-elem-map
  {:paatehtava ::n/MainFunction
   :tehtava ::n/FunctionClassification
   :alitehtava ::n/SubFunction
   :asia ::n/SubFunction})

(defn- classification-hiccup [codes {:keys [code nodes type text]} str-code]
  (when code
    (let [xml-type (type type-to-xml-elem-map)
          this-code (first codes)
          next-node (first (filter #(= (second codes) (:code %)) nodes))
          new-str-code (str str-code (when-not (s/blank? str-code) " ") (format "%02d" this-code))
          next-hiccup (classification-hiccup (rest codes) next-node new-str-code)]
      (when (and xml-type (= this-code code) (or (= 1 (count codes)) next-hiccup))
        (cond-> [xml-type [::n/FunctionCode new-str-code] [::n/Title text]]
                next-hiccup (concat [next-hiccup])
                :always (vec))))))

(defn- classification-xml-root [content]
  (xml/element ::n/ClassificationScheme {} content))

(defn get-partial-tos-xml [db org-id function-code]
  (let [{:keys [tos]} (pers/get-currently-valid db org-id)
        hiccup (classification-hiccup (tokenize-function-code function-code) tos "")]
    (if hiccup
      {:status 200
       :headers {"Content-Type" "text/xml"}
       :body (-> hiccup
                 (xml/sexp-as-element)
                 (classification-xml-root)
                 (xml/emit-str))}
      (not-found {:error "Tiedonohjaussuunnitelmaa tai tehtäväluokkaa ei löydy."}))))
