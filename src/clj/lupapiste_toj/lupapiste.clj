(ns lupapiste-toj.lupapiste
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre])
  (:import [java.util.concurrent TimeUnit]
           [com.fasterxml.jackson.core JsonParseException]
           [java.util Date]))

(defonce organization-name-cache (atom {:names nil :fetch-timestamp nil}))
(def cache-duration (.toMillis TimeUnit/MINUTES 30))

(defn now [] (-> (Date.) .getTime))

(defn- fetch-organization-names [lupapiste-host api-key]
  (try (let [{:keys [ok names text]} (-> @(http/get (str lupapiste-host "/api/query/get-organization-names")
                                                    {:oauth-token api-key
                                                     :timeout 5000})
                                         :body
                                         (json/parse-string true))]
         (if ok
           names
           (timbre/error (str "Could not fetch organization names: " text))))
       (catch JsonParseException e
         (timbre/error e "Failed to parse response for get-organization-names"))))

(defn get-organization-names [lupapiste-host api-key]
  (let [{:keys [names fetch-timestamp]} @organization-name-cache
        now (now)]
    (if (or (not fetch-timestamp) (> (- now fetch-timestamp) cache-duration))
      (if-let [new-names (fetch-organization-names lupapiste-host api-key)]
        (:names (reset! organization-name-cache {:names new-names
                                                 :fetch-timestamp now}))
        [])
      names)))
