# syntax=docker/dockerfile:1
# Contexto de build: a RAIZ do monorepo (docker build -f showcase-client/Dockerfile .)

# ---------- estagio 1: build com Maven ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY showcase-core/pom.xml showcase-core/pom.xml
COPY showcase-server-stdio/pom.xml showcase-server-stdio/pom.xml
COPY showcase-server-webmvc/pom.xml showcase-server-webmvc/pom.xml
COPY showcase-server-webflux/pom.xml showcase-server-webflux/pom.xml
COPY showcase-client/pom.xml showcase-client/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl showcase-client -am dependency:go-offline || true

COPY showcase-client/src showcase-client/src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl showcase-client package -DskipTests

# ---------- estagio 2: runtime enxuto ----------
FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 showcase
USER showcase
WORKDIR /app
COPY --from=build /workspace/showcase-client/target/showcase-client-*.jar app.jar

# Variaveis de ambiente reconhecidas:
#   SERVER_PORT             - porta da API REST de demos (padrao 8090)
#   SHOWCASE_WEBMVC_URL     - URL do servidor webmvc  (ex.: http://showcase-webmvc:8091)
#   SHOWCASE_WEBFLUX_URL    - URL do servidor webflux (ex.: http://showcase-webflux:8092)
#   SPRING_PROFILES_ACTIVE  - "http-only" dentro do compose (nao sobe o subprocesso STDIO)
#   ANTHROPIC_API_KEY       - opcional; com o perfil real-llm ativa sampling com LLM real
ENV SERVER_PORT=8090
EXPOSE 8090

HEALTHCHECK --interval=15s --timeout=3s --retries=8 --start-period=40s \
    CMD curl -sf http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
