(ns user
  (:require [reloaded.repl :refer [system init start stop go reset]]
            [lupapiste-toj.system :as toj]
            [net.cgrand.enlive-html :as html]))

(reloaded.repl/set-init! #(toj/new-system (-> (toj/read-config "config.edn")
                                              (assoc-in [:http :mode] :dev))
                                          {"build" "dev"}))
