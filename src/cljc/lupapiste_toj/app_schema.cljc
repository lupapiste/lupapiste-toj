(ns lupapiste-toj.app-schema
  (:require #?(:cljs [schema.core :as s :include-macros true]
               :clj [schema.core :as s])))

(def SessionUser
  {:role s/Str
   :email s/Str
   :username s/Str
   :firstName s/Str
   :lastName s/Str
   :orgAuthz {s/Keyword #{s/Keyword}}
   :organizations [s/Str]
   :expires s/Int
   :id s/Str})

(def Organizations [{:id s/Keyword
                     :name {s/Keyword s/Str}
                     :roles #{s/Keyword}}])

(def User
  (-> SessionUser
      (assoc :organizations Organizations)
      (dissoc :role)
      (dissoc :orgAuthz)))
