# Builds the parent POM and common module, then saves the Maven repo
FROM maven:3.9.8-eclipse-temurin-21 AS common-builder

WORKDIR /app

# Copy parent POM and install it non-recursively
COPY pom.xml .
RUN mvn -N clean install -DskipTests

# Copy and build the common module
COPY common/pom.xml ./common/pom.xml
COPY common/src ./common/src
RUN mvn -f common/pom.xml clean install -DskipTests

# Persist the Maven repository for reuse
FROM scratch
COPY --from=common-builder /root/.m2 /root/.m2