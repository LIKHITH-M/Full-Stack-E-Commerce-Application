# =============================================================
# Stage 1: Build - Maven + JDK 21
# Layer order: pom.xml first so dependency downloads are cached
# unless pom.xml itself changes.
# =============================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy ONLY pom.xml first - Docker caches this layer separately.
# Maven downloads ~200 MB of dependencies here.
# If only src/ changes (no pom.xml change), this cached layer is reused.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source code and compile. This layer rebuilds on every src/ change,
# but the expensive dependency download above is already cached.
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================================
# Stage 2: Runtime - JRE only (smaller image, no build tools)
# =============================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create a non-root system user to run the application.
# Running as root inside a container is a security risk:
# if the app is compromised, the attacker has root in the container.
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy the compiled JAR from the build stage (not from your local machine).
COPY --from=build /app/target/*.jar app.jar

# Import Aiven CA certificate into the JVM truststore.
# This is needed only for cloud Kafka (Aiven SASL_SSL).
# For local Docker Compose, Kafka runs on PLAINTEXT so this is harmless.
# The file is created as a placeholder if it doesn't exist locally -
# see .dockerignore for why we always need it present in build context.
COPY aiven-ca.pem /app/aiven-ca.pem
RUN cp $JAVA_HOME/lib/security/cacerts /app/truststore.jks && \
    keytool -import -trustcacerts -alias aiven-ca \
            -file /app/aiven-ca.pem \
            -keystore /app/truststore.jks \
            -storepass changeit -noprompt

# Set file ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user for all subsequent commands and at runtime
USER appuser

EXPOSE 8081

# Spring Boot Actuator health endpoint used by Docker health check
# and by compose depends_on: condition: service_healthy
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djavax.net.ssl.trustStore=/app/truststore.jks", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-jar", "app.jar"]
