(ns karma.macros
  (:require [cljs.analyzer.api :as ana-api]))

(defmacro ns-metas [re]
  (mapv (fn [ns]
          `(ns-interns (quote ~(symbol ns))))
        (filter #(re-matches re (str %))
                (ana-api/all-ns))))
