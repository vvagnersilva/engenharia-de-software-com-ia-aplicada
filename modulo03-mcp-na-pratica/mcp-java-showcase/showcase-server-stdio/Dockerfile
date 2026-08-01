# syntax=docker/dockerfile:1
# Contexto de build: a RAIZ do monorepo (docker build -f showcase-server-stdio/Dockerfile .)
#
# Este servidor conversa por stdin/stdout, entao o uso e diferente:
#   docker run -i mcp-showcase-stdio
# E o formato que o Claude Desktop espera em "command": "docker", "args": ["run","-i",...].
# Nao ha porta exposta nem HEALTHCHECK - a "saude" e o proprio protocolo responder.

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
    ./mvnw -B -q -pl showcase-server-stdio -am dependency:go-offline || true

COPY showcase-core/src showcase-core/src
COPY showcase-server-stdio/src showcase-server-stdio/src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl showcase-server-stdio -am package -DskipTests

# ---------- estagio 2: runtime enxuto ----------
FROM eclipse-temurin:21-jre AS runtime
RUN useradd -r -u 1001 -m showcase
USER showcase
WORKDIR /app
COPY --from=build /workspace/showcase-server-stdio/target/showcase-server-stdio-*.jar app.jar

# O log vai para arquivo para nao poluir o stdout do protocolo
ENV SHOWCASE_LOG_FILE=/tmp/showcase-server-stdio.log

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
