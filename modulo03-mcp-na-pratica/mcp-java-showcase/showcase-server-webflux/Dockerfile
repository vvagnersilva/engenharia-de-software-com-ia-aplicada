# syntax=docker/dockerfile:1
# Contexto de build: a RAIZ do monorepo (docker build -f showcase-server-webflux/Dockerfile .)

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
    ./mvnw -B -q -pl showcase-server-webflux -am dependency:go-offline || true

COPY showcase-core/src showcase-core/src
COPY showcase-server-webflux/src showcase-server-webflux/src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl showcase-server-webflux -am package -DskipTests

# ---------- estagio 2: runtime enxuto ----------
FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 showcase
USER showcase
WORKDIR /app
COPY --from=build /workspace/showcase-server-webflux/target/showcase-server-webflux-*.jar app.jar

# Variaveis de ambiente reconhecidas:
#   SERVER_PORT             - porta HTTP (padrao 8092)
#   SPRING_PROFILES_ACTIVE  - use "stateless" para o modo sem sessao
ENV SERVER_PORT=8092
EXPOSE 8092

HEALTHCHECK --interval=15s --timeout=3s --retries=8 --start-period=40s \
    CMD curl -sf http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
