FROM java:8-jdk-alpine
COPY ./build/libs/api-demo-1.0.0.jar /usr/app/
COPY ./dd-java-agent.jar /usr/app/
WORKDIR /usr/app
RUN sh -c 'touch api-demo-1.0.0.jar'
ENTRYPOINT ["java","-jar", \
      "-javaagent:dd-java-agent.jar", \
      "-Dcom.sun.management.jmxremote", \
      "-Dcom.sun.management.jmxremote.port=11111", \
      "-Dcom.sun.management.jmxremote.authenticate=false", \
      "-Dcom.sun.management.jmxremote.ssl=false", \
      "-Ddd.service.name=api-demo", \
      "-Ddd.agent.host=host.docker.internal", \
      "api-demo-1.0.0.jar"]
