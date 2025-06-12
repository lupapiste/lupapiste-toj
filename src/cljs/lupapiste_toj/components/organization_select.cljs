(ns lupapiste-toj.components.organization-select
  (:require [lupapiste-toj.state :as state]
            [lupapiste-toj.utils :as utils]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]))

(defcomponent OrganizationSelect [_ owner]
  (render [_]
    (let [user (om/observe owner (state/user))
          current-organization (om/observe owner (state/organization))
          organizations (:organizations user)]
      (html
        (if (> (count organizations) 1)
          (into
            [:select {:on-change (fn [e]
                                   (let [id           (keyword (utils/event-target-value e))
                                         selected-org (first (filter #(= (:id %) id) organizations))]
                                     (when-not (= current-organization selected-org)
                                       (state/clear-node-in-editor)
                                       (om/update! current-organization selected-org)
                                       (state/get-draft)
                                       (state/get-published))))
                      :value     (name (:id current-organization))}]
            (mapv (fn [org] [:option {:value (:id org)} (get-in org [:name :fi])]) organizations))
          [:div.single-organization (get-in (first organizations) [:name :fi])])))))
