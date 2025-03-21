# First stage: build the application
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the parent pom.xml
COPY pom.xml /app/pom.xml

WORKDIR /app/server

# Copy server pom.xml
COPY  server/pom.xml .
# Copy the source code
COPY server/src ./src
# Build the application
RUN mvn -f pom.xml clean install -DskipTests

# Second stage: run the application
FROM eclipse-temurin:21-jre

# Set the working directory to /app
WORKDIR /app

# Copy the built JAR from the first stage
COPY --from=build /app/server/target/server-0.0.1-SNAPSHOT.jar /app/app.jar
# Expose the application's port
EXPOSE 9090

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
