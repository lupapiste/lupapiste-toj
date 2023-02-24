(ns lupapiste-toj.system
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [lupapiste-toj.http :as http]
            [lupapiste-toj.db :as db]
            [lupapiste-toj.translations :as translations]))

(defn read-config [filename]
  (try
    (edn/read-string (slurp filename))
    (catch java.io.FileNotFoundException e
      (println (str "Could not find configuration file: " filename)))))

(defn new-system [config build-info]
  (let [system {:db (db/new-Db (:db config))
                :translations (translations/->Translations)
                :app (component/using (http/new-HttpServer (:http config) build-info)
                                      [:db :translations])}]
    (component/map->SystemMap system)))
