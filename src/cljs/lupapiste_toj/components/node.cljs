(ns lupapiste-toj.components.node
  (:require [cljs.reader :as reader]
            [clojure.string :as string]
            [lupapiste-toj.components.fields :as fields]
            [lupapiste-toj.domain :as d]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.records :as records]
            [lupapiste-toj.state :as state]
            [om-tools.core :refer-macros [defcomponent]]
            [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))

(defn new-child-node [child-type parent path]
  (let [tree (state/get-tree)
        options (d/child-options tree (select-keys parent [:type :id :nodes]) path)
        mandatory-attrs {:type child-type}]
    (cond-> mandatory-attrs
      (d/has-field? child-type :text)     (assoc :text "")
      (d/has-field? child-type :id)       (assoc :id (first options)
                                                 :id-options options)
      (d/has-field? child-type :metadata) (assoc :metadata (d/default-metadata-for-type child-type))
      (d/has-field? child-type :nodes)    (assoc :nodes [])
      (d/has-field? child-type :code)     (assoc :code (d/next-code tree child-type)))))

(def type-icons
  {:liite "icon-attach"
   :asiakirja "icon-newspaper"
   :toimenpide "icon-hourglass"
   :toimenpide-tarkenne "icon-cog"
   :asia "icon-folder"})

(defn in-editor? [path node-in-editor]
  (let [edit-path (:path node-in-editor)]
    (= edit-path path)))

(defn show-in-editor [editor node path parent-codes]
  (om/transact! editor #(assoc %
                               :path path
                               :node node
                               :parent-codes parent-codes
                               :editing? false
                               :adding-node? false)))

(defn node-title [node]
  (-> (if-let [id (:id node)]
        id
        (:text node))
      t))

(defn group-title [{:keys [id]}]
  (when (fields/grouped-keyword? id)
    (t (fields/group id))))

(defn new-parent-codes [parent-codes node]
  (if-let [code (:code node)]
    (conj parent-codes code)
    parent-codes))

(defn r1-inside-r2? [r1 r2]
  (and (>= (.-top r1) (.-top r2)) (<= (.-bottom r1) (.-bottom r2))))

(defn scroll-element-into-view [owner]
  (let [node (om.core/get-node owner)
        container (.getElementById js/document "tree-container")
        container-rect (.getBoundingClientRect container)
        node-rect (.getBoundingClientRect node)]
    (when-not (r1-inside-r2? node-rect container-rect)
      (.scrollIntoView node))))

(def type-of-draggable (atom nil))

(defn build-drag-and-drop-attribute-map [node node-type path edit-disabled?]
  (if (or edit-disabled?)
    {:draggable false}
    (cond-> {:draggable (contains? #{:asiakirja :liite} node-type)
             :on-drag-over (fn [e]
                             (let [liite? (= :liite @type-of-draggable)
                                   asiakirja-type-target? (contains? (set records/records) (:id node))]
                               (when (or (= :toimenpide-tarkenne node-type) (and asiakirja-type-target? liite?))
                                 (set! (-> e .-dataTransfer .-dropEffect) "copy")
                                 (.preventDefault e))))
             :on-drop (fn [e]
                        (let [path-of-dragged (-> e .-dataTransfer (.getData "text") (reader/read-string))]
                          (when (contains? #{:asiakirja :liite} @type-of-draggable)
                            (state/send-edit (state/relocate-node path-of-dragged (conj path :nodes)))
                            (state/clear-node-in-editor))
                          (reset! type-of-draggable nil)
                          (.preventDefault e)))}
            (contains? #{:asiakirja :liite} node-type) (assoc :on-drag-start (fn [e] (let [data-transfer (.-dataTransfer e)
                                                                                           asiakirja-type? (contains? (set records/records) (:id node))]
                                                                                       (reset! type-of-draggable (if asiakirja-type? :asiakirja :liite))
                                                                                       (.setData data-transfer "text" (str path))))))))

(defcomponent Node [node owner]
  (display-name [_]
    (name (or (:type node) "Node")))
  (render-state [_ {:keys [path parent-codes can-add-child? really-cant-add-child?]}]
    (let [{:keys [editing?] :as node-in-editor} (om/observe owner (state/node-in-editor))
          leaf-class (str "leaf"
                          (when (in-editor? path node-in-editor) " active")
                          (when (seq (:nodes node)) " with-children"))
          type (:type node)
          visibility-cursor (get-in (om/observe owner (state/visibility-tree)) path)
          children-visible? (:children-visible? visibility-cursor)]
      (html
        [:div.node
         (if (empty? path)
           [:h3 (string/upper-case (node-title node))]
           [:div (merge {:class leaf-class} (build-drag-and-drop-attribute-map node type path really-cant-add-child?))
            (when (seq (:nodes node))
              [:i {:class (if children-visible? "icon-down-dir tree-expand" "icon-right-dir tree-expand")
                   :on-click (fn [_]
                               (om/transact! visibility-cursor #(assoc % :children-visible? (not children-visible?)))
                               (when-not children-visible?
                                 (js/setTimeout #(scroll-element-into-view owner) 200)))}])
            [:div.leaf-text {:on-click (fn [_]
                                         (when-not editing?
                                           (do (show-in-editor node-in-editor node path parent-codes)
                                               (om/transact! visibility-cursor #(assoc % :children-visible? true))
                                               (js/setTimeout #(scroll-element-into-view owner) 200))))}
             [:span
              (when-let [icon-class (get type-icons type)]
                [:i {:class icon-class}])
              (node-title node)]]
            (when (and can-add-child? (not really-cant-add-child?) (or children-visible? (empty? (:nodes node))))
              (let [child-type (d/type-of-child type)]
                [:i.button.icon-plus {:on-click (fn [_]
                                                  (let [new-child (new-child-node child-type node path)]
                                                    (om/transact! visibility-cursor #(assoc % :children-visible? true))
                                                    (om/transact! node [:nodes] #(conj % (assoc new-child :text (t (str "Uusi " (name child-type))))))
                                                    (om/transact! node-in-editor #(assoc %
                                                                                   :path (conj path :nodes (dec (count (:nodes @node))))
                                                                                   :node new-child
                                                                                   :parent-codes (new-parent-codes parent-codes node)
                                                                                   :adding-node? true
                                                                                   :editing? true))))}]))
            [:div.arrow-right]])
         (when children-visible?
           [:div.children
            (map-indexed
              (fn [i child]
                (om/build Node child {:state {:path (conj path :nodes i)
                                              :parent-codes (new-parent-codes parent-codes node)
                                              ;; FIXME maybe this global disable should be accessed through state ns directly
                                              :really-cant-add-child? really-cant-add-child?
                                              :can-add-child? (d/can-add-child? (state/get-tree) child path)}}))
              (:nodes node))])]))))
