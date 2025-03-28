# Server Dockerfile - reuses Maven repo from common image
FROM maven:3.9.8-eclipse-temurin-21 AS build

# Copy pre-built Maven repository from common image
COPY --from=common:latest /root/.m2 /root/.m2

WORKDIR /app

# Copy server files
COPY pom.xml .
COPY server/pom.xml ./server/pom.xml
COPY server/src ./server/src

# Build server (parent/common are already cached)
RUN mvn -f server/pom.xml clean install -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/server/target/server-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]