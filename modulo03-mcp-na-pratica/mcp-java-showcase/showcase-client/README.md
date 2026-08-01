# showcase-client

Cliente MCP Spring Boot que conecta **simultaneamente nos três servidores** (STDIO como subprocesso + dois Streamable HTTP) e exercita todas as capacidades do protocolo.

## Duas interfaces

**API REST** (porta 8090):

```bash
java -jar target/showcase-client-1.0.0-SNAPSHOT.jar

curl http://localhost:8090/demo                          # lista as 16 demos + async
curl -X POST http://localhost:8090/demo/sampling         # executa (JSON)
curl http://localhost:8090/demo/roots/text               # executa (texto puro)
curl http://localhost:8090/demo/notifications            # notificacoes recebidas
curl -X POST 'http://localhost:8090/demo/elicitation/next?action=DECLINE'  # proxima resposta de elicitation
```

**CLI** (roda e sai, exit code 0/1):

```bash
java -jar target/showcase-client-1.0.0-SNAPSHOT.jar --demo=all
java -jar target/showcase-client-1.0.0-SNAPSHOT.jar --demo=elicitation --demo=roots
```

## Perfis

| Perfil | Efeito |
|---|---|
| *(nenhum)* | `local-stdio` entra como default: conecta também no servidor STDIO subindo o jar como subprocesso |
| `http-only` | Só as conexões HTTP — é o usado no docker compose. (A conexão STDIO nasce em perfil próprio porque mapas de `connections` de perfis diferentes são mesclados, não substituídos) |
| `real-llm` | Sampling com LLM real via `ANTHROPIC_API_KEY` (senão, provedor mockado — zero chave para rodar) |

## Onde está o lado cliente de cada capacidade

| Capacidade | Classe |
|---|---|
| Sampling (`@McpSampling`) | `handlers/ServerRequestHandlers` + `sampling/*` |
| Elicitation (`@McpElicitation`, ACCEPT/DECLINE/CANCEL controláveis) | `handlers/ServerRequestHandlers` |
| Log, progresso e list-changed (`@McpLogging`, `@McpProgress`, `@Mcp*ListChanged`) | `handlers/ServerNotificationHandlers` |
| Roots por conexão (customizer) | `config/McpRootsConfiguration` |
| API sync (starter) | `demo/ShowcaseDemoService` |
| API async (SDK puro, `Mono`/`Flux`) | `demo/AsyncDemoService` |

Variáveis de ambiente: `SHOWCASE_WEBMVC_URL`, `SHOWCASE_WEBFLUX_URL`, `SHOWCASE_STDIO_JAR`, `SHOWCASE_WORKSPACE`, `SERVER_PORT`, `ANTHROPIC_API_KEY`.
