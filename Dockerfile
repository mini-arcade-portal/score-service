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

# Without this the JVM sizes the heap at 25% of the container limit, which is a
# default rather than a decision. Pinning it keeps memory use predictable, which
# matters once the HorizontalPodAutoscaler is scaling on measured utilisation.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]