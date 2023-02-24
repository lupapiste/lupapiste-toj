(ns lupapiste-toj.authorization
  (:require [clojure.set :refer [intersection]]
            [lupapiste-toj.persistence :as p]))

(def authorized-roles
  #{:authority :authorityAdmin :tos-publisher :tos-editor :reader})

(def unauthorized-response
  {:status 401
   :body "Sisäänkirjautuminen vaaditaan"
   :session {:redirect-after-login "/tiedonohjaus"}})

(def forbidden-response
  {:status 403
   :body "Sinulla ei ole tarvittavia käyttöoikeuksia"})

(defn redirect-response [redirect-path]
  {:status  302
   :headers {"Location" "/app/fi/welcome#!/login"}
   :session {:redirect-after-login redirect-path}})

(defn user-is-authorized-for-org [target-org user]
  (let [org-roles (get-in user [:orgAuthz target-org])]
    (not (empty? (intersection authorized-roles org-roles)))))

(defn user-is-authorized-for-some-org [{:keys [orgAuthz]}]
  (let [roles (->> orgAuthz
                   (reduce #(concat %1 (second %2)) [])
                   (map keyword))]
    (some authorized-roles roles)))

(defn wrap-user-authorization [handler organization & [redirect-path]]
  (fn [request]
    (if-let [user (or (get-in request [:session :user])
                      (:autologin-user request))]
      (if (or (and organization (user-is-authorized-for-org (keyword organization) user))
              (and (nil? organization) (user-is-authorized-for-some-org user)))
        (let [response (handler request)]
          (assoc response :session (-> (or (:session response) (:session request))
                                       (dissoc :redirect-after-login))))
        forbidden-response)
      (if redirect-path
        (redirect-response redirect-path)
        unauthorized-response))))

(defn get-authorized-orgs [user organization-names db]
  (doall
    (->> (:orgAuthz user)
         (filter (fn [[_ roles]] (not-empty (intersection authorized-roles roles))))
         (distinct)
         (map (fn [[org roles]]
                {:id    org
                 :name  (get organization-names (keyword org) {:fi org})
                 :roles roles
                 :draft (p/get-draft db (name org))})))))

(defn wrap-role [handler org role]
  (fn [request]
    (if-let [user (or (get-in request [:session :user])
                      (:autologin-user request))]
      (if (contains? (get-in user [:orgAuthz (keyword org)]) role)
        (handler request)
        forbidden-response)
      unauthorized-response)))
