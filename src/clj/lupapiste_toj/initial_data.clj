(ns lupapiste-toj.initial-data)

(def tos
  {:code 10,
   :nodes [{:code 3,
            :nodes [{:code 0,
                     :nodes [{:code 1,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [{:nodes [{:nodes [],
                                                :text "Asiakkaan yhteydenotto asiointipalvelussa",
                                                :type :toimenpide-tarkenne}],
                                       :text "Neuvonta, ohjaus",
                                       :type :toimenpide}
                                      {:nodes [{:nodes [],
                                                :text "Omistus- ja hallintaoikeuden selvittäminen",
                                                :type :toimenpide-tarkenne}
                                               {:nodes [],
                                                :text "Kartta-aineiston toimittaminen",
                                                :type :toimenpide-tarkenne}],
                                       :text "Valmisteilla",
                                       :type :toimenpide}
                                      {:nodes [{:nodes [{:id :hakemus,
                                                         :metadata {:henkilotiedot :ei-sisalla,
                                                                    :julkisuusluokka :julkinen,
                                                                    :sailytysaika {:arkistointi :ikuisesti,
                                                                                   :perustelu "Laki"},
                                                                    :kieli :fi,
                                                                    :tila :luonnos,
                                                                    :myyntipalvelu true,
                                                                    :nakyvyys :julkinen},
                                                         :nodes [{:id :paapiirustus.asemapiirros,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :ikuisesti,
                                                                                            :perustelu "Laki"},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}
                                                                 {:id :paapiirustus.pohjapiirros,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :ikuisesti,
                                                                                            :perustelu "Laki"},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}
                                                                 {:id :paapiirustus.leikkauspiirros,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :ikuisesti,
                                                                                            :perustelu "Laki"},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}
                                                                 {:id :paapiirustus.julkisivupiirros,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :ikuisesti,
                                                                                            :perustelu "Laki"},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}
                                                                 {:id :muut.kerrosalaselvitys,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :toistaiseksi,
                                                                                            :laskentaperuste :rakennuksen_purkamispäivä,
                                                                                            :perustelu "Laki"},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}
                                                                 {:id :ennakkoluvat_ja_lausunnot.selvitys_naapurien_kuulemisesta,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :toistaiseksi,
                                                                                            :laskentaperuste :rakennuksen_purkamispäivä,
                                                                                            :perustelu "Kuntaliiton ohje 14 A, s.29"},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}],
                                                         :type :asiakirja}],
                                                :text "Hakemus jätetään",
                                                :type :toimenpide-tarkenne}],
                                       :text "Vireilletulo",
                                       :type :toimenpide}
                                      {:nodes [{:nodes [{:id :ennakkoluvat_ja_lausunnot.lausunto,
                                                         :metadata {:henkilotiedot :ei-sisalla,
                                                                    :julkisuusluokka :julkinen,
                                                                    :sailytysaika {:arkistointi :ikuisesti,
                                                                                   :perustelu "Laki"},
                                                                    :kieli :fi,
                                                                    :tila :luonnos,
                                                                    :myyntipalvelu true,
                                                                    :nakyvyys :julkinen},
                                                         :nodes [],
                                                         :type :asiakirja}],
                                                :text "Lausunnot annetaan",
                                                :type :toimenpide-tarkenne}],
                                       :text "Käsittelyssä",
                                       :type :toimenpide}
                                      {:nodes [{:nodes [{:id :muut.paatos,
                                                         :metadata {:henkilotiedot :sisaltaa,
                                                                    :julkisuusluokka :julkinen,
                                                                    :sailytysaika {:arkistointi :ikuisesti,
                                                                                   :perustelu "AL 11665/07.01.01.03.01/2008"},
                                                                    :kieli :fi,
                                                                    :tila :luonnos,
                                                                    :myyntipalvelu true,
                                                                    :nakyvyys :julkinen},
                                                         :nodes [],
                                                         :type :asiakirja}],
                                                :text "Päätöksen laatiminen",
                                                :type :toimenpide-tarkenne}],
                                       :text "Päätöksenteko",
                                       :type :toimenpide}
                                      {:nodes [{:nodes [{:id :ilmoitus,
                                                         :metadata {:henkilotiedot :sisaltaa,
                                                                    :julkisuusluokka :julkinen,
                                                                    :sailytysaika {:arkistointi :toistaiseksi,
                                                                                   :laskentaperuste :rakennuksen_purkamispäivä,
                                                                                   :perustelu "Kuntaliiton ohje 1. s. 6"},
                                                                    :kieli :fi,
                                                                    :tila :luonnos,
                                                                    :myyntipalvelu true,
                                                                    :nakyvyys :julkinen},
                                                         :nodes [],
                                                         :type :asiakirja}],
                                                :text "Kuuluttaminen",
                                                :type :toimenpide-tarkenne}
                                               {:nodes [{:id :muut.paatosote,
                                                         :metadata {:henkilotiedot :sisaltaa,
                                                                    :julkisuusluokka :julkinen,
                                                                    :sailytysaika {:arkistointi :toistaiseksi,
                                                                                   :laskentaperuste :rakennuksen_purkamispäivä,
                                                                                   :perustelu "Kuntaliiton ohje 1. s. 6"},
                                                                    :kieli :fi,
                                                                    :tila :luonnos,
                                                                    :myyntipalvelu true,
                                                                    :nakyvyys :julkinen},
                                                         :nodes [],
                                                         :type :asiakirja}],
                                                :text "Päätöksestä tiedottaminen",
                                                :type :toimenpide-tarkenne}],
                                       :text "Toimeenpano",
                                       :type :toimenpide}],
                              :text "Rakennuslupamenettely",
                              :type :asia}
                             {:code 2,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Toimenpidelupamenettely",
                              :type :asia}
                             {:code 3,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Maisematyölupamenettely",
                              :type :asia}
                             {:code 4,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Purkamislupamenettely",
                              :type :asia}
                             {:code 5,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Kokoontumistila-asiat",
                              :type :asia}
                             {:code 6,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Poikkeamispäätösmenettely",
                              :type :asia}
                             {:code 7,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Suunnittelutarveratkaisumenettely",
                              :type :asia}
                             {:code 8,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [{:nodes [{:nodes [{:id :hakemus,
                                                         :metadata {:henkilotiedot :sisaltaa,
                                                                    :julkisuusluokka :julkinen,
                                                                    :sailytysaika {:arkistointi :määräajan,
                                                                                   :perustelu "Laki",
                                                                                   :pituus 50},
                                                                    :tila :luonnos,
                                                                    :myyntipalvelu true,
                                                                    :nakyvyys :julkinen,
                                                                    :kieli :fi},
                                                         :nodes [{:id :paapiirustus.asemapiirros,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :määräajan,
                                                                                            :perustelu "Laki",
                                                                                            :pituus 50},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}
                                                                 {:id :paapiirustus.pohjapiirros,
                                                                  :metadata {:henkilotiedot :ei-sisalla,
                                                                             :julkisuusluokka :julkinen,
                                                                             :sailytysaika {:arkistointi :määräajan,
                                                                                            :perustelu "Laki",
                                                                                            :pituus 50},
                                                                             :kieli :fi,
                                                                             :tila :luonnos,
                                                                             :myyntipalvelu true,
                                                                             :nakyvyys :julkinen},
                                                                  :type :liite}],
                                                         :type :asiakirja}],
                                                :text "Ilmoitus jätetään",
                                                :type :toimenpide-tarkenne}],
                                       :text "Vireilletulo",
                                       :type :toimenpide}],
                              :text "Toimenpideilmoitusmenettely",
                              :type :asia}
                             {:code 9,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Maisematyöilmoitusmenettely",
                              :type :asia}
                             {:code 10,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Purkamisilmoitusmenettely",
                              :type :asia}
                             {:code 11,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Työnjohtaja-asiat",
                              :type :asia}
                             {:code 12,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Rakentamisen aikainen valvonta",
                              :type :asia}
                             {:code 13,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Muutoksenhakumenettely",
                              :type :asia}],
                     :text "Rakennusvalvonta",
                     :type :alitehtava}],
            :text "Rakentaminen, ylläpito ja käyttö",
            :type :tehtava}],
   :text             "Maankäyttö, Rakentaminen ja Asuminen",
   :type             :paatehtava
   :tos-type         "R"
   :attachment-types "Rakennusluvat-v2"})

(def ya-tos
  {:code 10,
   :nodes [{:code 3,
            :nodes [{:code 1,
                     :nodes [{:code 0,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Yleinen asia",
                              :type :asia}],
                     :text "Yleisten alueiden suunnittelu",
                     :type :alitehtava}],
            :text "Rakentaminen, ylläpito ja käyttö",
            :type :tehtava}],
   :text "Maankäyttö, Rakentaminen ja Asuminen",
   :type :paatehtava
   :tos-type "YA"
   :attachment-types "YleistenAlueidenLuvat-v2"})

(def ymp-tos
  {:code 11,
   :nodes [{:code 0,
            :nodes [{:code 0,
                     :nodes [{:code 0,
                              :metadata {:henkilotiedot :ei-sisalla,
                                         :julkisuusluokka :julkinen,
                                         :sailytysaika {:arkistointi :ei, :perustelu "<Arvo puuttuu>"},
                                         :kieli :fi},
                              :nodes [],
                              :text "Yleinen asia",
                              :type :asia}],
                     :text "Ympäristö- ja maa-ainesluvat",
                     :type :alitehtava}],
            :text "Ympäristönsuojelu",
            :type :tehtava}],
   :text "Ympäristöasiat",
   :type :paatehtava
   :tos-type "YMP"
   :attachment-types "Ymparisto-types"})
