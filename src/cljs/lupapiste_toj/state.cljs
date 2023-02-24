(ns lupapiste-toj.state
  (:require [ajax.core :refer [GET POST]]
            [lupapiste-toj.routing :as routing]
            [om.core :as om :include-macros true]))

(def initial-node-in-editor
  {:path nil
   :node nil
   :parent-codes nil
   :adding-node? false
   :editing? false
   :invalid-fields #{}})

(def initial-app-state
  {:tree {}
   :visibility-tree {}
   :node-in-editor initial-node-in-editor
   :published []
   :user {}
   :error {}
   :organization {}
   :view-mode {:draft? true}
   :translations {}
   :selected-language :unset})

(defonce app-state (atom initial-app-state))

(defn build-visibility-tree [node path]
  (let [existing-visibility (get-in (:visibility-tree @app-state) (conj path :children-visible?))
        visible? (if-not (nil? existing-visibility) existing-visibility (< (count path) 5))
        visibility-node {:children-visible? visible?}]
    (if (seq (:nodes node))
      (assoc visibility-node :nodes (vec (map-indexed (fn [i child] (build-visibility-tree child (conj path :nodes i))) (:nodes node))))
      visibility-node)))

(defn update-tree [tos]
  (swap! app-state assoc :tree tos :visibility-tree (build-visibility-tree tos [])))

(defn tree []
  (om/ref-cursor (:tree (om/root-cursor app-state))))

(defn update-tree-with-cursor [tos]
  (om/transact! (om/root-cursor app-state) #(assoc % :tree tos :visibility-tree (build-visibility-tree tos []))))

(defn node-in-editor []
  (om/ref-cursor (:node-in-editor (om/root-cursor app-state))))

(defn clear-node-in-editor []
  (om/update! (node-in-editor) initial-node-in-editor))

(defn stop-editing []
  (om/update! (node-in-editor) [:editing?] false))

(defn published []
  (om/ref-cursor (:published (om/root-cursor app-state))))

(defn user []
  (om/ref-cursor (:user (om/root-cursor app-state))))

(defn error []
  (om/ref-cursor (:error (om/root-cursor app-state))))

(defn organization []
  (om/ref-cursor (:organization (om/root-cursor app-state))))

(defn visibility-tree []
  (om/ref-cursor (:visibility-tree (om/root-cursor app-state))))

(defn current-language []
  (om/ref-cursor (:selected-language (om/root-cursor app-state))))

(defn on-error [{:keys [status response]}]
  (om/update! (error) {:status status
                       :text response}))

(defn set-field-valid [field valid?]
  (let [f (if valid? disj conj)]
    (swap! app-state update-in [:node-in-editor :invalid-fields] f field)))

(defn field-valid? [field]
  (not (contains? (get-in @app-state [:node-in-editor :invalid-fields]) field)))

(defn route-with-organization [route]
  (let [organization (get-in @app-state [:organization :id])]
    (routing/path (str "/org/" (name organization) route))))

(defn get-tree []
  (get @app-state :tree))

(defn view-mode []
  (om/ref-cursor (:view-mode (om/root-cursor app-state))))

(defn set-draft-mode [draft-mode?]
  (om/update! (view-mode) {:draft? draft-mode?}))

(defn translations []
  (:translations @app-state))

(defn send-edit [edit]
  (POST (route-with-organization "/draft")
      :params edit
      :handler update-tree
      :error-handler on-error))

(defn set-value [path key value]
  {:edit-type :set-value
   :path path
   :data {:key key
          :value value}})

(defn add-node [path data]
  {:edit-type :add-node
   :data data
   :path path})

(defn remove-node [path]
  {:edit-type :remove-node
   :path path})

(defn relocate-node [path new-path]
  {:edit-type :move-node
   :path path
   :data {:new-path new-path}})

(defn language-header []
  {"Accept-Language" (name (:selected-language @app-state))})

(defn nodes-with-updated-order [tree path new-index]
  (let [node (get-in tree path)
        siblings-path (vec (butlast path))
        siblings (remove #(= % node) (get-in tree siblings-path))
        [siblings-before siblings-after] (split-at new-index siblings)]
    (vec (concat siblings-before [node] siblings-after))))

(defn move-node [path up?]
  (let [{:keys [tree node-in-editor visibility-tree]} @app-state
        current-index (last path)
        siblings-path (vec (butlast path))
        parent-path (vec (butlast siblings-path))
        new-index (if up? (max 0 (dec current-index))
                          (min (dec (count (get-in tree siblings-path))) (inc current-index)))]
    (when (not= new-index current-index)
      (let [updated-nodes (nodes-with-updated-order tree path new-index)
            new-tree (assoc-in tree siblings-path updated-nodes)
            new-vt (assoc-in visibility-tree siblings-path (nodes-with-updated-order visibility-tree path new-index))
            new-nie (assoc node-in-editor :path (conj siblings-path new-index))]
        (swap! app-state assoc :tree new-tree :node-in-editor new-nie :visibility-tree new-vt)
        (send-edit (set-value parent-path :nodes updated-nodes))))))

(defn publish [name valid-from]
  (POST (route-with-organization "/publish")
      :headers (language-header)
      :params {:name name :valid-from valid-from}
      :handler (fn [e] (om/update! (published) (vec e)))
      :error-handler on-error))

(defn get-draft [& [cb]]
  (GET (route-with-organization "/draft")
      :headers (language-header)
      :handler #(do (update-tree %)
                    (set-draft-mode true)
                    (when cb
                      (cb)))
      :error-handler on-error
      :headers {"X-Requested-With" "XMLHttpRequest"}))

(defn get-published [& [cb]]
  (GET (route-with-organization "/published")
      :headers (language-header)
      :handler #(do (om/update! (published) (vec %))
                    (when cb
                      (cb)))
      :error-handler on-error
      :headers {"X-Requested-With" "XMLHttpRequest"}))

(defn fetch-translations [lang]
  (GET (routing/path (str "/i18n/" (name lang)))
      :headers (language-header)
      :handler #(swap! app-state assoc :translations % :selected-language lang)
      :error-handler on-error
      :headers {"X-Requested-With" "XMLHttpRequest"}))

(defn set-org-if-empty [organizations]
  (let [previous-org (:organization @app-state)]
    (when (or (empty? previous-org) (empty? (filter #(= % previous-org) organizations)))
      (om/update! (organization) (first organizations)))))

(defn get-user [lang]
  (GET (routing/path "/user")
       :headers (language-header)
       :handler #(do (om/update! (user) %)
                     (set-org-if-empty (:organizations %))
                     (get-draft)
                     (get-published)
                     (fetch-translations (or lang :fi)))
       :error-handler on-error
       :headers {"X-Requested-With" "XMLHttpRequest"}))

(defn selected-language [query]
  (#{:fi :sv :en} (keyword (first (keys query)))))
