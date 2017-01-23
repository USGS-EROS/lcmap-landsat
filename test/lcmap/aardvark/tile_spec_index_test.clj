(ns lcmap.aardvark.tile-spec-index-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lcmap.aardvark.db :as db]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojure.tools.logging :as log]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.shared :as shared]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.tile-spec-test :refer [L5 spec-opts]]
            [lcmap.aardvark.tile-spec-index :as index]))

(deftest test-indexing
  (shared/with-system

    (tile-spec/process L5 spec-opts)

    (testing "+tags"
      (is (not (= nil? (index/+tags (first (tile-spec/all)))))))

    (testing "clear the index"
      (let [out (index/clear)
            raw (index/search "tm")
            err (get-in (first (get-in raw [:root_cause])) [:type])]
        (is (= "index_not_found_exception" err))))

    (testing "load and search the index"
          ;; have to call _refresh after loading the index to open a new segment
          ;; Otherwise we'd have to wait 1 second for the results to be searchable
      (let [load-results    (index/save (tile-spec/all))
            refresh-results (http/post (str (get-in config [:search :index-url])
                                            "/_refresh"))
            refresh-status  (:status @refresh-results)]
        (log/debug "ES Refresh Results:" refresh-status)
        (is (< 0 (count (index/result (index/search "tm")))))))

    (testing "index search"
      (let [raw (index/search "((tm AND cloud) OR band3) AND NOT shadow AND 5")
            results (index/result raw)
            expected-ubids (seq (vector "LANDSAT_5/TM/sr_cloud_qa"
                                        "LANDSAT_5/TM/sr_adjacent_cloud_qa"
                                        "LANDSAT_5/TM/cfmask_conf"
                                        "LANDSAT_5/TM/cfmask"
                                        "LANDSAT_5/TM/sr_band3"
                                        "LANDSAT_5/TM/toa_band3"))]

        ;; "LANDSAT_7/ETM/sr_band3" "LANDSAT_7/ETM/toa_band3"
        (log/trace "Raw results from search:" raw)
        (log/trace "Raw type:" (type raw))
        (log/trace "Intermediate:" (get-in raw [:hits]))
        (log/debug "Formatted results from search:" results)
        (is (not (= nil? results)))
        (is (seq? results))
        (is (= (sort expected-ubids) (sort (map #(get % :ubid) results))))))))
