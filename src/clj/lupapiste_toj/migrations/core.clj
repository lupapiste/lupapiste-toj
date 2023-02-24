(ns lupapiste-toj.migrations.core
  (:require [clojure.set :refer [difference]]
            [monger.collection :as mc]
            [taoensso.timbre :as timbre]
            [lupapiste-toj.migrations.add-tila :refer [add-tila-metadata]]
            [lupapiste-toj.migrations.add-valid-from :refer [add-valid-from]]
            [lupapiste-toj.migrations.add-publisher :refer [add-publisher]]
            [lupapiste-toj.migrations.change-document-types :refer [update-asiakirja-ids]]
            [lupapiste-toj.migrations.replace-empty-string-values :refer [set-values-to-empty-required-fields]]
            [lupapiste-toj.migrations.add-myyntipalvelu-and-nakyvyys :refer [add-myyntipalvelu-and-nakyvyys-metadata]]
            [lupapiste-toj.migrations.add-default-data-collection :refer [create-collection-default-data drop-default-data-and-recreate-as-normal-collection]]
            [lupapiste-toj.migrations.change-tila-to-enum :refer [change-tila-metadata-to-enum]]
            [lupapiste-toj.migrations.change-lausunto-to-liite :refer [change-lausunto-to-liite-id]]
            [lupapiste-toj.migrations.change-paatos-to-liite :refer [change-paatos-to-liite]]
            [lupapiste-toj.migrations.remove-default-document :refer [remove-default-documents]]
            [lupapiste-toj.migrations.change-nakyvyys-md-key :refer [change-nakyvyys-key]]
            [lupapiste-toj.migrations.remove-invalid-suojaustaso :refer [remove-suojaustaso-from-julkinen-docs]]
            [lupapiste-toj.migrations.add-kayttajaryhma :refer [add-kayttajaryhma-for-secret-documents]]
            [lupapiste-toj.migrations.add-kieli :refer [add-kieli-to-metadata]]
            [lupapiste-toj.migrations.attachment-types-v2 :refer [migrate-attachment-types]]
            [lupapiste-toj.migrations.remove-invalid-retention-ends :refer [remove-invalid-laskentaperuste]]
            [lupapiste-toj.migrations.fix-retention-ends :refer [add-missing-laskentaperuste]]
            [lupapiste-toj.migrations.add-tos-type :refer [add-tos-type]]
            [lupapiste-toj.migrations.add-ya-tos-default-data :refer [create-ya-default-data]]
            [lupapiste-toj.migrations.add-ymp-tos-default-data :refer [create-ymp-default-data]]
            [lupapiste-toj.migrations.change-to-permanent-archiving :refer [change-all-r-orgs-archived-docs-to-permanent-retention]]
            [lupapiste-toj.migrations.remove-lupaehto-attachment :refer [change-lupaehto-to-muu]]
            [lupapiste-toj.migrations.add-lausunnon-liite :refer [add-lausunnon-liite-attachment]]
            [lupapiste-toj.persistence :as p]))

(def migrations
  {1 {:description "Adds :tila key to metadata under :liite and :asiakirja nodes"
      :op add-tila-metadata}
   2 {:description "Adds :valid-from to published plans (tos)"
      :op add-valid-from}
   3 {:description "Adds publisher to published plans (tos)"
      :op add-publisher}
   4 {:description "Change document ids and drop unrecognized ones"
      :op update-asiakirja-ids}
   5 {:description "Adds placeholder values to required text fields that currently have a blank value"
      :op set-values-to-empty-required-fields}
   6 {:description "Adds :myyntipalvelu and :näkyvyys keys to metadata under :liite and :asiakirja nodes"
      :op add-myyntipalvelu-and-nakyvyys-metadata}
   7 {:description "Adds :default data collection"
      :op create-collection-default-data}
   8 {:description "Changes :tila metadata to a enum value"
      :op change-tila-metadata-to-enum}
   9 {:description "Changes :asiakirja type nodes with id :lausunto to :liite type and :ennakkoluvat_ja_lausunnot.lausunto id"
       :op change-lausunto-to-liite-id}
   10 {:description "Recreates default-data as a normal collection"
       :op drop-default-data-and-recreate-as-normal-collection}
   11 {:description "Changes :päätös docs to :muut.päätösote type"
       :op change-paatos-to-liite}
   12 {:description "Remove :default-document from TOS"
       :op remove-default-documents}
   13 {:description "Changes :näkyvyys metadata key to :nakyvyys"
       :op change-nakyvyys-key}
   14 {:description "Removes :suojaustaso metadata key from docs with julkisuusluokka = julkinen"
       :op remove-suojaustaso-from-julkinen-docs}
   15 {:description "Adds :kayttajaryhma and :kayttajaryhmakuvaus for secret documents"
       :op add-kayttajaryhma-for-secret-documents}
   16 {:description "Adds :kieli = fi to metadata"
       :op add-kieli-to-metadata}
   17 {:description "Migrate attachment types to v2"
       :op migrate-attachment-types}
   18 {:description "Remove invalid laskentaperuste values from sailytysaika metadata"
       :op remove-invalid-laskentaperuste}
   19 {:description "Re-run attachment type migration with updated mappings"
       :op migrate-attachment-types}
   20 {:description "Add missing laskentaperuste values"
       :op add-missing-laskentaperuste}
   21 {:description "Adds :tos-type and :attachment-types to R default-data"
       :op add-tos-type}
   22 {:description "Create YA default-data"
       :op create-ya-default-data}
   23 {:description "Change retention to permanent"
       :op change-all-r-orgs-archived-docs-to-permanent-retention}
   24 {:description "Change YA lupaehto attachments to muut.muu"
       :op change-lupaehto-to-muu}
   25 {:description "Re-run change retention to permanent"
       :op change-all-r-orgs-archived-docs-to-permanent-retention}
   26 {:description "Re-run tila migration to fix invalid values"
       :op change-tila-metadata-to-enum}
   27 {:description "Add lausunnon liite attachment to lausunto type nodes"
       :op add-lausunnon-liite-attachment}
   28 {:description "Add YMP default data"
       :op create-ymp-default-data}
   })

(defn validate-documents [db]
  (doseq [[coll schema] [["draft" p/Document] ["published" p/PublishedDocument] ["default-data" p/Document]]]
    (doseq [doc (mc/find-maps db coll)]
      (p/coerce-and-validate schema doc))))

(defn run-migrations
  ([db]
   (run-migrations db migrations)
   (validate-documents db))
  ([db migrations]
   (let [all-migration-ids (apply sorted-set (keys migrations))
         applied-migration-ids (set (map :_id (mc/find-maps db "migrations" {} [:_id])))
         unapplied-migration-ids (difference all-migration-ids applied-migration-ids)]
     (doseq [id unapplied-migration-ids]
       (timbre/info (str "Running migration " id ": " (get-in migrations [id :description])))
       ((get-in migrations [id :op]) db)
       (mc/insert db "migrations" {:_id id :description (get-in migrations [id :description])})))))
