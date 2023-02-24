(ns lupapiste-toj.app
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [lupapiste-toj.system :as system]
            [lupapiste-commons.utils :refer [get-build-info]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [lupapiste-toj.logging :as logging]))

(def app-system (atom nil))

(defn -main [& args]
  (when-let [config (system/read-config (or (first args) "config.edn"))]
    (logging/configure-logging! (:logging config))
    (timbre/info "Starting app")
    (try
      (let [build-info (get-build-info "lupapiste-toj.jar")]
        (doseq [[k v] build-info]
          (timbre/info (str k ": " v)))
        (let [system (component/start (system/new-system config build-info))]
          (reset! app-system system)
          (.addShutdownHook (Runtime/getRuntime) (java.lang.Thread. #(component/stop system)))))
      (catch Throwable t
        (let [message "Error while starting application"]
          (timbre/error t message)
          (println (str message ": " t)))
        (System/exit 1)))))
