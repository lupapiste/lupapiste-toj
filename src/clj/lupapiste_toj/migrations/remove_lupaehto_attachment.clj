(ns lupapiste-toj.migrations.remove-lupaehto-attachment
  (:require [lupapiste-toj.migrations.attachment-type-utils :as utils]))

(def attachment-mapping
  {{:type-group :muut :type-id :lupaehto} {:type-group :muut :type-id :muu}})

(defn change-lupaehto-to-muu [db]
  (utils/migrate-attachment-types db ["draft" "default-data" "published"] attachment-mapping))
