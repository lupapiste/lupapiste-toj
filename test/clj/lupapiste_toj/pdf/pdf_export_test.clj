(ns lupapiste-toj.pdf.pdf-export-test
  (:require [clojure.test :refer :all]
            [lupapiste-toj.i18n :refer [t]]
            [monger.collection :as mc]
            [lupapiste-toj.persistence :as p]
            [lupapiste-commons.shared-utils :refer [dissoc-in]]
            [lupapiste-toj.test-data :refer [default-metadata] :as test-data]
            [lupapiste-toj.test-helpers :refer [with-mongo-db mongo]]
            [taoensso.timbre :as timbre :refer [trace tracef debug debugf info infof warn warnf error errorf fatal fatalf]]
            [lupapiste-toj.pdf.pdf-export :as pdf])
  (:import (java.io File FileOutputStream ByteArrayOutputStream ByteArrayInputStream)
           (java.util Date)))

(use-fixtures :each with-mongo-db)

(def sample-markup [{:000title "Maankäyttö, Rakentaminen ja Asuminen",
                     :path     "",
                     :type     :paatehtava}
                    [[{:000title "Rakentaminen, ylläpito ja käyttö",
                       :path     "10",
                       :type     :tehtava}
                      [[{:000title "Rakennusvalvonta", :path "10.3", :type :alitehtava}
                        [[{:000title        "Rakennuslupamenettely",
                           :henkilotiedot   :ei-sisalla,
                           :julkisuusluokka :julkinen,
                           :path            "10.3.0",
                           :sailytysaika
                                            {:arkistointi     :ei,
                                             :perustelu       "perustelu",
                                             :laskentaperuste :lupapäätöspäivä,
                                             :pituus          0},
                           :type            :asia}
                          [[{:000title "Neuvonta", :path "10.3.0.1", :type :toimenpide}
                            [[{:000title "Asiakkaan yhteydenotto asiointipalvelussa",
                               :path     "10.3.0.1.31",
                               :type     :toimenpide-tarkenne}
                              [[{:000title        "Ilmoitus",
                                 :henkilotiedot   :ei-sisalla,
                                 :julkisuusluokka :julkinen,
                                 :myyntipalvelu   false,
                                 :nakyvyys        :julkinen,
                                 :path            "10.3.0.1.31.46",
                                 :sailytysaika
                                                  {:arkistointi     :ei,
                                                   :perustelu       "perustelu",
                                                   :laskentaperuste :lupapäätöspäivä,
                                                   :pituus          0},
                                 :tila            :luonnos,
                                 :type            :asiakirja}
                                nil]]]]]
                           [{:000title "Käsittelyssä",
                             :path     "10.3.0.1",
                             :type     :toimenpide}
                            [[{:000title "Hakemus jätetään käsittelyyn",
                               :path     "10.3.0.1.32",
                               :type     :toimenpide-tarkenne}
                              [[{:000title        "Hakemus",
                                 :henkilotiedot   :ei-sisalla,
                                 :julkisuusluokka :julkinen,
                                 :myyntipalvelu   false,
                                 :nakyvyys        :julkinen,
                                 :path            "10.3.0.1.32.47",
                                 :sailytysaika
                                                  {:arkistointi     :ei,
                                                   :perustelu       "perustelu",
                                                   :laskentaperuste :lupapäätöspäivä,
                                                   :pituus          0},
                                 :tila            :luonnos,
                                 :type            :asiakirja}
                                nil]
                               [{:000title        "Muu liite",
                                 :henkilotiedot   :ei-sisalla,
                                 :julkisuusluokka :julkinen,
                                 :myyntipalvelu   false,
                                 :nakyvyys        :julkinen,
                                 :path            "10.3.0.1.32.48",
                                 :sailytysaika
                                                  {:arkistointi     :ei,
                                                   :perustelu       "perustelu",
                                                   :laskentaperuste :lupapäätöspäivä,
                                                   :pituus          0},
                                 :tila            :luonnos,
                                 :type            :asiakirja}
                                nil]]]]]]]]]]]]])

(defn setup []
  (lupapiste-toj.i18n/load-translations)
  (let [{:keys [db]} @mongo
        org "hervanta"
        brian {:first-name "Brian"
               :last-name  "Kottarainen"}]
    (p/create-draft db org test-data/tos)
    (let [data (first (p/publish db org "Tiedonohjaussuunnitelma 2011" (Date. 1300000000000) test-data/john))]
      (Thread/sleep 10)
      data
      )))


(defn render-tree-list [[data children]]
  (cond-> [:paragraph (:000title data)]
          (not (empty? children)) (into [(into [:list] (mapv #(render-tree-list %) children))]))
  )


(deftest tos-pdf-markup-fi
  (let [published (setup)
        pdf-markup (pdf/tos-pdf-markup published :fi)]
    ;;(debug "published: \n" (with-out-str (clojure.pprint/pprint published)))
    ;(debug "pdf-markup: \n" (with-out-str (clojure.pprint/pprint pdf-markup)))
    ))

(deftest export-pdf-file
  (let [published (setup)
        file (File/createTempFile (str "test-export-pdf-file-fi-") ".pdf")
        fis (FileOutputStream. file)]
    (debug "wrote pdf to : " (.getAbsolutePath file))
    (pdf/write-tos-pdf-to-stream published :fi fis)))

(def datax
  ["Maankäyttö, Rakentaminen ja Asuminen"
   [["Rakentaminen, ylläpito ja käyttö"
     [["Rakennusvalvonta"
       [["Rakennuslupamenettely"
         [["Neuvonta"
           [["Asiakkaan yhteydenotto asiointipalvelussa"
             [["Ilmoitus"
               []]]]]]
          ["Käsittelyssä"
           [["Hakemus jätetään käsittelyyn"
             [["Hakemus"
               []]
              ["Muu liite"
               []]]]]]]]]]]]]])
