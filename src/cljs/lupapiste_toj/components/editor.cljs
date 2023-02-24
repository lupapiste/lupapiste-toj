(ns lupapiste-toj.components.editor
  (:require [lupapiste-toj.components.years :refer [years-field]]
            [lupapiste-toj.components.retention :refer [retention-period]]
            [lupapiste-toj.components.function-classification :refer [FunctionClassification]]
            [lupapiste-toj.components.node :refer [node-title group-title]]
            [lupapiste-toj.components.fields :as fields]
            [lupapiste-toj.domain :as d]
            [lupapiste-commons.tos-metadata-schema :as tms]
            [lupapiste-toj.i18n :refer [t]]
            [lupapiste-toj.state :as state]
            [lupapiste-toj.utils :as utils]
            [lupapiste-commons.states :as states]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [clojure.string :as string]
            [schema.core :as s]))

(defn handle-change [metadata ks value]
  (om/update! metadata ks value))

(defn current-value [metadata attribute]
  (get metadata (:type attribute)))

(defn generic-text-input [editing? attribute metadata ks value]
  (if editing?
    [:input {:type        "text"
             :value       (or value "")
             :on-change   #(handle-change metadata ks (utils/event-target-value %))
             :placeholder (t (:type attribute))}]
    [:div (if (string/blank? value) (t "<Arvo puuttuu>") value)]))

(defn generic-bool-input [editing? metadata ks checked?]
  (if editing?
    [:input {:type        "checkbox"
             :checked      checked?
             :on-change   #(handle-change metadata ks (not checked?))}]
    [:div (if checked? (t "yes") (t "no"))]))

(defn edit-field [editing? attribute metadata ks value]
  (condp = (:schema attribute)
    tms/Vuodet (years-field editing? metadata ks)
    tms/NonEmptyStr (om/build fields/RequiredTextField metadata {:opts {:editing? editing? :ks ks}})
    s/Bool (generic-bool-input editing? metadata ks value)
    (generic-text-input editing? attribute metadata ks value)))

(defn select-field [editing? metadata ks selection values]
  (if editing?
    (do
      (when-not selection
        (om/update! metadata ks (first values)))
      (fields/make-select (for [v values] [v (t v)])
                          selection
                          (partial om/update! metadata ks)))
    [:div (t (or selection "<Arvo puuttuu>"))]))

(defn render-attribute
  [{:keys [calculated type values dependencies] :as attribute}
   editing?
   {{:keys [metadata]} :node :keys [path] :as node-in-editor}]

  (condp = attribute
    :function-classification [(om/build FunctionClassification node-in-editor {})]

    tms/SailytysAika [(retention-period metadata editing? path)]

    (when-not calculated
      (let [ks [type]
            value (current-value metadata attribute)]
        (concat [[:div {:id (name type)} [:label (t type)]
                  [:br]
                  (if values
                    (select-field editing? metadata ks value values)
                    (edit-field editing? attribute metadata ks value))]]
                (when-let [deps (get dependencies (or value (first values)))]
                  (mapcat #(render-attribute % editing? node-in-editor) deps)))))))

(defn toggle-edit-state [node-in-editor path]
  (-> node-in-editor
      (update-in [:editing?] not)
      (assoc :node (get-in (state/get-tree) path))
      (assoc :invalid-fields #{})))

(def state-presentation-order-map
  (into {} (map-indexed (fn [idx k] [k idx])
                        [:info :answered :draft :open :submitted :sent :complementNeeded
                         :agreementPrepared :agreementSigned :finished
                         :canceled :verdictGiven :appealed :constructionStarted
                         :inUse :closed :onHold :extinct :foremanVerdictGiven :acknowledged
                         :hearing :final :proposal :proposalApproved])))

(defn full-states-in-presentation-order [tos-type]
  (case tos-type
    "YA" (sort-by state-presentation-order-map (keys states/full-ya-application-state-graph))
    "YMP" (sort-by state-presentation-order-map (keys states/default-application-state-graph))
    (sort-by state-presentation-order-map (keys states/r-and-tj-transitions))))

(defn state-selector [node editing?]
  (when (#{:toimenpide :toimenpide-tarkenne} (:type node))
    [:div
     [:label (t "Asian tila Lupapisteessä")]
     (if editing?
       [:select {:value (name (or (:application-state node) :ei-valittu))
                 :on-change (fn [e] (let [value (keyword (utils/event-target-value e))
                                          value (if (= :ei-valittu value) nil value)]
                                      (if (= :ei-valittu value)
                                        (om/transact! node #(dissoc % :application-state))
                                        (om/update! node :application-state value))))}
        (map (fn [state] [:option {:value (name state)} (t state)]) (conj (full-states-in-presentation-order (:tos-type (state/get-tree))) :ei-valittu))]
       [:div (t (or (:application-state node) :ei-valittu))])]))

(defn submit [node path]
  (state/send-edit (cond-> []
                     (:metadata node) (conj (state/set-value path :metadata (tms/sanitize-metadata (:metadata node))))
                     (:text node)     (conj (state/set-value path :text (:text node)))
                     (:code node)     (conj (state/set-value path :code (:code node)))
                     (:application-state node) (conj (state/set-value path :application-state (:application-state node)))
                     (nil? (:application-state node)) (conj (state/remove-node (conj path :application-state)))))
  (state/stop-editing))

(defcomponent Editor [_ owner]
  (render [_]
    (let [{:keys [path node invalid-fields editing?] :as node-in-editor} (om/observe owner (state/node-in-editor))
          editor-hidden? (nil? path)
          edit-valid? (empty? invalid-fields)
          draft-mode? (:draft? (om/observe owner (state/view-mode)))
          edit-allowed? (contains? (:roles (om/observe owner (state/organization))) :tos-editor)]
      (html
       (if editor-hidden?
         [:div {:style {:display "none"}}]
         [:div.editor
          [:div.editor-header
           [:div
            (if (and editing? (not (d/selectable? (:type node))))
              (fields/name-field node :text)
              [:div
               [:span.group-title (group-title node)]
               [:span.node-title (node-title node)]])]
           (when (and (d/editable? (:type node)) draft-mode? edit-allowed?)
             (let [tree (om/observe owner (state/tree))
                   sibling-count (count (get-in tree (vec (butlast path))))]
               [:div.edit-button-container
                [:i.button.icon-up-open-big {:title (t "Siirrä ylöspäin tiedonohjaussuunnitelmassa")
                                             :class (when (= (last path) 0) "disabled")
                                             :on-click #(state/move-node @path true)}]
                [:i.button.icon-down-open-big {:title (t "Siirrä alaspäin tiedonohjaussuunnitelmassa")
                                               :class (when (= (last path) (dec sibling-count)) "disabled")
                                               :on-click #(state/move-node @path false)}]
                [:i {:class    (if editing? "button icon-cancel-circled" "button icon-pencil")
                     :title    (if editing? (t "cancel") (t "edit"))
                     :on-click (fn [_] (om/transact! node-in-editor #(toggle-edit-state % @path)))}]]))]
          (let [metadata (d/metadata-for-type (:type node))]
            (into
              [:div.editor-form]
              (concat
                (mapcat #(render-attribute % editing? node-in-editor) metadata)
                [(state-selector node editing?)]
                (when editing?
                  [[:div.editor-buttons.cf
                    [:button.save.btn.btn-primary {:type     "submit"
                                                   :disabled (not edit-valid?)
                                                   :on-click (fn [_] (submit @node @path))}
                     (t "Tallenna")]
                    [:button.delete.btn.btn-danger {:type     "button"
                                                    :on-click (fn [_]
                                                                (when (or (empty? (:nodes node))
                                                                          (.confirm js/window (-> (t "Haluatko varmasti poistaa kohteen {nimi} ja kaikki sen alatasot?")
                                                                                                  (.replace "{nimi}" (node-title node)))))
                                                                  (state/send-edit (state/remove-node @path))
                                                                  (state/clear-node-in-editor)))}
                     (t "Poista")]]]))))])))))

(defn to-option-groups [keywords]
  (reduce (fn [acc [k v]]
            (cond-> acc
                    (not-any? #{k} (:ordered-keys acc)) (assoc :ordered-keys (conj (:ordered-keys acc) k))
                    true (assoc-in [:option-groups k] (conj (get-in acc [:option-groups k] []) v))))
          {:ordered-keys [] :option-groups {}}
          (for [kw keywords]
            (if (fields/grouped-keyword? kw)
              [(fields/group kw) kw]
              [:asiakirjat kw]))))

(defn add-texts [{:keys [ordered-keys option-groups]}]
  (for [group-key ordered-keys]
    [(t group-key)
     (for [option (get option-groups group-key)]
       [option (t option)])]))

(defn node-type-selection [{:keys [id-options id] :as node}]
  (let [options (add-texts (to-option-groups id-options))]
    (fields/make-select options id (fn [selection]
                                     (om/transact! node #(assoc % :id selection))))))

(defn add-node [path node]
  (let [sanitized-node (cond-> node
                         true (dissoc :id-options)
                         (:metadata node) (assoc :metadata (tms/sanitize-metadata (:metadata node))))]
    (state/send-edit (state/add-node (vec (butlast path)) sanitized-node))
    (state/clear-node-in-editor)))

(defn cloning-options [nodes]
  (->> (map-indexed (fn [i node] [:option {:value i} (:text node)]) nodes)
       (cons [:option {:value "no-to-cloning"} (t "Ei kopioida")])))

(defn remove-existing-docs-from-children [form existing-ids]
  (if-let [children (:nodes form)]
    (let [new-children (reduce (fn [acc child]
                                 (let [updated-child (remove-existing-docs-from-children child existing-ids)]
                                   (if (contains? existing-ids (:id updated-child))
                                     ; Move grandchildren up
                                     (if (:nodes updated-child)
                                       (let [updated-granchildren (if (= :asiakirja (:type updated-child))
                                                                    (map #(assoc % :type :asiakirja :nodes []) (:nodes updated-child))
                                                                    (:nodes updated-child))]
                                         (concat acc updated-granchildren))
                                       acc)
                                     (conj acc updated-child))))
                         [] children)]
      (assoc form :nodes new-children))
    form))

(defn remove-existing-docs [tree path clone]
  (let [parent (get-in tree (drop-last 2 path))
        existing-ids (into #{} (remove nil? (map :id (tree-seq :nodes :nodes parent))))]
    (remove-existing-docs-from-children clone existing-ids)))

(defcomponent NewNodeEditor [_ owner]
  (render [_]
    (let [{:keys [node path invalid-fields] :as node-in-editor} (om/observe owner (state/node-in-editor))
          tree (om/observe owner (state/tree))
          type (:type node)
          edit-valid? (empty? invalid-fields)]
      (html
       [:div.editor
        [:div.editor-header
         [:div
          (if (d/selectable? type)
            (node-type-selection node)
            (fields/name-field node :text))]
         [:div.edit-button-container
          [:i {:class "button icon-cancel-circled"
               :on-click (fn [e]
                           (state/clear-node-in-editor)
                           (state/get-draft))}]]]
        (when (= :toimenpide type)
          [:div.cloning
           [:h5 (t "Kopioi uuden toimenpiteen sisältö toisen tehtävän toimenpiteestä")]
           [:p (t "Asiakirjat, jotka on jo määritelty tämän tehtävän alla, jätetään kopioimatta.")]
           (let [cloneable-nodes (d/cloneable-nodes tree)
                 parent (get-in tree (drop-last 2 path))]
             [:select {:on-change (fn [e]
                                    (let [selected (utils/event-target-value e)]
                                      (if (= "no-to-cloning" selected)
                                        (om/update! node :nodes [])
                                        (let [idxs (map #(js/parseInt %) (string/split selected #","))
                                              selected-node (get-in cloneable-nodes [(first idxs) :nodes (second idxs)])
                                              clone-with-valid-docs (remove-existing-docs tree path @selected-node)]
                                          (om/update! node :nodes (:nodes clone-with-valid-docs))))))}
              (->> (map-indexed
                     (fn [i group]
                       (when-not (or (= group parent) (empty? (:nodes group)))
                         [:optgroup {:label (:text group)}
                          (map-indexed (fn [j child] [:option {:value [i j]} (:text child)]) (:nodes group))]))
                     cloneable-nodes)
                   (remove nil?)
                   (cons [:option {:value "no-to-cloning"} (t "Ei kopioida")]))])])
        (when (d/children-cloneable? type)
          [:div.cloning
           [:h5 (t "Kopioi toimenpiteet toisesta tehtävästä")]
           (let [cloneable-nodes (d/cloneable-nodes tree)]
             [:select {:on-change (fn [e]
                                    (let [selected (utils/event-target-value e)]
                                      (if (= "no-to-cloning" selected)
                                        (om/update! node :nodes [])
                                        (let [selected-node (nth cloneable-nodes (js/parseInt selected))]
                                          (om/update! node :nodes (:nodes @selected-node))))))}
              (cloning-options cloneable-nodes)])])
        [:div.editor-form
         (map #(render-attribute % :editing? node-in-editor) (d/metadata-for-type type))
         (state-selector node :editing?)
         [:div.editor-buttons.cf
          [:button.save.btn.btn-primary {:type     "submit"
                                         :disabled (not edit-valid?)
                                         :on-click (fn [e] (add-node @path @node))}
           (t "Tallenna")]]]]))))
