(ns lupapiste-toj.routes-test
  (:require [lupapiste-toj.routes :as r]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [lupapiste-toj.test-helpers :refer [with-mongo-db mongo timbre-logging-disabled]]))

(use-fixtures :each with-mongo-db timbre-logging-disabled)

(defn post-rekey-from [handler address]
  (handler (assoc (mock/request :post "/rekey")
                  :remote-addr address)))

(deftest rekey
  (testing "Rekey allowed only from localhost"
    (let [{:keys [db]} @mongo
          handler (r/create-routes {:mode :prod
                                    :db db
                                    :build-info {}
                                    :session-key-path "/dev/random"
                                    :lupapiste-host ""})]
      (is (= 401 (:status (post-rekey-from handler "8.8.8.8"))))
      (is (= 200 (:status (post-rekey-from handler "127.0.0.1"))))
      (is (= 200 (:status (post-rekey-from handler "0:0:0:0:0:0:0:1")))))))
