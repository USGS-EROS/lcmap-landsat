(ns lcmap.aardvark.landsat
  "Resources and representations.

  This namespace contains all route definitions."
  (:require [camel-snake-kebab.core :refer [->snake_case_keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [ring.util.accept :refer [accept]]
            [lcmap.aardvark.html :as html]
            [lcmap.aardvark.source :as source]
            [lcmap.aardvark.tile :as tile]
            [lcmap.aardvark.tile-spec :as tile-spec]
            [lcmap.aardvark.util :refer [vectorize]]
            [lcmap.aardvark.middleware :refer [wrap-handler]]))

;;; Response producing functions

(defn allow [& verbs]
  (log/debug "explaining allow verbs")
  {:status 405
   :headers {"Allow" (str/join "," verbs)}})

(defn sample-source
  "Retrieve some random sources"
  []
  (log/debugf "summarizing sources")
  {:status 200 :body (source/sample 10)})

(defn get-source
  "Search for a source and produce a response map."
  [source-id]
  (log/debugf "lookup source: %s" source-id)
  (if-let [result (seq (source/search source-id))]
    {:status 200 :body result}
    {:status 404 :body []}))

(defn put-source
  "Handle request for creating a source and produce a response."
  [source-id {params :params :as req}]
  (let [source (merge {:id source-id :progress_name "created"} params)]
    (or (some->> (source/validate source)
                 (assoc {:status 403} :body))
        (some->> (source/search source-id)
                 (assoc {:status 409} :body))
        (some->> (source/insert-and-publish source)
                 (assoc {:status 202} :body)))))

(defn get-tiles
  "Get tiles containing point for given UBID and ISO8601 time range."
  [{{:keys [:ubid :x :y :acquired]} :params :as req}]
  (let [tile+    {:ubids (vectorize ubid)
                  :x (Integer/parseInt x)
                  :y (Integer/parseInt y)
                  :acquired (str/split acquired #"/")}
        tiles (tile/find tile+)]
    (log/debugf "GET /landsat/tiles")
    {:status 200 :body tiles}))

(defn get-tile-spec
  "Search for a source and produce a response map."
  [ubid {params :params :as req}]
  (if-let [results (first (tile-spec/query (merge {:ubid ubid} params)))]
    {:status 200 :body results}
    {:status 404 :body nil}))

(defn delete-tile-spec
  "Deleting a tile-spec is not supported.

  Supporting removal of a tile-spec is unusual and would make it
  impossible to get tile data for those UBIDs."
  [ubid]
  {:status 501})

(defn get-tile-specs
  "Get all tile-specs"
  []
  (let [results (tile-spec/all)]
    {:status 200 :body results}))

(defn post-tile-spec
  "Save or create all tile-specs"
  [{body :body :as req}]
  (log/debugf "creating multiple tile specs: %s" (count body))
  (let [saved (map tile-spec/insert body)]
    {:status 200 :body {:saved (count saved)}}))

(defn put-tile-spec
  "Handle request for creating a tile-spec."
  [ubid {body :body :as req}]
  (log/debugf "tile-spec: %s = %s" ubid body)
  (let [tile-spec (merge {:ubid ubid} body)]
    (or (some->> (tile-spec/validate tile-spec)
                 (assoc {:status 403} :body))
        (some->> (tile-spec/insert tile-spec)
                 (assoc {:status 202} :body)))))

;;; Request entity transformers.

(defn decode-json
  ""
  [body]
  (log/debug "req - decoding as JSON")
  (->> body
       (slurp)
       (json/decode)
       (transform-keys ->snake_case_keyword)))

(defn prepare-with
  "Request transform placeholder."
  [request]
  (log/debugf "req - prepare body: %s" (get-in request [:headers]))
  (if (= "application/json" (get-in request [:headers "content-type"]))
    (update request :body decode-json)
    request))

;;; Response entity transformers.

(defn to-html
  "Encode response body as HTML."
  [response]
  (log/debug "responding with HTML")
  (let [template-fn (:template (meta response) html/default)]
    (update response :body template-fn)))

(defn to-json
  "Encode response body as JSON."
  [response]
  (log/debug "responding with json")
  (update response :body json/encode))

(def supported-types (accept "application/json" to-json
                             "text/html" to-html))

(defn respond-with
  ""
  [request response]
  (supported-types request response))

;;; Routes

(defn resource
  "Handlers for landsat resource."
  []
  (wrap-handler
   (context "/landsat" request
     (GET    "/" []
             (with-meta {:status 200}
               {:template html/default}))
     (GET    "/sources" []
             (with-meta (sample-source)
               {:template html/source-list}))
     (GET    "/source/:source-id{.+}" [source-id]
             (with-meta (get-source source-id)
               {:template html/source-info}))
     (PUT    "/source/:source-id{.+}" [source-id]
             (with-meta (put-source source-id request)
               {:template html/source-info}))
     (GET    "/tiles" []
             (with-meta (get-tiles request)
               {:template html/tile-list}))
     (GET    "/tile/:id" [id]
             (with-meta {:body "coming soon"}
               {:template html/tile-info}))
     (GET    "/tile-specs" []
             (with-meta (get-tile-specs)
               {:template html/tile-spec-list}))
     (POST   "/tile-spec/" []
             (with-meta (post-tile-spec request)
               {:template html/tile-spec-list}))
     (GET    "/tile-spec/:ubid{.+}" [ubid]
             (with-meta (get-tile-spec ubid request)
               {:template html/tile-spec-info}))
     (PUT    "/tile-spec/:ubid{.+}" [ubid]
             (with-meta (put-tile-spec ubid request)
               {:template html/tile-spec-info}))
     (DELETE "/tile-spec/:ubid{.+}" [ubid]
             (with-meta (delete-tile-spec ubid)
               {:template html/tile-spec-info}))
     (GET    "/problem/" []
             {:status 200 :body "problem resource"}))
   prepare-with respond-with))
