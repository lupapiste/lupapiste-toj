(ns ^:figwheel-load lupapiste-toj.app-test
  (:require [hipo.core :as hipo]
            [dommy.core :as dommy :refer-macros [sel1]]
            [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]
            [om-tools.core :refer-macros [defcomponent]]
            [lupapiste-toj.app :as app]
            [lupapiste-toj.components.node :refer [Node]]
            [lupapiste-toj.state :as state]
            [cljs.test :as test :refer-macros [deftest is use-fixtures]]))

(enable-console-print!)

(def root-id "test-app")

(defn setup [id]
  (.appendChild js/document.body (hipo/create [:div {:id id}])))

(defn teardown [id previous-state]
  (let [test-app (. js/document (getElementById id))]
    (om/detach-root test-app)
    (.removeChild (.-parentElement test-app) test-app)
    (reset! state/app-state previous-state)))

(defn with-root [f]
  (let [previous-state @state/app-state]
    (setup "test-app")
    (f)
    (teardown "test-app" previous-state)))

(deftest tree-component
  (let [tree {:text "Eläminen"
              :type :paatehtava
              :code "00"
              :nodes
              [{:text "oppiminen"
                :type :tehtava
                :code "01"
                :nodes
                [{:text "työnteko"
                  :type :alitehtava
                  :code "02"
                  :nodes
                  [{:text "hauskanpito"
                    :type :asia
                    :code "03"
                    :metadata {}
                    :nodes []}]}]}]}]
    (state/update-tree tree)
    (om/root Node
             tree
             {:target (. js/document (getElementById root-id))
              :state {:path []
                      :parent-codes []
                      :can-add-child? true}}))

    (is (= "ELÄMINEN" (dommy/text (sel1 [:body :div#test-app :div.node :h3]))))
    (is (= "oppiminen" (dommy/text (sel1 [:body :div#test-app :div.node :div.children :div.node :div.leaf :div.leaf-text]))))
    (is (= "työnteko" (dommy/text (sel1 [:body :div#test-app :div.node :div.children :div.node :div.children :div.node :div.leaf :div.leaf-text]))))
    (is (= "hauskanpito" (dommy/text (sel1 [:body :div#test-app :div.node :div.children :div.node :div.children :div.node :div.children :div.node :div.leaf :div.leaf-text])))))

(use-fixtures :each with-root)
