(ns lcmap.aardvark.shared
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [lcmap.aardvark.config :as config]
            [mount.core :as mount :refer [defstate]]
            [org.httpkit.client :as http]
            [qbits.alia :as alia]))

(defmacro with-system
  "Start and stop the system, useful for integration tests."
  [& body]
  `(let [cfg# (config/build {:edn (io/resource "lcmap-landsat.edn")})]
     (mount/start-with {#'config/config cfg#})
     (log/debug "Starting test system with config:" cfg#)
     (try
       (do ~@body)
       (finally
         (log/debug "Stopping test system")
         (mount/stop)))))

(defn req
  "Convenience function for making HTTP requests."
  [method url & {:keys [headers query-params form-params body]
                 :as   opts}]
  (let [defaults {:headers {"Accept" "application/json"}}]
    @(http/request (merge {:url url :method method} defaults opts))))
