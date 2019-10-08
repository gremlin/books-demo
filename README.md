# books-demo
Demo app that sets up a 3-service system and integrates ALFI and Datadog APM

# Prerequisities
* export `DD_API_KEY=<xxx>` - this is the Datadog API Key, which you can get from the Datadog UI
* valid team certificate at `~/.gremlin/cert.pem`.  You can change this path, but you then need to expose it to Docker via docker-compose.yml
* valid team private key at `~/.gremlin/priv.pem`.  You can change this path, but you then need to expose it to Docker via docker-compose.yml
* export `GREMLIN_TEAM_ID=<xxx>` - this is the Gremlin team ID, which you can get from the Gremlin UI

# Steps to get running
* `gradlew clean build` - this will build the JARs 
* `docker-compose build` - this will turn the JARs into Docker images locally
* `docker-compose --compatibility up` - this will start all of the Docker images of services and the Datadog agent.  The compatibility flag applies the memory/CPU limits, so that failures happen in a known way, regardless of the place you run this demo

# How to use
* Once you've got the system running, confirm that you've got 4 containers using `docker ps`.  You should see a datadog metrics container, and 3 application containers (`api`, `recommendations` and `users`).
* Once all applications are started up properly, you can make a request to the API at: `http://localhost:8090/recommendations/<username>`, where any string should work in place of `<username>`.
* If you want to tweak some of the configuration, the pieces which are currently exposed as properties are located at `api/src/main/resources/application.properties`.  They include HTTP timeouts, concurrency bounds, number of server threads, and whether to use the S3 fallback.  If you edit these values, you'll need to rerun all of the build steps
* If you want to run the client to produce a large number of requests, then go to the `asyncclient` directory and run `../gradlew run`.  You can edit the target RPS and length of the test in the code.
