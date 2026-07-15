# ---- Stage 1: build the JAR ----
# Uses Maven + JDK 21 to compile and package. Tests are skipped here because
# they need a database; run `mvn test` locally / in CI instead.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first so Maven can cache dependencies when only source changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Stage 2: lightweight runtime ----
# Only a JRE + the built JAR — smaller and safer than shipping the whole JDK.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user (good practice; Cloud Run doesn't require it but it's safer).
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/target/books-*.jar app.jar

# Cloud Run sends traffic to the port in the PORT env var (default 8080).
# Spring reads it via server.port=${PORT:8080} in application.yml.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
