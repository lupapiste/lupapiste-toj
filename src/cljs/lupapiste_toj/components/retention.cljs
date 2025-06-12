(ns lupapiste-toj.components.retention
  (:require [lupapiste-toj.components.years :refer [years-field]]
            [lupapiste-toj.components.fields :as fields]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.utils :as utils]
            [lupapiste-toj.state :as state]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [clojure.string :as string]))

(defn retention-options [metadata {:keys [arkistointi]}]
  [:div.retention-options
   (for [key tms/arkistointi]
     (let [input-id (name key)]
       [:span
        [:input.radio {:id input-id
                       :type "radio"
                       :name "säilytysaika"
                       :value (name key)
                       :checked (if (= (or arkistointi (first tms/arkistointi)) key) true false)
                       :on-change (fn [e]
                                    (om/update! metadata [:sailytysaika :arkistointi] key)
                                    (when (= :toistaiseksi key) (om/update! metadata [:sailytysaika :laskentaperuste] :rakennuksen_purkamispäivä)))}]
        [:label {:html-for input-id} (t key)]]))])

(defcomponent RetentionPeriod [{:keys [sailytysaika] :as metadata} owner]
  (render-state [_ {:keys [editing?]}]
    (let [{:keys [laskentaperuste arkistointi]} sailytysaika]
      (html
       [:div.retention
        [:h4 (t "Säilytysaika")]
        [:h5 (t "Arkistointi")]
        (if editing?
          (retention-options metadata sailytysaika)
          [:div [:span (t arkistointi)]])
        (when (= arkistointi :määräajan)
          [:span
           [:label (t "Arkistointiaika")]
           (years-field editing? metadata [:sailytysaika :pituus])])
        (when (= :toistaiseksi arkistointi)
          [:span
           [:label (t "Päättymisen peruste")]
           (if editing?
             (fields/make-select (for [option tms/laskentaperuste] [option (t option)])
                                 laskentaperuste
                                 (partial om/update! metadata [:sailytysaika :laskentaperuste]))
             [:div (t laskentaperuste)])])
        [:label (t "Perustelu")]
        (when editing?
          [:select {:on-change (fn [e] (let [value (keyword (utils/event-target-value e))]
                                         (when-not (= :custom-justification value)
                                           (om/update! metadata [:sailytysaika :perustelu] (t value)))))}
           (map (fn [suggestion] [:option {:value suggestion} (t suggestion)]) tms/sailytysaika-perustelu-suggestions)])
        (om/build fields/RequiredTextField metadata {:opts {:editing? editing? :ks [:sailytysaika :perustelu]}})]))))

(defn retention-period [metadata editing? path]
  (om/build RetentionPeriod
            metadata
            {:state {:editing? editing?}
             :react-key (conj @path :retention)}))
