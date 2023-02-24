(ns lupapiste-toj.authorization-test
  (:require [lupapiste-toj.authorization :as auth]
            [clojure.test :refer :all]))

(deftest user-is-authorized-for-org
  (let [user {:role "authorityAdmin"
              :email "admin@sipoo.fi"
              :username "sipoo"
              :firstName "Simo"
              :orgAuthz {:753-R #{:morjens :authority}
                         :569-R #{:authority}
                         :132-R #{:morjens}}
              :organizations ["753-R"]
              :expires 1429096234191
              :id "50ac77ecc2e6c2ea6e73f83e"
              :lastName "Suurvisiiri"}]
    (is (auth/user-is-authorized-for-org :753-R user))
    (is (auth/user-is-authorized-for-org :569-R user))
    (is (not (auth/user-is-authorized-for-org :132-R user)))))
