(ns lupapiste-toj.lupapiste-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [lupapiste-toj.lupapiste :as lupis]
            [lupapiste-toj.test-helpers :refer [timbre-logging-disabled]]))

(use-fixtures :each timbre-logging-disabled)

(deftest get-organization-names
  (testing "Fetching organization names from lupapiste API with cache"
    (let [call-count (atom 0)
          names {:123-R {:fi "Rakennusvalvonta 123"}}
          canned-response {:names names
                           :ok true}
          fake-http-get (fn [url opts]
                          (swap! call-count inc)
                          (atom {:body (json/encode canned-response)}))]
      (reset! @#'lupapiste-toj.lupapiste/organization-name-cache {:names nil :fetch-timestamp nil})
      (with-redefs [org.httpkit.client/get fake-http-get]
        (is (= names (lupis/get-organization-names "dummy-token" "dummy-host"))
            "id and name resolved")
        (lupis/get-organization-names "dummy-token" "dummy-host")
        (is (= 1 @call-count) "second call does not make a fetch, but returns from cache"))))
  (testing "Errors on fetching organization names"
    (let [invalid-json-http-get (fn [url opts] (atom {:body "<html></html>"}))]
      (reset! @#'lupapiste-toj.lupapiste/organization-name-cache {:names nil :fetch-timestamp nil})
      (with-redefs [org.httpkit.client/get invalid-json-http-get]
        (is (empty? (lupis/get-organization-names "dummy-token" "dummy-host")))))))
