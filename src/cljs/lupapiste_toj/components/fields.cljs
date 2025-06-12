(ns lupapiste-toj.components.fields
  (:require [clojure.string :as s]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.state :as state]
            [lupapiste-toj.utils :as utils]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]))

(defn ->string [x]
  (if (keyword? x) (name x) x))

(defn grouped-keyword? [kw]
  (and (keyword? kw)
       (= 2 (count (s/split (name kw) #"\.")))))

(defn parts [dotted-kw]
  (-> dotted-kw name (s/split #"\.")))

(defn group [dotted-kw]
  (keyword (first (parts dotted-kw))))

(defn value [dotted-kw]
  (keyword (second (parts dotted-kw))))

(defn grouped-options? [options]
  (let [[group-name group] (first options)
        [option-key option-text] (first group)]
    (and (string? group-name)
         (keyword? option-key)
         (string? option-text))))

(defn make-option-nodes [options]
  (if (grouped-options? options)
    (for [[group-text options] options]
      [:optgroup {:label group-text}
       (for [[option text] options]
         [:option {:value (->string option)} text])])
    (for [[option text] options]
      [:option {:value (->string option)} text])))

(defn make-select [options selection on-change-fn]
  {:pre [(and (if selection (keyword? selection) true)
              (if (grouped-options? options)
                (and (every? string? (map first options))
                     (every? #(and (keyword? (first %))
                                   (string? (second %)))
                             (mapcat second options)))
                (and (every? keyword? (map first options))
                     (every? string? (map second options)))))]}
  (into
    [:select {:value     (or (->string selection) "")
              :on-change (fn [e] (-> e
                                     utils/event-target-value
                                     keyword
                                     on-change-fn))}]
    (make-option-nodes options)))

(defcomponent NameField [data owner {:keys [edit-key]}]
  (render[_]
    (let [valid? (state/field-valid? edit-key)]
      (html
        [:input.node-name {:type        "text"
                           :value       (get data edit-key "")
                           :placeholder (t "Nimi")
                           :auto-focus   true
                           :on-change   (fn [e]
                                          (let [value (utils/event-target-value e)]
                                            (om/update! data [edit-key] value)
                                            (state/set-field-valid edit-key (not (s/blank? value)))))
                           :class       (if valid? "valid-edit" "invalid-edit")}]))))

(defn name-field [data key]
  (let [value (get data key)
        valid? (not (s/blank? value))]
    (state/set-field-valid key valid?))
  (om/build NameField data {:opts {:edit-key key}}))

(defcomponent RequiredTextField [data owner {:keys [editing? ks]}]
  (will-mount [_]
    (when editing?
      (state/set-field-valid ks (not (s/blank? (get-in data ks))))))
  (will-unmount [_]
    (when editing?
      (state/set-field-valid ks true)))
  (render [_]
    (let [value (get-in data ks)
          {:keys [invalid-fields]} (om/observe owner (state/node-in-editor))
          edit-valid? (not (contains? invalid-fields ks))]
      (state/set-field-valid ks (not (s/blank? value)))
      (html
        (if editing?
          [:input {:type        "text"
                   :value       (or value "")
                   :class       (if edit-valid? "valid-edit" "invalid-edit")
                   :on-change   (fn [e] (let [new-value (utils/event-target-value e)]
                                          (state/set-field-valid ks (not (s/blank? new-value)))
                                          (om/update! data ks new-value)))
                   :placeholder (t "Pakollinen tieto")}]
          [:div (if (s/blank? value) (t "<Arvo puuttuu>") value)])))))
