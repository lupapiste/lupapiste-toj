(ns lupapiste-toj.routes
  (:require [clojure.set :refer [rename-keys]]
            [compojure.core :refer [routes GET POST context wrap-routes]]
            [compojure.route :as route]
            [lupapiste-toj.app-schema :as a]
            [lupapiste-toj.authorization :as authorization]
            [lupapiste-toj.i18n :as i18n]
            [lupapiste-toj.lupapiste :as lupis]
            [lupapiste-toj.persistence :as p]
            [lupapiste-toj.routing :as routing]
            [lupapiste-commons.ring.autologin :as auto]
            [lupapiste-commons.ring.session :as rs]
            [lupapiste-commons.ring.session-timeout :as st]
            [lupapiste-toj.tos-api :as tos-api]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-body]]
            [ring.util.response :refer [response resource-response redirect status content-type header]]
            [schema.core :as s]
            [taoensso.timbre :as timbre]
            [lupapiste-commons.ring.utils :as ru]
            [lupapiste-commons.route-utils :as route-utils]
            [clojure.string :as str])
  (:import [java.util Date]))

(defn publisher [{:keys [session autologin-user]}]
  (-> (or (:user session) autologin-user)
      (select-keys  [:firstName :lastName])
      (rename-keys {:firstName :first-name
                    :lastName :last-name})))

(defn persistence-routes [org {:keys [db lupapiste-host lupapiste-api-key]}]
  (-> (-> (routes (GET "/draft" request
                    (response (:tos (p/get-draft db org))))
                  (-> (POST "/draft" {body :body}
                        (try
                          (response (p/update-draft db body org))
                          (catch Throwable e
                            (timbre/error e (str "Error occurred when persisting the draft for " org " with modification " body))
                            {:status 400
                             :body "Tiedonohjaussuunnitelmaa ei voitu tallentaa virhetilanteen vuoksi."})))
                      (wrap-routes authorization/wrap-role org :tos-editor))
                  (GET "/published" []
                    (response (p/get-published db org)))
                  (GET "/download/:lang/:version" [lang version :as request]
                    (let [org-names (lupis/get-organization-names lupapiste-host lupapiste-api-key)
                          org-name (get-in org-names [(keyword org) (keyword lang)])]
                      (tos-api/get-tos-pdf db org org-name (keyword lang) version)))
                  (-> (POST "/publish" {{:keys [name valid-from]} :body :as request}
                        (if (or (str/blank? name)
                                (not (instance? Date valid-from)))
                          {:status 400
                           :body "Missing name or valid-from"}
                          (response (p/publish db org name valid-from (publisher request)))))
                      (wrap-routes authorization/wrap-role org :tos-publisher)))
          (authorization/wrap-user-authorization org))
      (wrap-routes wrap-transit-response)
      (wrap-routes wrap-transit-body)))

(s/defn user-data :- a/User
  [user :- a/SessionUser organizations :- a/Organizations]
  (-> (assoc user :organizations organizations)
      (dissoc :orgAuthz)
      (dissoc :role)))

(defn sort-organizations [organizations]
  (->> organizations
       (map
         (fn [organization]
           (let [draft-date (get-in organization [:draft :modified])]
             {:date draft-date :organization (dissoc organization :draft)})))
       ;; If there's no draft (date), then this organization is not supported and we don't need it in the ui
       (remove #(nil? (:date %)))
       (sort-by :date)
       (reverse)
       (map :organization)))

(defn user-routes [{:keys [db lupapiste-host lupapiste-api-key]}]
  (-> (GET "/user" request
        (if-let [user (or (get-in request [:session :user])
                          (:autologin-user request))]
          (let [organization-names (lupis/get-organization-names lupapiste-host lupapiste-api-key)
                ;; This has a side effect of creating a draft for all supported authorized organizations
                auth-orgs (authorization/get-authorized-orgs user organization-names db)]
            (if-not (empty? auth-orgs)
              (response (user-data user (sort-organizations auth-orgs)))
              authorization/forbidden-response))
          authorization/unauthorized-response))
      (wrap-routes wrap-transit-response)))

(defn external-api-routes [db org]
  (-> (routes (GET "/asiat" []
                (tos-api/get-asiat db org))
              (GET "/asiat/:code/document/:id" [code id]
                (tos-api/get-metadata-for-document-type db org code id))
              (GET "/asiat/:code" [code]
                (tos-api/get-metadata-for-function db org code))
              (GET "/asiat/:code/toimenpide-for-state/:state" [code state]
                (tos-api/get-toimenpide-name-for-state db org code state))
              (GET "/asiat/:code/classification" [code]
                (tos-api/get-partial-tos-xml db org code)))
      (wrap-routes wrap-json-response)))

(defn index-route [build-info]
  (-> (GET "/" []
        (route-utils/process-index-response build-info))
      (wrap-routes authorization/wrap-user-authorization nil "/tiedonohjaus")))

(def index-redirect (GET "/" [] (redirect routing/root)))

(defn status-route [build-info]
  (-> (GET "/status" [] (response build-info))
      (wrap-routes wrap-json-response)))

(defn log-js-error [{:keys [msg url linenumber] :as body}]
  (timbre/warn (str "JS error '" msg "', url: '" url "', linenumber: " linenumber))
  "logged")

(def js-error-route
  (-> (POST "/jserror" {body :body} (response (log-js-error body)))
      (wrap-routes wrap-transit-body)
      (wrap-routes wrap-transit-response)))

(defn resource-routes [mode]
  (apply routes (cond-> [(route/resources "/" {:root "/META-INF/resources"})
                         (route/resources "/" {:mime-types {"js" "text/javascript; charset=utf-8"}})]
                  (= mode :dev) (conj (route/files "/" {:root "target/cljs/dev"})))))

(defn rekey-route [store]
  (POST "/rekey" request
    (if (#{"127.0.0.1" "0:0:0:0:0:0:0:1"} (:remote-addr request))
      (do
        (rs/rekey store)
        (response "rekeyd"))
      (status (response "Unauthorized") 401))))

(def i18n-route
  (-> (GET "/i18n/:lang" [lang]
        (response (get @i18n/translations (keyword lang) {})))
      (wrap-routes wrap-transit-response)))

(defn create-routes [{:keys [mode db build-info session-key-path autologin-check-url] :as config}]
  (let [store (rs/rekeyable session-key-path)]
    (-> (routes index-redirect
                (rekey-route store)
                (context routing/root []
                  (index-route build-info)
                  (status-route build-info)
                  (resource-routes mode)
                  (user-routes config)
                  js-error-route
                  i18n-route
                  (context "/org/:org" [org]
                    (persistence-routes org config))
                  (context "/api/org/:org" [org]
                    (external-api-routes db org))))
        (ru/optional-middleware ru/wrap-request-logging (= mode :prod))
        (auto/wrap-sso-autologin autologin-check-url)
        st/wrap-session-timeout
        (wrap-session {:store store})
        ru/wrap-exception-logging
        ru/wrap-no-ajax-cache)))
