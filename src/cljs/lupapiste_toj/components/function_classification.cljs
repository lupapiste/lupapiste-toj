(ns lupapiste-toj.components.function-classification
  (:require [clojure.string :as string]
            [lupapiste-toj.domain :as d]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.state :as state]
            [lupapiste-toj.utils :as utils]
            [om-tools.core :refer-macros [defcomponent]]
            [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))

(defn format-function-classification [codes]
  (string/join " " (map #(str (when (< % 10) "0") %) codes)))

(defn available? [taken-codes num]
  (when-not (contains? taken-codes num)
    num))

(defn parse-code [taken-codes value]
  (some->> value
           utils/parse-number
           (available? taken-codes)))

(defn on-change [node taken-codes value]
  (let [code (parse-code taken-codes value)]
    (state/set-field-valid :code (boolean code))
    (om/update! node [:code] (or code value))))

(defcomponent FunctionClassification [{:keys [node path invalid-fields editing? parent-codes]} owner]
  (render [_]
    (let [code (:code node)
          original-code (:code (get-in (state/get-tree) path))
          taken-codes (disj (set (map :code (d/nodes-of-type (:type node) (state/get-tree))))
                            original-code)
          edit-valid? (not (contains? invalid-fields :code))]
      (html
       [:div
        [:label (t "Tehtäväluokka")]
        (if (not code)
          [:div (format-function-classification parent-codes)]
          (if editing?
            [:div
             (format-function-classification parent-codes)
             [:input.code-input {:value (str code)
                                 :class (if edit-valid? "valid-edit" "invalid-edit")
                                 :on-change #(on-change node
                                                        taken-codes
                                                        (utils/event-target-value %))}]]
            [:div (format-function-classification (if code (conj parent-codes code) parent-codes))]))]))))
