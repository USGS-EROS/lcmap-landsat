(ns lcmap.aardvark.config
  "Config-related state management.

  This application is configured by reading EDN; either from a file
  during development/test or from STDIN when built and deployed.

  Schema is used in tandem with `uberconf` to enforce the presence of
  required values and coerce values into the expected types (numbers,
  booleans, and lists)."
  (:require [uberconf.core :as uberconf]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate] :as mount]
            [schema.core :as schema]))

(def jetty {(schema/optional-key :port) schema/Num
            (schema/optional-key :join?) schema/Bool
            (schema/optional-key :daemon?) schema/Bool
            schema/Keyword schema/Str})

(def event {:host schema/Str
            :port schema/Num
            schema/Keyword schema/Str})

(def database {:contact-points [schema/Str]
               :default-keyspace schema/Str})

(def server {:exchange schema/Str
             :queue schema/Str})

(def worker {:exchange schema/Str
             :queue schema/Str})

(def root-cfg
  {:jetty jetty
   :event event
   :database database
   (schema/optional-key :server) server
   (schema/optional-key :worker) worker
   schema/Keyword schema/Str})

(defstate config
  :start (let [cfg ((mount/args) :config)]
           (log/debugf "starting config: %s" cfg)
           (->> cfg
                (uberconf.core/coerce-cfg root-cfg)
                (uberconf.core/check-cfg root-cfg))))
