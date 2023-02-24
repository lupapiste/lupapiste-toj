(ns lupapiste-toj.components.years
  (:require [lupapiste-toj.state :as state]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.utils :as utils]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]))

(defn positive? [value]
  (when (pos? value)
    value))

(defn parse-years [value]
  (some->> value
           utils/parse-number
           positive?))

(defn on-change [data ks value]
  (let [years (parse-years value)]
    (state/set-field-valid ks (boolean years))
    (om/update! data ks (or years value))))

(defcomponent YearsField [data owner {:keys [editing? ks]}]
  (will-mount [_]
    (when editing?
      (state/set-field-valid ks (boolean (parse-years (get-in data ks))))))
  (will-unmount [_]
    (when editing?
      (state/set-field-valid ks true)))
  (render [_]
    (let [value (get-in data ks)
          {:keys [invalid-fields]} (om/observe owner (state/node-in-editor))
          edit-valid? (not (contains? invalid-fields ks))]
      (html
        [:div.years-field
         (if editing?
           [:input {:type "number"
                    :class (if edit-valid? "valid-edit" "invalid-edit")
                    :value (str value)
                    :on-change (fn [e] (on-change data ks (utils/event-target-value e)))}]
           [:span value])
         [:span.year-label (t "vuotta") " " (t "päätöspäivästä")]]))))

(defn years-field [editing? data ks]
  (om/build YearsField data {:opts {:editing? editing? :ks ks}}))
