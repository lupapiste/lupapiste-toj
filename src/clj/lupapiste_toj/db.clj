(ns lupapiste-toj.db
  (:require [com.stuartsierra.component :as component]
            [lupapiste-toj.migrations.core :as migration]
            [monger.core :as mg]
            [schema.core :as s :refer [defrecord]]
            [taoensso.timbre :as timbre]
            [monger.collection :as mc])
  (:refer-clojure :exclude [defrecord])
  (:import [com.mongodb WriteConcern]
           [java.util.logging Logger Level]))

(def Config {:uri s/Str})

(defn silence-mongo-logging []
  (.setLevel (Logger/getLogger "org.mongodb") Level/WARNING))

(defrecord Db [config :- Config]
  component/Lifecycle
  (start [this]
    (silence-mongo-logging)
    (if (:mongo this)
      this
      (let [{:keys [db] :as mongo} (mg/connect-via-uri (:uri config))
            component (assoc this :mongo mongo)]
        (mg/set-default-write-concern! WriteConcern/MAJORITY)
        (timbre/info "Connected to mongoDB")
        (migration/run-migrations db)
        (mc/ensure-index db :draft {:organization 1})
        (mc/ensure-index db :published {:organization 1})
        (mc/ensure-index db :published {:organization 1
                                        :valid-from -1
                                        :published -1})
        component)))
  (stop [this]
    (when-let [mongo (:mongo this)]
      (mg/disconnect (get-in this [:mongo :conn]))
      (let [component (-> this
                          (dissoc :mongo)
                          (dissoc :conn))]
        (timbre/info "Disconnected from mongoDB")
        component))
    this))

(s/defn ^:always-validate new-Db :- s/Any
  [config :- Config]
  (map->Db {:config config}))
