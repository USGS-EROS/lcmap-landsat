# Frequently Asked Questions
Common issues experienced when developing and testing lcmap-landsat

### Test failure: "Caused by: com.datastax.driver.core.exceptions.InvalidQueryException: Undefined column name XXXXX"
The Cassandra schema is out of sync from what the code expects.  If running
Cassandra locally for development & test, the simplest way to solve this
is to stop all docker processes with `docker-compose down` and then remove
all docker containers & images with `bin/docker-super-clean`.  Running
`lein test` will automatically create the proper Cassandra schema before
executing tests.
