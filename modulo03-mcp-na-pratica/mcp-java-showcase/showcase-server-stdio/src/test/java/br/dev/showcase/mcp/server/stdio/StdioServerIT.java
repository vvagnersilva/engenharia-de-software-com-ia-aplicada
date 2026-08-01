package br.dev.showcase.mcp.server.stdio;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integracao sobre o transporte <b>STDIO</b>: sobe o jar empacotado como
 * subprocesso e conversa JSON-RPC por stdin/stdout - exatamente o que o Claude
 * Desktop faz.
 *
 * <p>Roda na fase integration-test (failsafe), depois do package, entao o jar
 * ja existe em target/. Se nao existir (ex.: execucao direta pela IDE antes do
 * package), o teste e pulado com aviso em vez de falhar.
 */
class StdioServerIT {

    @Test
    void handshakeEChamadaDeToolPorStdio() {
        File jar = new File("target/showcase-server-stdio-1.0.0-SNAPSHOT.jar");
        assumeTrue(jar.isFile(), "Jar nao encontrado; rode mvn verify para empacotar antes");

        ServerParameters parameters = ServerParameters.builder("java")
                .args("-jar", jar.getAbsolutePath())
                .build();
        var transport = new StdioClientTransport(parameters, McpJsonDefaults.getMapper());

        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(Implementation.builder("stdio-it-client", "1.0.0").build())
                .requestTimeout(Duration.ofSeconds(60))
                .initializationTimeout(Duration.ofSeconds(60))
                .build();
        try {
            InitializeResult init = client.initialize();
            assertThat(init.serverInfo().name()).isEqualTo("showcase-stdio");

            assertThat(client.listTools().tools()).extracting("name").contains("showcase_greet");

            CallToolResult result = client.callTool(CallToolRequest.builder("showcase_greet")
                    .arguments(Map.of("name", "STDIO", "language", "en")).build());
            assertThat(((TextContent) result.content().get(0)).text()).contains("Hello, STDIO");

            assertThat(client.ping()).isNotNull();
        }
        finally {
            client.closeGracefully();
        }
    }
}
