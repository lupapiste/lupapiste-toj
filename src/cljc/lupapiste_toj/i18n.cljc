(ns lupapiste-toj.i18n
  (:require #?(:cljs [lupapiste-toj.state :as state])
            #?@(:clj [[lupapiste-commons.i18n.core :refer [merge-translations read-translations keys-by-language]]
                      [clojure.java.io :as io]])))

#?(:clj (do
(def translations (atom {}))

(defn load-translations []
  (reset! translations (keys-by-language (merge-translations (read-translations (io/resource "shared_translations.txt"))
                                                             (read-translations (io/resource "translations.txt"))))))))

(defn find-string [key data]
  (let [str-key (if (keyword? key) (name key) key)]
    (if-let [result (some #(get data %) [str-key
                                         (str "attachmentType." str-key)
                                         (str "attachmentType." str-key "._group_label")])]
      result
      str-key)))

(defn t
  ([k]
   (t k :fi))
  ([k lang]
   #?(:cljs (find-string k (state/translations))
      :clj (find-string k (lang @translations)))))
