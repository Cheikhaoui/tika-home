# Multi-stage build: compile with the JDK, run with the smaller JRE.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
# Dependencies first: this layer is cached as long as pom.xml doesn't change.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package \
 && java -Djarmode=tools -jar target/backend-*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
# curl only for the container health check.
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 1001 --no-create-home app
WORKDIR /app
# Spring Boot layers, least to most often changed: a code change only rebuilds the small last layer.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
