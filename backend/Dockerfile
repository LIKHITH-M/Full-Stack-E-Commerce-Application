# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Import Aiven CA certificate into a copy of the default JVM truststore.
# This preserves all standard CAs (needed for Neon PostgreSQL, etc.)
# and adds Aiven's CA so Kafka SSL handshake succeeds.
COPY aiven-ca.pem /app/aiven-ca.pem
RUN cp $JAVA_HOME/lib/security/cacerts /app/truststore.jks && \
    keytool -import -trustcacerts -alias aiven-ca \
            -file /app/aiven-ca.pem \
            -keystore /app/truststore.jks \
            -storepass changeit -noprompt

EXPOSE 8081
ENTRYPOINT ["java", \
  "-Djavax.net.ssl.trustStore=/app/truststore.jks", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-jar", "app.jar"]
