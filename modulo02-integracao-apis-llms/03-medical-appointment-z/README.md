# Medical Appointment Assistant

Assistente de agendamento de consultas médicas construído com **LangGraph**, **LangChain** e a API da **OpenRouter**, exposto via servidor HTTP (Fastify).

## Como funciona

O usuário envia uma mensagem em linguagem natural (ex: "Quero agendar uma consulta com Dra. Ana para amanhã às 14h") e o grafo:

1. **identifyIntent**: identifica a intenção (`schedule`, `cancel` ou `unknown`) e extrai os dados necessários (profissional, data/hora, paciente, motivo) usando saída estruturada com Zod.
2. Roteia condicionalmente para **schedule** ou **cancel**, que validam os campos obrigatórios e executam a operação no banco de dados (SQLite).
3. **message**: gera uma resposta amigável para o paciente com base no resultado da operação.

```
START → identifyIntent ─┬→ schedule ─┐
                        ├→ cancel ───┼→ message → END
                        └→ (unknown) ┘
```

## Estrutura do projeto

```
src/
  ├── config.ts                     # Configuração (env vars, modelo, provider)
  ├── index.ts                      # Ponto de entrada do servidor HTTP
  ├── server.ts                     # Servidor Fastify (rota POST /chat)
  ├── graph/
  │   ├── graph.ts                  # StateGraph com edges condicionais
  │   ├── factory.ts                # Construção do grafo (DI dos serviços)
  │   └── nodes/
  │       ├── identifyIntentNode.ts # Classificação de intenção (LLM estruturado)
  │       ├── schedulerNode.ts      # Agendamento de consultas
  │       ├── cancellerNode.ts      # Cancelamento de consultas
  │       └── messageGeneratorNode.ts # Geração da mensagem final ao paciente
  ├── prompts/v1/                   # Prompts (system/user) por funcionalidade
  ├── services/
  │   ├── appointmentService.ts     # Acesso a dados de profissionais/consultas
  │   └── openRouterService.ts      # Cliente LLM (OpenRouter via LangChain)
  └── database/
      ├── database.ts               # Conexão SQLite
      └── seed.ts                   # Popula o banco com dados de exemplo

tests/
  └── router.e2e.test.ts            # Testes e2e do fluxo de agendamento/cancelamento
```

## Instalação

```bash
npm install
```

## Configuração

Copie `.env.example` para `.env` e preencha com suas credenciais:

```env
OPENROUTER_API_KEY=sk-or-v1-...
LANGCHAIN_API_KEY=...
LANGCHAIN_TRACING_V2=true
LANGCHAIN_PROJECT=03-medical-appointment
```

## Uso

```bash
# Inicia o servidor HTTP
npm start

# Modo desenvolvimento (watch)
npm run dev

# Servir o grafo via LangGraph CLI (com tracing/Studio)
npm run langgraph:serve
```

Exemplo de requisição:

```bash
curl -X POST \
  -H 'Content-type: application/json' \
  --data '{"question": "Quero agendar uma consulta com a Dra. Ana amanhã às 14h, meu nome é Maria Santos"}' \
  localhost:3000/chat
```

## Testes

```bash
npm test
npm run test:e2e
```

## Node Version

Requer Node.js >= 24.10.0.

## License

MIT
