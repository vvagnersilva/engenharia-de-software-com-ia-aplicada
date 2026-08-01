# showcase-core

As capacidades MCP do projeto, **independentes de transporte**. Os três servidores apenas apontam o component scan para `br.dev.showcase.mcp.core` e escolhem o transporte por configuração.

## O que tem aqui

| Pacote | Conteúdo |
|---|---|
| `tools/` | 14 tools anotadas com `@McpTool`: entrada simples (`GreetingTools`), objeto complexo com validação (`OrderTools`), structured output (`WeatherTools`), erro `isError` e paginação por cursor (`CatalogTools`), imagem + recurso embutido (`MediaTools`), progresso + logging (`LongRunningTools`), sampling (`SamplingTools`), elicitation (`ElicitationTools`), roots (`RootsTools`), list-changed/updated (`DynamicCatalogTools`), ping/log/capabilities (`DiagnosticsTools`) |
| `resources/` | Resources estáticos (texto, JSON e blob binário) e templates de URI (`CatalogResources`) |
| `prompts/` | Prompts sem argumento, com argumentos e multi-mensagem (`ShowcasePrompts`) |
| `completion/` | `@McpComplete` para argumento de prompt e variáveis de template (`ShowcaseCompletions`) |
| `config/` | Handler de `roots/list_changed` (SDK puro) e capabilities com `subscribe=true` |
| `service/` | Catálogo em memória e registro de roots |
| `model/` | Records que viram JSON Schema (entrada complexa, saída estruturada, página com cursor) |

## Detalhes que valem estudo

- `ProductPage` mostra o par `@Nullable` + `@JsonInclude(NON_NULL)`: sem ele, a última página falharia na validação do outputSchema.
- `RootsChangeConfiguration` documenta por que roots não tem anotação de servidor e qual é o gancho real do autoconfigure.
- `ShowcaseServerCapabilities` explica por que `subscribe` precisa ser religado manualmente.
- As tools que recebem `McpSyncRequestContext` são bidirecionais — no modo stateless o scanner as pula no boot.
