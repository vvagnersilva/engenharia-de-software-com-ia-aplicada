# showcase-server-webflux

Servidor MCP **Streamable HTTP reativo** (WebFlux + Netty, I/O não bloqueante). Porta padrão **8092**, endpoint MCP em **`/mcp`**.

## Rodar

```bash
../mvnw -pl showcase-server-webflux -am package
java -jar target/showcase-server-webflux-1.0.0-SNAPSHOT.jar
```

Variáveis: `SERVER_PORT` (padrão 8092), `SPRING_PROFILES_ACTIVE`.

## Perfil `stateless`

```bash
java -jar target/showcase-server-webflux-1.0.0-SNAPSHOT.jar --spring.profiles.active=stateless
```

`protocol: STATELESS`: sem sessão, cada requisição é independente — escala horizontalmente sem sticky session. O preço: **não existe canal servidor→cliente**, então o scanner do Spring AI **pula no boot** todas as tools bidirecionais (as que recebem `McpSyncRequestContext`), avisando `Stateless servers doesn't support bidirectional parameters`. Ficam de fora: sampling, elicitation, roots, progresso, logging e ping reverso.

## Testes

- `WebFluxServerIT` — handshake, tools, resources, prompts e ping pelo transporte reativo;
- `StatelessServerIT` — prova que o básico funciona sem sessão e que as tools bidirecionais realmente não são registradas.
