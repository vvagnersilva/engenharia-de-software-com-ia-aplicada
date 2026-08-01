# Mapa de capacidades MCP → código

Cada recurso do MCP Java SDK, onde ele está implementado neste projeto e qual teste o prova.
Convenção de caminhos: `core` = `showcase-core/src/main/java/br/dev/showcase/mcp/core`,
`client` = `showcase-client/src/main/java/br/dev/showcase/mcp/client`.

## Lado servidor

| Recurso MCP | Mensagem(ns) do protocolo | Onde está | Teste que prova |
|---|---|---|---|
| Tool simples (entrada primitiva) | `tools/list`, `tools/call` | `core/tools/GreetingTools.java` → `showcase_greet` | `WebMvcServerCapabilitiesIT.toolsListEChamadaSimples` |
| Tool com objeto complexo + validação | `tools/call` | `core/tools/OrderTools.java` → `showcase_create_order` (entrada `model/OrderRequest.java`) | `OrderToolsTests`, demo `complex-input` |
| Structured output (outputSchema) | `tools/call` → `structuredContent` | `core/tools/WeatherTools.java` → `showcase_weather_forecast` (`generateOutputSchema = true`, saída `model/ForecastReport.java`) | `WebMvcServerCapabilitiesIT.structuredOutputSegueOSchema` |
| Erro de negócio tratado (`isError`) | `tools/call` → `isError: true` | `core/tools/CatalogTools.java` → `showcase_reserve_stock` | `WebMvcServerCapabilitiesIT.erroDeNegocioChegaComoIsError` |
| Conteúdo não textual (imagem + recurso embutido) | `tools/call` → `ImageContent`, `EmbeddedResource` | `core/tools/MediaTools.java` → `showcase_stock_chart` | `MediaToolsTests.imagemEUmPngValidoEmBase64` |
| Tool longa com progresso | `notifications/progress` | `core/tools/LongRunningTools.java` → `showcase_run_batch` (`@McpProgressToken` + `context.progress(...)`) | `WebMvcServerCapabilitiesIT.progressoChegaQuandoHaProgressToken` |
| Resources estáticos | `resources/list`, `resources/read` | `core/resources/CatalogResources.java` → `showcase://catalog/overview`, `products.json`, `logo.png` (binário/blob) | `WebMvcServerCapabilitiesIT.resourcesEstaticosETemplates` |
| Resource templates (URI parametrizada) | `resources/templates/list`, `resources/read` | `core/resources/CatalogResources.java` → `showcase://products/{sku}`, `showcase://categories/{category}/products` | idem |
| Resource list changed | `notifications/resources/list_changed` | `core/tools/DynamicCatalogTools.java` → `McpSyncServer.notifyResourcesListChanged()` | demo `subscription` |
| Resource subscription (updated) | `resources/subscribe`, `notifications/resources/updated` | `core/tools/DynamicCatalogTools.java` → `notifyResourcesUpdated(...)`; capability ligada em `core/config/ShowcaseServerCapabilities.java` | demo `subscription` (só a URI assinada é notificada) |
| Prompt sem argumentos | `prompts/list`, `prompts/get` | `core/prompts/ShowcasePrompts.java` → `daily-standup` | `ShowcasePromptsTests` |
| Prompt com argumentos | `prompts/get` | `core/prompts/ShowcasePrompts.java` → `code-review` (`@McpArg` required/opcional) | `ShowcasePromptsTests` |
| Prompt multi-mensagem | `prompts/get` | `core/prompts/ShowcasePrompts.java` → `sales-analysis` (ASSISTANT + 2× USER) | `ShowcasePromptsTests.promptMultiMensagemAlternaPapeis` |
| Completion de argumento de prompt | `completion/complete` (`ref/prompt`) | `core/completion/ShowcaseCompletions.java` → `@McpComplete(prompt = "code-review")` | `WebMvcServerCapabilitiesIT.completionsParaPromptEResourceTemplate` |
| Completion de variável de template | `completion/complete` (`ref/resource`) | `core/completion/ShowcaseCompletions.java` → `@McpComplete(uri = "showcase://products/{sku}")` | idem |
| Logging estruturado com nível | `notifications/message`, `logging/setLevel` | `core/tools/DiagnosticsTools.java` → `showcase_log_all_levels`; o corte por nível é feito pelo SDK | `WebMvcServerCapabilitiesIT.logRespeitaONivelConfigurado` |
| Sampling (servidor pede geração) | `sampling/createMessage` | `core/tools/SamplingTools.java` → `showcase_summarize` (`ModelPreferences` + fallback sem capability) | `WebMvcServerCapabilitiesIT.samplingUsaOModeloDoCliente` |
| Elicitation (ACCEPT/DECLINE/CANCEL) | `elicitation/create` | `core/tools/ElicitationTools.java` → `showcase_discontinue_product` (`context.elicit(...)` tipado) | `WebMvcServerCapabilitiesIT.elicitationAceitaEExecutaAAcao` + demo `elicitation` (3 ações) |
| Roots (consulta) | `roots/list` | `core/tools/RootsTools.java` → `showcase_list_roots` (`context.roots()`) | `WebMvcServerCapabilitiesIT.rootsDoClienteSaoVisiveisAoServidor` |
| Roots (notificação de mudança) | `notifications/roots/list_changed` | `core/config/RootsChangeConfiguration.java` (bean `BiConsumer` consumido pelo autoconfigure) + `core/service/RootsRegistry.java` | demo `roots` |
| Ping (servidor → cliente) | `ping` | `core/tools/DiagnosticsTools.java` → `showcase_ping_client` (`context.ping()`) | demo `ping` |
| Ping (cliente → servidor) | `ping` | respondido pelo SDK; exercitado em `client/demo/ShowcaseDemoService.java#ping` | `WebMvcServerCapabilitiesIT.pingRespondido` |
| Paginação por cursor (domínio) | padrão `nextCursor` | `core/tools/CatalogTools.java` → `showcase_list_products` + `core/service/CatalogService.java#page` | `CatalogServiceTests`, `WebMvcServerCapabilitiesIT.paginacaoPorCursorNaTool` |
| Paginação por cursor (listagens do protocolo) | `tools/resources/prompts/list` com `cursor` | cliente drena com `listTools(cursor)` em `client/demo/ShowcaseDemoService.java#drainTools` (o servidor do SDK 2.0.0 responde em página única — ver nota abaixo) | demo `pagination` |
| Tool annotations (hints) | `readOnlyHint`, `destructiveHint`, ... | todas as tools do core via `@McpTool.McpAnnotations` | inspecionável em `tools/list` |
| Capabilities do servidor | `initialize` | `core/config/ShowcaseServerCapabilities.java` + customizers por app (`McpCapabilitiesConfiguration.java` em cada servidor) | `WebMvcServerCapabilitiesIT.capabilitiesAnunciadasIncluemTudo` |
| Instructions do servidor | `initialize` → `instructions` | `application.yml` de cada servidor (`spring.ai.mcp.server.instructions`) | demo `servers` |

## Lado cliente

| Recurso MCP | Onde está | Teste/demonstração |
|---|---|---|
| Cliente sync multi-servidor (STDIO + 2× HTTP) | `showcase-client/src/main/resources/application.yml` (`spring.ai.mcp.client.*.connections`) + `client/mcp/McpServerRegistry.java` | demo `servers` |
| Cliente async (SDK puro, `Mono`/`Flux`) | `client/demo/AsyncDemoService.java` (`McpClient.async` + `WebClientStreamableHttpTransport`) | demo `async` |
| Handler de sampling | `client/handlers/ServerRequestHandlers.java` → `@McpSampling` + `client/sampling/*` (mock por padrão, LLM real no perfil `real-llm`) | demo `sampling` |
| Handler de elicitation | `client/handlers/ServerRequestHandlers.java` → `@McpElicitation` (responde ACCEPT/DECLINE/CANCEL sob controle da API REST) | demo `elicitation` |
| Declaração de roots + `roots/list_changed` | `client/config/McpRootsConfiguration.java` (customizer por conexão) + `McpSyncClient.addRoot`/`rootsListChangedNotification` na demo | demo `roots` |
| Recebimento de log | `client/handlers/ServerNotificationHandlers.java` → `@McpLogging` | demo `progress-logging` |
| Recebimento de progresso | `client/handlers/ServerNotificationHandlers.java` → `@McpProgress` | demo `progress-logging` |
| Tool/resource/prompt list changed | `client/handlers/ServerNotificationHandlers.java` → `@McpToolListChanged` etc. | demo `subscription` |
| `logging/setLevel` | `McpSyncClient.setLoggingLevel(...)` em `client/demo/ShowcaseDemoService.java#progressAndLogging` | demo `progress-logging` |
| `resources/subscribe` / `unsubscribe` | `client/demo/ShowcaseDemoService.java#subscription` | demo `subscription` |

## Transportes

| Transporte | Módulo | Observação |
|---|---|---|
| STDIO | `showcase-server-stdio` | stdout reservado ao protocolo; log em arquivo. Testado em `StdioServerIT` subindo o jar como subprocesso |
| Streamable HTTP (servlet, síncrono) | `showcase-server-webmvc` | endpoint único `/mcp` (POST/GET/DELETE). Testado em `WebMvcServerCapabilitiesIT` |
| Streamable HTTP (reativo) | `showcase-server-webflux` | Netty/WebFlux. Testado em `WebFluxServerIT` |
| Stateless (sem sessão) | `showcase-server-webflux`, perfil `stateless` | tools bidirecionais são **puladas no boot** pelo scanner (sem sessão não há canal de volta). Testado em `StatelessServerIT` |
| SSE clássico (legado) | `showcase-server-webmvc`, perfil `sse` | depreciado pelo protocolo desde 2025-03-26; mantido só para clientes antigos |

## Notas honestas

- **Paginação das listagens do protocolo**: o servidor do MCP Java SDK 2.0.0 responde `tools/list`, `resources/list` e `prompts/list` em página única (`nextCursor` nulo). O cliente deste projeto implementa o laço de drenagem correto (que continua válido se o servidor paginar), e a semântica completa de cursor é demonstrada no nível de domínio pela tool `showcase_list_products`.
- **Roots via anotação**: não existe anotação de servidor para roots no Spring AI 2.0.0 (as `@Mcp*ListChanged` são do lado cliente). Por isso `RootsChangeConfiguration` usa o gancho do autoconfigure (bean `BiConsumer<McpSyncServerExchange, List<Root>>`), que chega ao `rootsChangeHandler` do SDK puro.
- **`subscribe` de resources**: o autoconfigure do Spring AI 2.0.0 fixa `subscribe=false` nas capabilities (não há propriedade). Os módulos de servidor religam via `McpSyncServerCustomizer` — no webmvc o customizer precisa ser `@Primary` e reaplicar `immediateExecution(true)` porque o starter servlet registra um customizer próprio e o autoconfigure aceita apenas um.
- **Modo ASYNC de servidor**: os três servidores rodam `type: SYNC` porque os callbacks async do Spring AI 2.0.0 só injetam `McpAsyncRequestContext` — um core único sync não roda nos dois modos. A API async é demonstrada no **cliente** (`AsyncDemoService`).
