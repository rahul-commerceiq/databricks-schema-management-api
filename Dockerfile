FROM 208876916689.dkr.ecr.us-west-2.amazonaws.com/java:openjdk17-newrelic-builder_20240405
WORKDIR /databricks-schema-management-api
COPY target/databricks-schema-management-api-*.jar  ./databricks-schema-management-api.jar
ENTRYPOINT java -jar databricks-schema-management-api.jar