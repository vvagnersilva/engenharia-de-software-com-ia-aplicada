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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracao do perfil <b>stateless</b>: sem sessao, cada requisicao independente.
 *
 * <p>O que este teste prova, alem do basico funcionar: as tools que recebem
 * {@code McpSyncRequestContext} (sampling, elicitation, roots, progresso, logging)
 * <b>nem sao registradas</b> - o scanner do Spring AI as pula no boot com o aviso
 * "Stateless servers doesn't support bidirectional parameters", porque sem sessao
 * nao existe canal servidor -> cliente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("stateless")
class StatelessServerIT {

    @LocalServerPort
    int port;

    @Test
    void toolsFuncionamEBidirecionaisFicamDeFora() {
        var transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();

        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(Implementation.builder("stateless-it-client", "1.0.0").build())
                .requestTimeout(Duration.ofSeconds(30))
                .build();
        try {
            InitializeResult init = client.initialize();
            assertThat(init.serverInfo().name()).isEqualTo("showcase-webflux-stateless");

            // O basico funciona normalmente sem sessao.
            CallToolResult greet = client.callTool(CallToolRequest.builder("showcase_greet")
                    .arguments(Map.of("name", "Stateless")).build());
            assertThat(text(greet)).contains("Stateless");

            assertThat(client.listResources().resources()).isNotEmpty();
            assertThat(client.listPrompts().prompts()).isNotEmpty();

            // As tools bidirecionais nao existem neste modo: o scanner as pulou no boot.
            var toolNames = client.listTools().tools().stream().map(tool -> tool.name()).toList();
            assertThat(toolNames)
                    .contains("showcase_greet", "showcase_weather_forecast", "showcase_list_products")
                    .doesNotContain("showcase_summarize", "showcase_discontinue_product",
                            "showcase_list_roots", "showcase_run_batch");
        }
        finally {
            client.closeGracefully();
        }
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }
}
