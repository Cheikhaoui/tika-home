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
# The assistant's local embedding model, baked into the image: containers start without downloading it.
ARG EMBEDDING_BASE=https://huggingface.co/Xenova/paraphrase-multilingual-MiniLM-L12-v2/resolve/main
RUN mkdir -p /app/models \
 && curl -fsSL -o /app/models/model_quantized.onnx "$EMBEDDING_BASE/onnx/model_quantized.onnx" \
 && curl -fsSL -o /app/models/tokenizer.json "$EMBEDDING_BASE/tokenizer.json"
# Spring Boot layers, least to most often changed: a code change only rebuilds the small last layer.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    TIKAHOME_ASSISTANT_EMBEDDING_MODEL_URI=file:/app/models/model_quantized.onnx \
    TIKAHOME_ASSISTANT_EMBEDDING_TOKENIZER_URI=file:/app/models/tokenizer.json \
    TIKAHOME_ASSISTANT_MODEL_CACHE_DIRECTORY=/tmp/tikahome-models \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
