package br.dev.showcase.mcp.server.webflux;

import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracao sobre o transporte <b>Streamable HTTP reativo</b> (WebFlux/Netty).
 *
 * <p>As capacidades avancadas ja sao cobertas uma a uma no modulo webmvc; aqui o
 * foco e provar que o mesmo core funciona por este transporte.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebFluxServerIT {

    @LocalServerPort
    int port;

    @Test
    void handshakeToolsResourcesEPingFuncionam() {
        var transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(Implementation.builder("webflux-it-client", "1.0.0").build())
                .requestTimeout(Duration.ofSeconds(30))
                .build();
        try {
            InitializeResult init = client.initialize();
            assertThat(init.serverInfo().name()).isEqualTo("showcase-webflux");

            assertThat(client.listTools().tools()).extracting("name").contains("showcase_greet");
            assertThat(client.listResources().resources()).isNotEmpty();
            assertThat(client.listPrompts().prompts()).isNotEmpty();

            CallToolResult result = client.callTool(CallToolRequest.builder("showcase_greet")
                    .arguments(Map.of("name", "WebFlux")).build());
            assertThat(((TextContent) result.content().get(0)).text()).contains("WebFlux");

            assertThat(client.ping()).isNotNull();
        }
        finally {
            client.closeGracefully();
        }
    }
}
