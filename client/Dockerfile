# client Dockerfile - reuses Maven repo from common image
FROM maven:3.9.8-eclipse-temurin-21 AS build

# Copy pre-built Maven repository from common image
COPY --from=common:latest /root/.m2 /root/.m2

WORKDIR /app

# Copy client files
COPY pom.xml .
COPY client/pom.xml ./client/pom.xml
COPY client/src ./client/src

# Build client (parent/common are already cached)
RUN mvn -f client/pom.xml clean install -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the built jar from the first stage
COPY --from=build app/client/target/client-0.0.1-SNAPSHOT.jar /app/app.jar
# Expose the port
EXPOSE 8080
# Set environment variables for logging levels
ENV LOGGING_LEVEL_REACTOR_NETTY_TRANSPORT_CONFIG=TRACE
# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
