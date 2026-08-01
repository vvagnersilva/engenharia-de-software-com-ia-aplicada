# showcase-server-webmvc

Servidor MCP **Streamable HTTP síncrono** sobre a pilha servlet (Spring MVC + Tomcat). Porta padrão **8091**, endpoint MCP em **`/mcp`**.

## Streamable HTTP em uma linha

Um único endpoint aceita `POST` (requisições JSON-RPC), `GET` (stream SSE de notificações servidor→cliente) e `DELETE` (encerrar sessão) — é o transporte HTTP recomendado do protocolo desde 2025-03-26.

## Rodar

```bash
../mvnw -pl showcase-server-webmvc -am package
java -jar target/showcase-server-webmvc-1.0.0-SNAPSHOT.jar
curl http://localhost:8091/actuator/health
```

Variáveis: `SERVER_PORT` (padrão 8091).

## Perfil `sse` (legado)

```bash
java -jar target/showcase-server-webmvc-1.0.0-SNAPSHOT.jar --spring.profiles.active=sse
```

Troca para o SSE clássico de dois endpoints (`/sse` + `/mcp/message`), **depreciado** pelo protocolo — existe aqui só para clientes antigos.

## Ponto didático do módulo

`McpCapabilitiesConfiguration`: o autoconfigure aceita **um** `McpSyncServerCustomizer` e a pilha servlet já registra o dela (que liga `immediateExecution`). O bean daqui é `@Primary` e reaplica esse ajuste além de ligar `subscribe=true` nos resources.

## Teste

`WebMvcServerCapabilitiesIT` — o IT mais completo do projeto: um método por capacidade avançada (sampling, elicitation, roots, progresso, logging com `setLevel`, completions, paginação, ping) usando o cliente real do SDK.
