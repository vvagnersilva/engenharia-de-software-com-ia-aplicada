# showcase-server-stdio

Servidor MCP com transporte **STDIO**: JSON-RPC por stdin/stdout, o formato que Claude Desktop, Claude Code e MCP Inspector usam para subir servidores locais como subprocesso.

## Regra de ouro do STDIO

O stdout pertence ao protocolo. Por isso o `application.yml`:

- desliga o banner e o log de console (`logging.pattern.console: ""`);
- manda o log para arquivo (`SHOWCASE_LOG_FILE`, padrão `./logs/showcase-server-stdio.log`);
- não sobe servidor web (`spring.main.web-application-type: none`).

## Rodar

```bash
../mvnw -pl showcase-server-stdio -am package
java -jar target/showcase-server-stdio-1.0.0-SNAPSHOT.jar   # fica aguardando JSON-RPC no stdin
```

Teste rápido com o Inspector:

```bash
npx @modelcontextprotocol/inspector java -jar target/showcase-server-stdio-1.0.0-SNAPSHOT.jar
```

Config para o Claude Desktop no [README raiz](../README.md#plugando-nos-clientes-mcp).

## Docker

```bash
docker build -f showcase-server-stdio/Dockerfile -t mcp-showcase-stdio .   # na raiz
docker run -i mcp-showcase-stdio                                           # -i e obrigatorio
```

## Teste

`StdioServerIT` sobe o jar como subprocesso real e conversa pelo transporte (fase `integration-test`, depois do `package`).
