(ns lupapiste-toj.components.publish
  (:require [cljs-time.coerce :as coerce]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [clojure.string :as string]
            [lupapiste-toj.components.organization-select :refer [OrganizationSelect]]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.state :as state]
            [lupapiste-toj.utils :as utils]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [lupapiste-toj.routing :as routing]))

(def formatter (fmt/formatter "d.M.yyyy"))

(defn parse-date [text]
  (some-> (try (fmt/parse-local formatter text) (catch js/Error e nil))
          (#(when (not (time/after? (coerce/to-date-time (time/today)) %))
              %))
          coerce/to-date))

(def initial-state
  (let [today (time/today)]
    {:name ""
     :date (coerce/to-date today)
     :date-str (fmt/unparse-local-date formatter today)
     :loading false
     :selected-tos "draft"}))

(defn publish-controls [owner name date date-str]
  (let [not-valid? (or (nil? date) (string/blank? name))
        draft-mode? (:draft? (om/observe owner (state/view-mode)))
        publish-allowed? (contains? (:roles (om/observe owner (state/organization))) :tos-publisher)]
    (when (and publish-allowed? draft-mode?)
      [:div.right
       [:h3 (t "Julkaise uusi versio")]
       [:div
        [:span (t "Nimi:")]
        [:input {:type "text"
                 :value name
                 :on-change #(om/set-state! owner [:name] (utils/event-target-value %))}]
        [:span (t "Voimassa alkaen:")]
        [:input {:type "text"
                 :value date-str
                 :placeholder "PP.KK.VV"
                 :class (if-not (nil? date) "valid-edit" "invalid-edit")
                 :on-change (fn [e]
                              (let [value (utils/event-target-value e)]
                                (om/update-state! owner #(assoc % :date-str value :date (parse-date value)))))}]
        [:button.btn-primary {:disabled not-valid?
                              :on-click (fn [_]
                                          (state/publish name date)
                                          (om/set-state! owner initial-state))}
         (t "Julkaise")]]])))

(defn parse-local-date-str [date-obj]
  (str (.getDate date-obj) "." (inc (.getMonth date-obj)) "." (.getFullYear date-obj)))

(defn make-option-text [{:keys [name valid-from]}]
  (if valid-from
    (-> (t "{suunnitelman_nimi}, voimassa {pvm} alkaen")
        (.replace "{suunnitelman_nimi}" name)
        (.replace "{pvm}" (parse-local-date-str valid-from)))
    name))

(defcomponent Publisher [current-lang owner]
  (init-state [_]
    initial-state)
  (render-state [_ {:keys [name date date-str selected-tos]}]
    (let [items (cons {:name (t "Luonnos") :_id "draft"} (om/observe owner (state/published)))
          loading (empty? (om/observe owner (state/user)))
          organization (om/observe owner (state/organization))]
      (html [:div.publisher.cf
             (if loading
               [:div.loading
                [:span.icon-spin6.animate-spin {:title (t "Ladataan...")}]]
               [:div.publisher-wrapper.cf
                [:div.left
                 [:h3 (t "Versio")]
                 [:select {:on-change (fn [e]
                                        (let [selected (utils/event-target-value e)]
                                          (if (= "draft" selected)
                                            (state/get-draft)
                                            (do (state/update-tree-with-cursor (:tos (first (filter #(= selected (:_id %)) items))))
                                                (state/set-draft-mode false)
                                                (state/clear-node-in-editor)))
                                          (om/update-state! owner #(assoc % :selected-tos selected))))}
                  (map (fn [item] [:option {:value (:_id item)} (make-option-text item)]) items)]]
                [:div.left
                 [:h3 (t "Organisaatio")]
                 (om/build OrganizationSelect {})]
                [:div.left.download-button
                 [:a {:href (routing/path (str "/org/" (clojure.core/name (:id organization)) "/download/" (clojure.core/name current-lang) "/" selected-tos))}
                  [:i.lupicon-download {:title (t "Lataa tiedonohjaussuunnitelma PDF-tiedostona")}]
                  [:span (t "Lataa PDF")]]]
                (publish-controls owner name date date-str)])]))))
