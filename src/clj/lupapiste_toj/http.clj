(ns lupapiste-toj.http
 (:require [com.stuartsierra.component :as component]
           [schema.core :as s :refer [defrecord]]
           [org.httpkit.server :refer [run-server]]
           [lupapiste-toj.routes :as routes]
           [lupapiste-toj.db]
           [taoensso.timbre :as timbre])
 (:refer-clojure :exclude [defrecord])
 (:import [lupapiste_toj.db Db]))

(def Config {:port s/Int
             :mode (s/enum :dev :prod)
             :session-key-path s/Str
             :lupapiste-host s/Str
             :autologin-check-url s/Str
             :lupapiste-api-key s/Str
             :thread s/Int})

(defn start-http-server [component {:keys [port mode session-key-path] :as config} db build-info]
  (let [stop-fn (run-server (routes/create-routes (assoc config
                                                         :db db
                                                         :build-info build-info))
                            {:port port})]
    (timbre/info (str "HTTP server started on port " port " with mode " mode))
    (assoc component :server-stop stop-fn)))

(defn stop-http-server [component server-stop]
  (server-stop)
  (timbre/info "HTTP server stopped")
  (dissoc component :server-stop))

(defrecord HttpServer [config :- Config
                       build-info :- {s/Str s/Str}
                       db :- Db]
  component/Lifecycle
  (start [this]
    (if (:server-stop this)
      this
      (do
        (start-http-server this config (get-in db [:mongo :db]) build-info))))
  (stop [this]
    (when-let [server-stop (:server-stop this)]
      (stop-http-server this server-stop))
    this))

(s/defn ^:always-validate new-HttpServer :- s/Any
  [config :- Config
   build-info :- {s/Str s/Str}]
  (map->HttpServer {:config config
                    :build-info build-info}))
