# books-demo
Demo app that sets up a 3-service system and integrates ALFI and Datadog APM

# Prerequisities
* export `DD_API_KEY=<xxx>` - this is the Datadog API Key, which you can get from the UI
* valid team certificate at `~/.gremlin/cert.pem`.  You can change this path, but you then need to expose it to Docker via docker-compose.yml
* valid team private key at `~/.gremlin/priv.pem`.  You can change this path, but you then need to expose it to Docker via docker-compose.yml
* export GREMLIN_TEAM_ID=<xxx>` - this is the Gremlin team ID, which you can get from the UI

# Steps to get running
* `gradlew clean build` - this will build the JARs 
* `docker-compose build` - this will turn the JARs into Docker images locally
* `docker --compatibility up` - this will start all of the Docker images of services and the Datadog agent
