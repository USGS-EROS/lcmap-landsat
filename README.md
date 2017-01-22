[![Build Status](https://travis-ci.org/USGS-EROS/lcmap-landsat.svg?branch=develop)](https://travis-ci.org/USGS-EROS/lcmap-landsat)

<!-- Add the clojars badge once this project is actually pushed there -->
<!--[![Clojars Project][clojars-badge]][clojars]-->

# lcmap-landsat

LCMAP Landsat data ingest, inventory &amp; distribution.

### Usage
Retrieve data.  Any number of ubids may be specified.
```bash
# Using httpie
user@machine:~$ http http://host:port/landsat/tiles
                     ?x=-2013585
                     &y=3095805
                     &acquired=2000-01-01/2017-01-01
                     &ubid=LANDSAT_8/OLI_TIRS/sr_band1
                     &ubid=LANDSAT_8/OLI_TIRS/sr_band2
                     &ubid=LANDSAT_8/OLI_TIRS/sr_band3
```

Search for ubids.  ?q= parameter uses [ElasticSearch QueryStringSyntax](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax).
```bash
user@machine:~$ http http://host:port/landsat/ubids
                     ?q=((landsat AND 8) AND sr AND (band1 OR band2 OR band3))
```

lcmap-landsat honors HTTP ```Accept``` headers for both ```application/json```
and ```text/html```.  The default is json.

### Developing

Initialize submodules (to get dev/test data).

```bash
git submodule init
git submodule update
```

Install docker-compose (make sure the version support docker-compose.yml version 2 formats).

```bash
# will run infrastructure as a daemon
make docker-dev-up

# Keeps processes in foreground, useful for troubleshooting
make docker-dev-up-nodaemon

# cleanly shut down daemons when done.
make docker-dev-down
```

Start a REPL.

```bash
lein run
```

Switch to the `lcmap.aardvark.dev` namespace and start the system. This
will start a server (using Jetty) and a worker.

```clojure
(dev)
(start)
```

A [FAQ][3] is available for common development & test issues.


### Building

Use `lein uberjar` (or `make build`) to build a standalone jarfile.

### Running

There are two modes of operation: a web-server that handles HTTP requests, and a worker that handles AMQP messages. A single process can simultaneusly run both modes, although this is not recommended in a production environment.

Execute the jarfile like this, passing configuration data as EDN via STDIN:

```bash
java -jar \
  target/uberjar/lcmap-landsat-0.1.0-SNAPSHOT-standalone.jar \
  $(cat dev/resources/lcmap-landsat.edn)
```

### Docker Image

Use `make docker-image` to build a Docker image that includes GDAL dependencies.

## Deployment

[Docker images][2] are automatically built when all tests pass on Travis CI. You may either run the Docker image with additional command line parameters or, if you prefer, build an image using file based configuration.

Example:
```
docker run -p 5679:5679 usgseros/lcmap-landsat:0.1.0-SNAPSHOT $(cat ~/landsat.edn)
```

Example config:
```edn
{:database  {:contact-points "172.17.0.1"
             :default-keyspace "lcmap_landsat"}
 :event     {:host "172.17.0.1"
             :port 5672
             :queues [{:name "lcmap.landsat.server.queue"
                       :opts {:durable true
                              :exclusive false
                              :auto-delete false}}
                      {:name "lcmap.landsat.worker.queue"
                       :opts {:durable true
                              :exclusive false
                              :auto-delete false}}]
             :exchanges [{:name "lcmap.landsat.server.exchange"
                          :type "topic"
                          :opts {:durable true}}
                         {:name "lcmap.landsat.worker.exchange"
                          :type "topic"
                          :opts {:durable true}}]
             :bindings [{:exchange "lcmap.landsat.server.exchange"
                         :queue "lcmap.landsat.worker.queue"
                         :opts {:routing-key "ingest"}}]}
 :http      {:port 5679
             :join? false
             :daemon? true}
 :server    {:exchange "lcmap.landsat.server.exchange"
             :queue    "lcmap.landsat.server.queue"}
 :worker    {:exchange "lcmap.landsat.worker.exchange"
             :queue    "lcmap.landsat.worker.queue"}
 :search     {:index-url      "http://localhost:9200/tile-specs"
              :bulk-api-url   "http://localhost:9200/tile-specs/ops/_bulk"
              :search-api-url "http://localhost:9200/tile-specs/_search"
              :max-result-size 10000}}

```

### Links

[1]: https://github.com/USGS-EROS/lcmap-landsat/blob/develop/resources/shared/lcmap-landsat.edn "Configuration File"
[2]: https://hub.docker.com/r/usgseros/lcmap-landsat/ "Docker Image"
[3]: docs/DevFAQ.md "Developers Frequently Asked Questions"
