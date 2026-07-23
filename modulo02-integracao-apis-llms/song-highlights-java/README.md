# Song Highlights Java

Porte para **Spring Boot** do projeto [`04-song-highlights-z`](../04-song-highlights-z) (Node.js/TypeScript + LangGraph). Mesmo domínio - um assistente conversacional que recomenda músicas e aprende as preferências do usuário ao longo da conversa - mas exposto como **API REST** em vez de um loop de chat em CLI: cada turno de conversa é uma chamada HTTP.

## Por que este projeto existe

O projeto original usa um grafo de estados do LangGraph (`chat -> savePreferences/summarize -> end`) rodando dentro de um processo de CLI de longa duração, com memória de conversa via checkpointer do LangGraph. Aqui, como cada chamada REST é isolada (sem processo de longa duração mantendo estado em memória), o histórico de conversa e as preferências do usuário são persistidos no Postgres e recarregados a cada requisição, com a mesma lógica de merge/sumarização do original reproduzida como código Java explícito (ver `ChatOrchestrationService`).

## Arquitetura (mapeamento em relação ao projeto TypeScript)

| TypeScript (`04-song-highlights-z`) | Java (`song-highlights-java`) |
|---|---|
| `graph/graph.ts` (StateGraph + roteamento condicional) | `service/ChatOrchestrationService` (orquestração explícita com `if`s) |
| `graph/nodes/chatNode.ts` | `ChatOrchestrationService.handleTurn` + `prompt/ChatPrompts` |
| `graph/nodes/savePreferencesNode.ts` | `PreferencesService.mergePreferences` |
| `graph/nodes/summarizationNode.ts` | `service/SummarizationService` |
| `services/preferencesService.ts` (SQLite/Knex, tabela `user_preferences`) | `service/PreferencesService` (JPA, entidade `UserPreferencesEntity`) |
| Checkpointer do LangGraph (Postgres, por `thread_id`) | Tabela `conversation_messages` (JPA), por `userId` |
| `services/openrouterService.ts` (`generateStructured`) | `llm/StructuredChatClient` + `OpenRouterStructuredChatClient` (Spring AI `ChatClient`) |
| `src/index.ts` (loop de CLI, `graph.invoke()` por linha) | `controller/ChatController` (`POST /api/v1/chat` por turno) |

## Simplificações assumidas nesta conversão

- **Modelo único, sem fallback.** O projeto original usa recursos proprietários do OpenRouter (`models: [...]` como lista de fallback e `provider.sort`). O Spring AI não expõe esses campos nativamente, então esta conversão usa um único modelo fixo (`spring.ai.openai.chat.options.model`, padrão `nvidia/nemotron-3-super-120b-a12b:free`). Se o fallback multi-modelo for necessário, a alternativa é substituir `OpenRouterStructuredChatClient` por um cliente HTTP (`RestClient`/`WebClient`) próprio, chamando a API do OpenRouter diretamente.
- **Postgres próprio, não compartilhado.** Este projeto tem seu próprio `docker-compose.yml` (porta `5433`, banco `song_highlights_java`) em vez de reaproveitar o container do `04-song-highlights-z` (porta `5432`), para que os dois exemplos continuem independentes.
- **`favoriteGenres`/`favoriteBands` como JSON em texto**, igual ao `table.json(...)` do Knex no projeto original - não usa o tipo nativo `jsonb` do Postgres.

## Contrato REST

### `POST /api/v1/chat`

Cada chamada é um turno da conversa - equivalente a uma linha digitada no CLI original.

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"userId": "erickwendel", "message": "Oi! Meu nome é Erick e eu amo rock"}'
```

```json
{
  "userId": "erickwendel",
  "reply": "E aí, Erick! Rock é demais! ...",
  "preferencesUpdated": true,
  "conversationSummarized": false
}
```

### `GET /api/v1/preferences/{userId}`

```bash
curl http://localhost:8080/api/v1/preferences/erickwendel
```

Retorna `200` com as preferências salvas, ou `404` se o usuário ainda não tiver nenhuma.

## Rodando localmente

1. Suba o Postgres deste projeto (não é o mesmo container do `04-song-highlights-z`):
   ```bash
   docker compose up -d
   ```
2. Configure o `.env` (copie de `.env.example`) com sua `OPENROUTER_API_KEY`.
3. Rode a aplicação:
   ```bash
   OPENROUTER_API_KEY=... mvn spring-boot:run
   ```
4. Rode os testes (usam H2 em memória via perfil `test`, não precisam do Postgres/Docker rodando):
   ```bash
   mvn test
   ```

## Testando com curl

Com o Postgres e a aplicação de pé (ver "Rodando localmente" acima), este é um roteiro de teste manual que reproduz o mesmo tipo de conversa multi-turno que o CLI original fazia - mas cada linha vira uma chamada HTTP independente.

**1. Primeiro turno - o usuário se apresenta:**

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"userId": "erickwendel", "message": "Oi! Meu nome é Erick e eu amo rock"}'
```

```json
{
  "userId": "erickwendel",
  "reply": "E aí, Erick! Rock é demais! Que bandas você curte? Recomendo \"Everlong\" do Foo Fighters!",
  "preferencesUpdated": true,
  "conversationSummarized": false
}
```

**2. Segundo turno - nova chamada HTTP, mesmo `userId`, sem nenhum estado do lado do cliente. O servidor recarrega sozinho o que já sabe sobre o Erick:**

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"userId": "erickwendel", "message": "Pode recomendar mais músicas parecidas?"}'
```

A resposta deve levar em conta o rock mencionado no primeiro turno, mesmo sem o cliente reenviar esse contexto.

**3. Conferir as preferências persistidas:**

```bash
curl http://localhost:8080/api/v1/preferences/erickwendel
```

```json
{
  "userId": "erickwendel",
  "name": "Erick",
  "age": null,
  "favoriteGenres": ["rock"],
  "favoriteBands": [],
  "keyPreferences": null,
  "importantContext": null
}
```

**4. Usuário sem preferências salvas ainda - retorna `404`:**

```bash
curl -i http://localhost:8080/api/v1/preferences/usuario-que-nao-existe
```

## Estrutura

```
src/main/java/com/songhighlights/
  controller/ChatController.java         # POST /api/v1/chat, GET /api/v1/preferences/{userId}
  service/
    ChatOrchestrationService.java        # substitui o StateGraph
    PreferencesService.java              # merge de preferências (JPA)
    SummarizationService.java            # sumarização + trim do histórico
  llm/
    StructuredChatClient.java            # interface (mockável em teste)
    OpenRouterStructuredChatClient.java  # implementação via Spring AI ChatClient
    LlmChatResponse.java, UserPreferencesData.java, ConversationSummaryData.java
  prompt/
    ChatPrompts.java                     # porte literal de chatResponse.ts
    SummarizationPrompts.java            # porte literal de summarization.ts
  entity/, repository/, dto/
```
