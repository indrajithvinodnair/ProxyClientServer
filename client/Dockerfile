# First stage: build the application
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the parent pom.xml
COPY pom.xml /app/pom.xml

WORKDIR /app/client

# Copy client pom.xml
COPY client/pom.xml .
# Copy the source code
COPY client/src ./src

# Accept build arguments
ARG OFFSHORE_PROXY_URL

# Set environment variables
ENV OFFSHORE_PROXY_URL=${OFFSHORE_PROXY_URL}

# Replace the placeholder in application.properties
RUN sed -i "s|http://localhost:9090|${OFFSHORE_PROXY_URL}|g" src/main/resources/application.properties

# Build the application
RUN mvn -f pom.xml clean install -DskipTests

# Second stage: run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the first stage
COPY --from=build app/client/target/client-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]