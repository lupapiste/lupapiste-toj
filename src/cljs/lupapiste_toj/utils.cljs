(ns lupapiste-toj.utils
  (:require [ajax.core :refer [POST]]
            [lupapiste-toj.routing :as routing]))

(enable-console-print!)

(defn on-error [{:keys [status status-text]}]
  (println (str "Oops: " status " " status-text)))

(defn event-target-value [e]
  (.. e -target -value))

(defn parse-base-10 [x]
  (js/parseInt x 10))

(defn parse-number [x]
  (if (number? x)
    x
    (some->>  x
              (re-find #"^\d+$")
              parse-base-10)))

(defn now []
  (.getTime (js/Date.)))

(defonce last-error-timestamp (atom (now)))

(defn report-to-server [msg url linenumber]
  (let [timestamp (now)
        delta (- timestamp @last-error-timestamp)]
    (when (> delta 1000)
      (reset! last-error-timestamp timestamp)
      (POST (routing/path "/jserror")
          :params {:msg msg :url url :linenumber linenumber}
          :error-handler #(println "Oops when reporting oops to server:" %)))))

(defonce error-logging-added (atom false))

(defn setup-error-logging []
  (when-not @error-logging-added
    (println "Adding JS error logging")
    (if-let [orig-on-error (.-onerror js/window)]
      (set! (.-onerror js/window)
            (fn [msg url linenumber]
              (orig-on-error msg  url linenumber)
              (report-to-server msg  url linenumber)))
      (set! (.-onerror js/window) report-to-server))
    (reset! error-logging-added true)))

(defn is-old-ie? []
  (let [agent (.-userAgent js/navigator)
        version (second (re-find #".*MSIE (\d)\.0.*" agent))]
    (and version (< version 10))))

(defn setup-print-to-console!
  "Set *print-fn* to console.log"
  []
  (set! *print-newline* false)
  (set! *print-length* 42)
  (if (is-old-ie?)
    (do (set! *print-fn*
          (fn [& args]
            (doseq [item args]
              (.log js/console item))))
        (set! *print-err-fn*
          (fn [& args]
            (doseq [item args]
              (.error js/console item)))))
    (do (set! *print-fn*
          (fn [& args]
            (.apply (.-log js/console) js/console (into-array args))))
        (set! *print-err-fn*
          (fn [& args]
            (.apply (.-error js/console) js/console (into-array args))))))
  nil)
