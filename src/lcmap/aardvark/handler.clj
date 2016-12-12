(ns lcmap.aardvark.handler
  "Build middleware and handlers for various resources provided.

  Resources require access to objects like a Cassandra session, an
  AMQP channel and exchange for publishing messages, and configuration
  to determine keyspace and table names for queries."
  (:require [cheshire.core :refer :all]
            [cheshire.generate :refer [add-encoder encode-str remove-encoder]]
            [compojure.core :refer :all]
            [clojure.tools.logging :as log]
            [ring.middleware.accept :refer [wrap-accept]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.problem :refer [wrap-problem]]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [lcmap.aardvark.config :refer [config]]
            [lcmap.aardvark.db :as db :refer [db-session]]
            [lcmap.aardvark.event :as event :refer [amqp-channel]]
            [lcmap.aardvark.landsat :as landsat]
            [lcmap.aardvark.middleware :refer [wrap-authenticate wrap-authorize]]
            [lcmap.aardvark.problem :as problem]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [mount.core :refer [defstate] :as mount])
  (:import [org.joda.time.DateTime]
           [org.apache.commons.codec.binary Base64]))

(defn make
  "Build a middleware wrapped handler for app. This approach makes
  dependencies (components) available to handling functions."
  []
  (context "/" request
       (-> (routes (source/resource)
                   (landsat/resource)
                   (problem/resource)
                   (tile/resource)
                   (tile-spec/resource))
        (wrap-accept)
        (wrap-authorize)
        (wrap-authenticate)
        (wrap-keyword-params)
        (wrap-params)
        (wrap-problem #(problem/transformer request)))))

(defstate server-exchange
  :start (let [exchange-name (get-in config [:event :server-exchange])]
           (log/debugf "creating server exchange: %s" exchange-name)
           (le/declare amqp-channel exchange-name "topic" {:durable true})))

(defstate server-queue
  :start (let [queue-name (get-in config [:event :server-queue])]
           (log/debugf "creating server queue: %s" queue-name)
           (lq/declare event/amqp-channel queue-name {:durable true
                                                      :exclusive false
                                                      :auto-delete false})))

(defstate handler
  :start (do
           (log/debugf "starting Ring handler")
           (make)))


;;; Server wide encoders -- should we use this for worker too?

(add-encoder org.joda.time.DateTime
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (str c))))

(defn base64-encoder
  ""
  [buffer jsonGenerator]
  (log/debug "encoding HeapByteBuffer")
  (let [size (- (.limit buffer) (.position buffer))
        copy (byte-array size)]
    (.get buffer copy)
    (.writeString jsonGenerator (Base64/encodeBase64String copy))))

(add-encoder java.nio.HeapByteBuffer base64-encoder)
