(ns lupapiste-toj.test-data
  (:require [lupapiste-commons.tos-metadata-schema :as tms]))

(def john {:first-name "John" :last-name "Doe"})

(def default-metadata
  (assoc-in tms/default-metadata [:sailytysaika :perustelu] "perustelu"))

(def asiakirja-default-metadata
  (assoc-in tms/asiakirja-default-metadata [:sailytysaika :perustelu] "perustelu"))

(def tos
  {:text "Maankäyttö, Rakentaminen ja Asuminen"
   :type :paatehtava
   :tos-type "R"
   :attachment-types "Rakennusluvat-v2"
   :code 10
   :nodes
   [{:text "Rakentaminen, ylläpito ja käyttö"
     :type :tehtava
     :code 3
     :nodes
     [{:text "Rakennusvalvonta"
       :type :alitehtava
       :code 0
       :nodes
       [{:text "Rakennuslupamenettely"
         :type :asia
         :code 1
         :metadata default-metadata
         :nodes
         [{:text "Neuvonta"
           :type :toimenpide
           :application-state :draft
           :nodes
           [{:text "Asiakkaan yhteydenotto asiointipalvelussa"
             :type :toimenpide-tarkenne
             :nodes
             [{:type :asiakirja
               :id :ilmoitus
               :metadata asiakirja-default-metadata
               :nodes []}]}]}
          {:text "Käsittelyssä"
           :type :toimenpide
           :application-state :submitted
           :nodes
           [{:text "Hakemus jätetään käsittelyyn"
             :type :toimenpide-tarkenne
             :application-state :sent
             :nodes
             [{:type :asiakirja
               :id :hakemus
               :metadata asiakirja-default-metadata
               :nodes []}
              {:type :asiakirja
               :id :muut.muu
               :metadata asiakirja-default-metadata
               :nodes []}]}]}]}]}]}]})
