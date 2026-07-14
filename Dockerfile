# syntax=docker/dockerfile:1.7

# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Cache dependencies separately from source
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && \
    ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/*-SNAPSHOT.jar app.jar

RUN useradd -r spring
USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]