(ns lupapiste-toj.app
  (:require [lupapiste-toj.state :as state]
            [lupapiste-toj.components.publish :refer [Publisher]]
            [lupapiste-toj.components.node :refer [Node]]
            [lupapiste-toj.components.editor :refer [Editor NewNodeEditor]]
            [lupapiste-toj.components.header :refer [Header]]
            [lupapiste-toj.components.error-message :refer [ErrorMessage]]
            [lupapiste-toj.domain :as d]
            [lupapiste-toj.utils :as utils]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [cemerick.url :as url]))

(defcomponent Main [app-state owner]
  (render [_]
    (let [editing? (get-in app-state [:node-in-editor :editing?])
          tree (:tree app-state)
          current-lang (:selected-language app-state)
          draft-mode? (get-in app-state [:view-mode :draft?])
          edit-allowed? (contains? (get-in app-state [:organization :roles]) :tos-editor)]
      (html
        [:div
         (om/build ErrorMessage {})
         (om/build Header current-lang {})
         (om/build Publisher current-lang {})
         (if (or (empty? tree) (empty? (:translations app-state)))
           [:div.main-app
            [:div.loading.cf
             [:span.icon-spin6.animate-spin]]]
           [:div.main-app
            [:div#tree-container {:class (if editing? "tree-container editing" "tree-container")}
             (om/build Node tree {:state {:path []
                                          :parent-codes []
                                          :really-cant-add-child? (or editing? (not draft-mode?) (not edit-allowed?))
                                          :can-add-child? (d/can-add-child? tree tree [])}})]
            [:div.editor-container (if (:adding-node? (state/node-in-editor))
                                     (om/build NewNodeEditor {})
                                     (om/build Editor {}))]])])))
  (will-mount [_]
    (state/get-user (state/selected-language (:query (url/url (-> js/window .-location .-href)))))))

(defn ^:export main []
  (om/root Main
           state/app-state
           {:target (. js/document (getElementById "app"))}))

(utils/setup-print-to-console!)
(utils/setup-error-logging)