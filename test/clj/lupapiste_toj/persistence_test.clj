(ns lupapiste-toj.persistence-test
  (:require [clojure.test :refer :all]
            [monger.collection :as mc]
            [lupapiste-toj.persistence :as p]
            [lupapiste-commons.shared-utils :refer [dissoc-in]]
            [lupapiste-toj.test-data :refer [default-metadata] :as test-data]
            [lupapiste-toj.test-helpers :refer [with-mongo-db mongo]]
            [lupapiste-toj.domain :as d]))

(use-fixtures :each with-mongo-db)

(deftest get-draft
  (let [{:keys [db]} @mongo]
    (testing
      "Fetching latest version creates initial version if no published plans exist
       for supported org types R, YA and YMP"
      (are [collection] (zero? (mc/count db collection))
                        "draft"
                        "published")
      (:tos (p/get-draft db "hervanta-R"))
      (is (= 1 (mc/count db "draft")))
      (is (zero? (mc/count db "published")))
      (is (p/get-draft db "123-R"))
      (is (p/get-draft db "123-YA"))
      (is (p/get-draft db "123-YMP")))
    (testing "For non supported organisation types, get-draft returns nil"
      (is (nil? (p/get-draft db "123-NONE"))))))

(deftest tos-type?
  (testing "Resolve tos types for supported types R, YA, YMP"
    (is (p/tos-type "123-R"))
    (is (p/tos-type "123-YMP"))
    (is (p/tos-type "123-YA")))
  (testing "For non-supported types, tos-type is falsy"
    (is (not (p/tos-type "123-NONE")))))

(deftest draft-is-valid
  (testing "Draft have valid information"
    (let [{:keys [db]} @mongo
          ya-draft (:tos (p/get-draft db "456-YA"))
          r-draft (:tos (p/get-draft db "456-R"))]
      (is (not (nil? ya-draft)))
      (is (not (nil? r-draft)))
      (is (= (:tos-type ya-draft) "YA"))
      (is (= (:tos-type r-draft) "R"))
      (is (= (:attachment-types ya-draft) "YleistenAlueidenLuvat-v2"))
      (is (= (:attachment-types r-draft) "Rakennusluvat-v2")))))

(deftest set-value
  (let [{:keys [db]} @mongo
        org "hervanta"
        value "foo"]
    (p/create-draft db org test-data/tos)
    (p/update-draft db
                    {:edit-type :set-value
                     :data {:key :text
                            :value value}
                     :path []}
                    org)
    (is (= value (:text (:tos (p/get-draft db org)))))))

(deftest add-node
  (let [{:keys [db]} @mongo
        new-node {:text "foo" :type :asia :code 0 :metadata default-metadata :nodes []}
        org "hervanta"
        path [:nodes 0 :nodes 0 :nodes]]
    (p/create-draft db org test-data/tos)
    (p/update-draft db
                    {:edit-type :add-node
                     :data new-node
                     :path path}
                    org)
    (is (= (update-in test-data/tos path conj new-node)
           (:tos (p/get-draft db org))))))

(deftest remove-node
  (let [{:keys [db]} @mongo
        path [:nodes 0 :nodes 0 :nodes 0 :nodes 0]
        org "hervanta"
        initial-data (:tos (p/create-draft db org test-data/tos))]
    (is (not (nil? (get-in initial-data path))))
    (p/update-draft db
                    {:edit-type :remove-node
                     :path path}
                    org)
    (is (= (dissoc-in initial-data path)
           (:tos (p/get-draft db org))))))

(deftest publish
  (let [{:keys [db]} @mongo
        published-collection "published"
        org "hervanta"]
    (p/create-draft db org test-data/tos)
    (p/publish db org "Tiedonohjaussuunnitelma 2015" (java.util.Date.) test-data/john)
    (is (= 1 (count (p/get-published db org))))
    (is (= 1 (mc/count db published-collection)))
    (is (not (nil? (:published (mc/find-one-as-map db published-collection {})))))))

(deftest currently-valid-published
  (let [{:keys [db]} @mongo
        org "hervanta"
        brian {:first-name "Brian"
               :last-name "Kottarainen"}]
    (p/create-draft db org test-data/tos)
    (p/publish db org "Tiedonohjaussuunnitelma 2011" (java.util.Date. 1300000000000) test-data/john)
    (p/publish db org "Tiedonohjaussuunnitelma 2014" (java.util.Date. 1400000000000) test-data/john)
    (Thread/sleep 10)
    (p/publish db org "Tiedonohjaussuunnitelma 2014" (java.util.Date. 1400000000000) brian)
    (p/publish db org "Tiedonohjaussuunnitelma 2027" (java.util.Date. 1800000000000) test-data/john)
    (is (apply distinct? (map :published (p/get-published db org))))
    (is (= "Tiedonohjaussuunnitelma 2014" (:name (p/get-currently-valid db org))))
    (is (= brian (:publisher (p/get-currently-valid db org))))))

(deftest composite-edits
  (let [{:keys [db]} @mongo
        org "hervanta"
        path1 []
        value1 "foo"
        path2 [:nodes 0]
        value2 "bar"
        key :text]
    (p/create-draft db org test-data/tos)
    (p/update-draft db
                    [{:edit-type :set-value
                      :data {:key key
                              :value value1}
                      :path path1}
                     {:edit-type :set-value
                      :data {:key key
                             :value value2}
                      :path path2}]
                    org)
    (is (= value1 (get-in (p/get-draft db org) (conj [:tos] key))))
    (is (= value2 (get-in (p/get-draft db org) (conj [:tos :nodes 0] key))))))

(def string-sample-tos
  {:text "paatehtava"
   :code 0
   :type "paatehtava"
   :tos-type "R"
   :attachment-types "Rakennusluvat-v2"
   :nodes [{:text "tehtava"
             :code 0
             :type "tehtava"
             :nodes [{:text "alitehtava"
                       :code 0
                       :type "alitehtava"
                       :nodes [{:text "asia"
                                 :code 0
                                 :type "asia"
                                 :metadata {:henkilotiedot "ei-sisalla", :julkisuusluokka "julkinen", :kieli "fi",
                                             :sailytysaika {:arkistointi "ikuisesti", :perustelu "Laki"}}
                                 :nodes [{:text "Vireilletulo"
                                           :type "toimenpide"
                                           :nodes [{:text "Hakemus jätetään"
                                                     :type "toimenpide-tarkenne"
                                                     :nodes [{:type "asiakirja"
                                                               :id "hakemus"
                                                               :metadata {:henkilotiedot "ei-sisalla", :julkisuusluokka "julkinen",
                                                                           :sailytysaika {:arkistointi "määräajan", :laskentaperuste "rakennuksen_purkamispäivä", :perustelu "Laki", :pituus 5}
                                                                           :tila "luonnos", :myyntipalvelu true, :nakyvyys "julkinen", :kieli "fi"}
                                                               :nodes [{:type "liite"
                                                                         :id "paapiirustus.asemapiirros"
                                                                         :metadata {:henkilotiedot "ei-sisalla", :julkisuusluokka "salainen",
                                                                                     :sailytysaika {:arkistointi "ikuisesti", :perustelu "Laki"},
                                                                                     :suojaustaso "suojaustaso1", :salassapitoaika 5, :salassapitoperuste "salassapitolaki", :turvallisuusluokka "turvallisuusluokka4"
                                                                                     :kayttajaryhma "viranomaisryhma" :kayttajaryhmakuvaus "muokkausoikeus"
                                                                                     :tila "luonnos", :myyntipalvelu true, :nakyvyys "julkinen", :kieli "fi"}}]}]}]}]}]}]}]})

(deftest coercion-to-schema-works
  (let [coerced (p/coerce-and-validate d/TOS string-sample-tos)]
    (is (= :paatehtava (:type coerced)))))
