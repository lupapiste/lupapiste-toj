(ns lupapiste-toj.pdf.pdf-export
  (:require [lupapiste-toj.app-schema :as a]
            [lupapiste-toj.authorization :as authorization]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.lupapiste :as lupis]
            [lupapiste-toj.persistence :as p]
            [pdfa-generator.core :as pdf]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clj-time.local :as l]
            [taoensso.timbre :as timbre :refer [trace tracef debug debugf info infof warn warnf error errorf fatal fatalf]]
            [clojure.string :as string])
  (:import (javax.imageio ImageIO)
           (java.io ByteArrayOutputStream ByteArrayInputStream)
           (java.util Date)
           (java.text SimpleDateFormat)))

(def sdf (f/formatter "dd.MM.yyyy HH:mm"))

;;TODO: Make these counters reset per document (thread local)
(def toimenpide-code (atom 0))
(def toimenpide-tarkenne-code (atom 0))
(def asiakirja-code (atom 0))
(def liite-code (atom 0))

(defn build-path [parent-code {:keys [id code type]}]
  (cons (cond
          (= type :toimenpide) (swap! toimenpide-code inc)
          (= type :toimenpide-tarkenne) (swap! toimenpide-tarkenne-code inc)
          (= type :asiakirja) (swap! asiakirja-code inc)
          (= type :liite) (swap! liite-code inc)
          :else (int code))
        parent-code))

(defn- render-title [data]
  (str (:000title data) " (" (:path data) ")"))

(defn- render-path [pre data]
  (str pre (:path data)))

(defn- render-code [val]
  (clojure.string/join "." (take 4 (re-seq #"\d+" val))))


(defn collect-data [lang path {:keys [metadata] :as data}]
  [(into (sorted-map) (cond-> metadata
                              true (assoc :path (clojure.string/join "." (reverse (build-path path data))))
                              true (assoc :001type (:type data))
                              true (assoc :000title (str (if (empty? (:text data)) (t (name (:id data)) lang) (:text data))))
                              (or (= (get-in metadata [:sailytysaika :arkistointi]) :ei)
                                  (= (get-in metadata [:sailytysaika :arkistointi]) :ikuisesti))
                              (assoc :sailytysaika (dissoc (:sailytysaika metadata) :pituus :laskentaperuste))
                              (= (get-in metadata [:sailytysaika :arkistointi]) :toistaiseksi)
                              (assoc :sailytysaika (dissoc (:sailytysaika metadata) :pituus))
                              (= (get-in metadata [:sailytysaika :arkistointi]) :määräajan)
                              (assoc :sailytysaika (dissoc (:sailytysaika metadata) :laskentaperuste))
                              ))
   (into [] (mapv #(collect-data lang (build-path path data) %) (:nodes data)))])

(defn- get-tree-style [data]
  (if (or (= (:001type data) :asia)
          (= (:001type data) :asiakirja)
          (= (:001type data) :liite))
    [:bold]
    [:normal]))

(defn render-index [[data children]]
  (cond-> [:paragraph
           [:anchor {:size   (if (.contains (:path data) ".") 11 14)
                     :styles (get-tree-style data)
                     :id     (render-path "i-" data)
                     :target (render-path "#" data)} (render-title data)]]
          (not (empty? children)) (into [(into [:list {:symbol "…"}] (mapv #(render-index %) children))])))

; :color [230 230 230]
(defn print-value [lang val]
  (doall
    (cond
      (keyword? val) (t val lang)
      (true? val) (t "true" lang)
      (false? val) (t "false" lang)
      (instance? Date val) (.format (SimpleDateFormat. "dd.MM.yyyy HH:mm") val)
      :else (str val))))

(defn render-row [lang [key val]]
  (if (map? val)
    [[:pdf-cell {:colspan 2 :valign :middle :set-border [:top :left :right]} [:paragraph {:style :bold} (t key lang)]]
     [:pdf-cell {:colspan 2 :align :center :valign :middle :set-border [:bottom :left :right]}
      (into [:pdf-table {:horizontal-align :center :width-percent 100} [1 1]] (mapv #(render-row lang %) val))]]
    [[:pdf-cell {:valign :middle} [:paragraph {:style :bold :indent 5} (t key lang)]]
     [:pdf-cell {:valign :middle}
      (if (= key :path)
        (render-code val)
        (print-value lang val))]]))

(defn render-data-tables [lang [data children]]
  (cond-> [:section {:numbers false}
           [:paragraph {:size 14 :styles [:bold]}
            [:anchor {
                      :id     (render-path "" data)
                      :target (render-path "#i-" data)} (render-title data)]]
           [:spacer]
           (into [:pdf-table [1 1]] (mapv #(render-row lang %) data))
           [:spacer 2]]
          (not (empty? children)) (into (mapv #(render-data-tables lang %) children))))


(defn tos-pdf-markup [published lang]

  (let [tos (:tos published)
        data (collect-data lang [] tos)
        index (render-index data)
        data-tables (render-data-tables lang data)
        ]
    [{:title  "Lupapiste.fi"
      :size   "a4"
      :footer {:align :right
               :text  (clojure.string/join " - " ["Lupapiste.fi"
                                                  (str (f/unparse sdf (l/local-now)))
                                                  (t "sivu" lang)])}
      :pages  true}


     [:chapter {:numbers false} [:heading {:style {:size 1 :color [0 0 0]}} (t "Kansisivu" lang)] " "]
     [:image {:xscale 1 :yscale 1} (ImageIO/read (io/resource "public/img/logo-v2-flat.png"))]
     [:spacer]
     [:heading {:style {:size 20}} (t "Tiedonohjaussuunnitelma" lang)]
     [:spacer]
     [:line]
     [:spacer 3]
     [:paragraph {:indent 50}
      [:pdf-table {:indent 5 :border true :cell-border false} [1 1]
       (render-row lang ["Nimi" (:name published)])
       (render-row lang ["Organisaatio" (:organization published)])
       (render-row lang ["Julkaistu" (:published published)])
       (render-row lang ["Voimassaolon alku" (:valid-from published)])
       (render-row lang ["Julkaisija" (:publisher published)])]]
     [:spacer 5]

     [:paragraph {:indent 50 :style :bold :size 14} (t "Rakenne" lang)]
     [:paragraph {:indent 30}
      [:paragraph {:indent 0}
       [:list {:symbol "•"}
        [:paragraph (t :paatehtava lang) " (" (t "tehtäväluokka" lang) ")"
         [:list {:symbol "…"} [:paragraph (t :tehtava lang) " (" (t "tehtäväluokka" lang) ")"
                               [:list {:symbol "…"} [:paragraph (t :alitehtava lang) " (" (t "tehtäväluokka" lang) ")"
                                                     [:list {:symbol "…"} [:paragraph {:style :bold} (t :asia lang) " (" (t "tehtäväluokka" lang) ")"
                                                                           [:list {:symbol "…"} [:paragraph {:style :normal} (t :toimenpide lang) " (" (t "tehtäväluokka" lang) " + " (t "tunniste" lang) ")"
                                                                                                 [:list {:symbol "…"} [:paragraph (t :toimenpide-tarkenne lang) " (" (t "tehtäväluokka" lang) " + " (t "tunniste" lang) ")"
                                                                                                                       [:list {:symbol "…"} [:paragraph {:style :bold} (t :asiakirja lang) " (" (t "tehtäväluokka" lang) " + " (t "tunniste" lang) ")"
                                                                                                                                             [:list {:symbol "…"} [:paragraph {:style :bold} (t :liite lang) " (" (t "tehtäväluokka" lang) " + " (t "tunniste" lang) ")"]]]]]]]]]]]]]]]]]]
     [:spacer 5]
     [:paragraph {:indent 50 :style :bold :size 8} (t "Ohje" lang)]
     [:list {:indent 50 :symbol "•"}
      [:paragraph {:style :normal :size 6} (t "otsikot.paksunnettu" lang)]
      [:paragraph {:style :normal :size 6} (t "pikalinkki.metadata" lang)]
      [:paragraph {:style :normal :size 6} (t "pikalinkki.index" lang)]]


     [:chapter {:numbers false} [:heading {:style {:size 14}} (t "Tiedonohjaussuunnitelma" lang)]]
     [:spacer]
     [:line]
     [:spacer 2]
     [:list {:symbol "•"} index]

     [:chapter {:numbers false} [:heading {:size 14} (t "Metatiedot" lang)]
      [:section "" [:paragraph [:line] [:spacer 2]]]
      data-tables]
     ]))

(defn markup-to-pdf [markup out]
  (pdf/pdf markup out))

(defn export-tos-pdf [published lang]
  (let [out (ByteArrayOutputStream.)
        pdf-markup (tos-pdf-markup published lang)]
    (markup-to-pdf pdf-markup out)
    (ByteArrayInputStream. (.toByteArray out))))

(defn write-tos-pdf-to-stream [published lang file]
  (with-open [out (io/output-stream file)]
    (io/copy (export-tos-pdf published lang) out)))
