(ns lupapiste-toj.records
  (:require [clojure.string :as s]
            [lupapiste-commons.attachment-types :as attachment-types]))

(def records [:hakemus :ilmoitus :neuvontapyyntö])

(defn grouped? [values]
  (let [[first-value second-value] (take 2 values)]
    (and (keyword? first-value) (sequential? second-value))))

(defn groups->dotted-keywords [groups]
  (reduce (fn [acc [group values]]
            (reduce (fn [acc value]
                      (conj acc (->> [group value]
                                     (map name)
                                     (s/join ".")
                                     keyword)))
                    acc values))
          [] (partition 2 groups)))

(def all-record-types-r (concat records (groups->dotted-keywords attachment-types/Rakennusluvat-v2)))

(def all-record-types-ya (concat records (groups->dotted-keywords attachment-types/YleistenAlueidenLuvat-v2)))

(def all-record-types-ymp (concat records (groups->dotted-keywords attachment-types/Ymparisto-types)))

(def all-record-types-including-old-set (set (concat all-record-types-r
                                                     all-record-types-ya
                                                     all-record-types-ymp
                                                     (groups->dotted-keywords attachment-types/Rakennusluvat))))

(defn all-record-types [attachment-type]
  (case attachment-type
    "Rakennusluvat-v2" all-record-types-r
    "YleistenAlueidenLuvat-v2" all-record-types-ya
    "Ymparisto-types" all-record-types-ymp))

(defn allowed-values [parent-type attachment-type]
  (case parent-type
    :toimenpide-tarkenne (all-record-types attachment-type)
    :asiakirja (all-record-types attachment-type)
    nil))
