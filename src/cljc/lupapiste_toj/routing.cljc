(ns lupapiste-toj.routing)

(def
  ^{:doc "Programmatic access to the root URL path.
          Be sure to check out URLs in resources if you want to change the root path."}
  root
  "/tiedonohjaus")

(defn path [name]
  (str root name))
