(ns lupapiste-toj.tos-api-test
  (:require [clojure.test :refer :all]
            [lupapiste-toj.persistence :as p]
            [lupapiste-toj.tos-api :as tos-api]
            [lupapiste-toj.test-data :as test-data]
            [lupapiste-toj.test-helpers :refer [with-mongo-db mongo]]))

(use-fixtures :each with-mongo-db)

(deftest get-asia-names
  (testing "All defined :asia type node names for organization are listed"
    (let [{:keys [db]} @mongo
          new-node {:text "Ilmoitusmenettely" :type :asia :code 2 :metadata test-data/default-metadata :nodes []}
          org "hervanta"
          path [:nodes 0 :nodes 0 :nodes]]
      (p/create-draft db org test-data/tos)
      (p/update-draft db
                      {:edit-type :add-node
                       :data new-node
                       :path path}
                      org)
      (p/publish db org "foobar" (java.util.Date. 1400000000000) test-data/john)
      (is (=  {:status 200,
               :headers {},
               :body '({:code "10 03 00 01",
                        :name "Rakennuslupamenettely"}
                       {:code "10 03 00 02",
                        :name "Ilmoitusmenettely"})}
             (tos-api/get-asiat db org))))))

(deftest get-metadata-for-document
  (testing "Metadata can be retrieved for a document type"
    (let [{:keys [db]} @mongo
          org "hervanta"]
      (p/create-draft db org test-data/tos)
      (p/publish db org "foobar" (java.util.Date. 1400000000000) test-data/john)
      (is (= {:status 200,
              :headers {},
              :body test-data/asiakirja-default-metadata}
             (tos-api/get-metadata-for-document-type db org "10 03 00 01" "hakemus"))))))

(deftest get-metadata-for-function
  (testing "Metadata can be retrieved for a function (asia) type"
    (let [{:keys [db]} @mongo
          org "hervanta"]
      (p/create-draft db org test-data/tos)
      (p/publish db org "foobar" (java.util.Date. 1400000000000) test-data/john)
      (is (= {:status 200,
              :headers {},
              :body test-data/default-metadata}
            (tos-api/get-metadata-for-function db org "10 03 00 01"))))))

(deftest get-metadata-for-document
  (testing "Metadata can be retrieved for a valid document type from muu document and not retrieved for invalid type"
    (let [{:keys [db]} @mongo
          org "hervanta"]
      (p/create-draft db org test-data/tos)
      (p/publish db org "foobar" (java.util.Date. 1400000000000) test-data/john)
      (is (= {:status 200,
              :headers {},
              :body test-data/asiakirja-default-metadata}
             (tos-api/get-metadata-for-document-type db org "10 03 00 01" "rakennuspaikan_hallinta.rasitesopimus")))
      (is (= {:status 404,
              :headers {},
              :body {:error "Asiakirjaa ei löytynyt."}}
             (tos-api/get-metadata-for-document-type db org "10 03 00 01" "ihan_tyhmä_asiakirja"))))))

(deftest get-toimenpide-name-for-application-state
  (testing "Related toimenpide name can be retrieved for a given application state"
    (let [{:keys [db]} @mongo
          org "hervanta"]
      (p/create-draft db org test-data/tos)
      (p/publish db org "foobar" (java.util.Date. 1400000000000) test-data/john)
      (is (= {:status 200,
              :headers {},
              :body {:name "Käsittelyssä"}}
            (tos-api/get-toimenpide-name-for-state db org "10 03 00 01" "submitted")))
      (is (= {:status 200,
              :headers {},
              :body {:name "Käsittelyssä / Hakemus jätetään käsittelyyn"}}
             (tos-api/get-toimenpide-name-for-state db org "10 03 00 01" "sent"))))))

(deftest get-tos-classification-scheme-xml
  (testing "TOS classification can be retrieved as an XML response"
    (let [{:keys [db]} @mongo
          org "hervanta"]
      (p/create-draft db org test-data/tos)
      (p/publish db org "foobar" (java.util.Date. 1400000000000) test-data/john)
      (is (= {:status 200
              :headers {"Content-Type" "text/xml"}
              :body "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a:ClassificationScheme xmlns:a=\"http://www.lupapiste.fi/onkalo/schemas/sahke2-case-file/2016/6/1\"><a:MainFunction><a:FunctionCode>10</a:FunctionCode><a:Title>Maankäyttö, Rakentaminen ja Asuminen</a:Title><a:FunctionClassification><a:FunctionCode>10 03</a:FunctionCode><a:Title>Rakentaminen, ylläpito ja käyttö</a:Title><a:SubFunction><a:FunctionCode>10 03 00</a:FunctionCode><a:Title>Rakennusvalvonta</a:Title><a:SubFunction><a:FunctionCode>10 03 00 01</a:FunctionCode><a:Title>Rakennuslupamenettely</a:Title></a:SubFunction></a:SubFunction></a:FunctionClassification></a:MainFunction></a:ClassificationScheme>"}
             (tos-api/get-partial-tos-xml db org "10 03 00 01"))))))
