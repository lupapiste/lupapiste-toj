(ns lupapiste-toj.domain-test
  (:require [lupapiste-toj.domain :as d]
            [lupapiste-toj.test-data :refer [default-metadata]]
            [clojure.test :refer :all]
            [schema.core :as s]))

(def sample-tos
  {:text "paatehtava"
   :code 0
   :type :paatehtava
   :tos-type "R"
   :attachment-types "Rakennusluvat-v2"
   :nodes
   [{:text "tehtava"
     :code 0
     :type :tehtava
     :nodes
     [{:text "alitehtava"
       :code 0
       :type :alitehtava
       :nodes
       [{:text "asia"
         :code 0
         :type :asia
         :metadata default-metadata
         :nodes
         [{:text "Vireilletulo"
           :type :toimenpide
           :nodes
           [{:text "Hakemus jätetään"
             :type :toimenpide-tarkenne
             :nodes
             [{:type :asiakirja
               :id :hakemus
               :metadata {:henkilotiedot :ei-sisalla, :julkisuusluokka :julkinen,
                          :sailytysaika {:arkistointi :ikuisesti, :laskentaperuste :rakennuksen_purkamispäivä, :perustelu "Laki", :pituus 0}, :suojaustaso :suojaustaso1,
                          :tila :luonnos, :myyntipalvelu true, :nakyvyys :julkinen, :kieli :fi}
               :nodes []}]}]}]}]}]}]})

(deftest child-codes-unique
  (is (nil? (s/check d/TOS sample-tos)))
  (let [asia {:text "asia"
              :type :asia
              :metadata default-metadata
              :nodes []}
        tos-with-duplicate-asia-code (update-in sample-tos
                                                [:nodes 0 :nodes 0 :nodes]
                                                conj
                                                (assoc asia :code 0))
        tos-with-valid-asia (update-in sample-tos
                                       [:nodes 0 :nodes 0 :nodes]
                                       conj
                                       (assoc asia :code 1))]
    (is (thrown-with-msg? Exception #"Childs have unique :code"
                          (s/validate d/TOS tos-with-duplicate-asia-code)))
    (is (nil? (s/check d/TOS tos-with-valid-asia)))))

(deftest document-ids-unique
  (is (nil? (s/check d/TOS sample-tos)))
  (let [document {:type :asiakirja
                  :metadata {:henkilotiedot :ei-sisalla, :julkisuusluokka :julkinen,
                             :sailytysaika {:arkistointi :ikuisesti, :laskentaperuste :rakennuksen_purkamispäivä, :perustelu "Laki", :pituus 0}, :suojaustaso :suojaustaso1,
                             :tila :luonnos, :myyntipalvelu true, :nakyvyys :julkinen, :kieli :fi}
                  :nodes []}
        tos-with-duplicate-doc (update-in sample-tos
                                                [:nodes 0 :nodes 0 :nodes 0 :nodes 0 :nodes 0 :nodes]
                                                conj
                                                (assoc document :id :hakemus))
        tos-with-valid-docs (update-in sample-tos
                                       [:nodes 0 :nodes 0 :nodes 0 :nodes 0 :nodes 0 :nodes]
                                       conj
                                       (assoc document :id :lausunto))]
    (is (thrown-with-msg? Exception #"Childs have unique :id, if any"
                          (s/validate d/TOS tos-with-duplicate-doc)))
    (is (nil? (s/check d/TOS tos-with-valid-docs)))))
