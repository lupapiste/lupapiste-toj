(ns utils.cljs-warning-handler
  (:require [clojure.java.shell :refer [sh]]
            [cljs.analyzer :as analyzer]))

(defn handle [warning-type env extra]
  (when-let [s (analyzer/error-message warning-type extra)]
    (binding [*out* *err*]
      (let [message (analyzer/message env s)]
        (println message)
        (sh "./notify.sh" message)))))
