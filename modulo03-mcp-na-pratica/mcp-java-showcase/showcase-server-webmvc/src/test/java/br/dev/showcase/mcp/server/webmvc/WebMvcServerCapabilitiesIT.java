package br.dev.showcase.mcp.server.webmvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.PromptReference;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceReference;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.Root;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste de integracao cliente-servidor real sobre <b>Streamable HTTP</b> (servlet).
 *
 * <p>Sobe a aplicacao completa em porta aleatoria e conecta um {@link McpSyncClient}
 * do SDK. Ha um metodo de teste por capacidade avancada do MCP: sampling,
 * elicitation, roots, progresso, logging com setLevel, completions, subscription
 * e paginacao.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebMvcServerCapabilitiesIT {

    @LocalServerPort
    int port;

    McpSyncClient client;

    final List<LoggingMessageNotification> logs = new CopyOnWriteArrayList<>();
    final List<ProgressNotification> progress = new CopyOnWriteArrayList<>();

    @BeforeAll
    void connect() {
        var transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();

        client = McpClient.sync(transport)
                .clientInfo(Implementation.builder("webmvc-it-client", "1.0.0").build())
                .requestTimeout(Duration.ofSeconds(30))
                .capabilities(ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .elicitation()
                        .build())
                .roots(new Root("file:///tmp/it-workspace", "workspace"))
                // Lado cliente do sampling: responde qualquer pedido com texto fixo.
                .sampling(request -> CreateMessageResult
                        .builder(Role.ASSISTANT, TextContent.builder("resumo-do-teste").build(), "it-mock-model")
                        .build())
                // Lado cliente da elicitation: aceita e preenche o formulario.
                .elicitation(request -> ElicitResult.builder(ElicitResult.Action.ACCEPT)
                        .content(Map.of("confirm", true, "reason", "teste de integracao"))
                        .build())
                .loggingConsumer(logs::add)
                .progressConsumer(progress::add)
                .build();

        InitializeResult init = client.initialize();
        assertThat(init.serverInfo().name()).isEqualTo("showcase-webmvc");
    }

    @AfterAll
    void disconnect() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    // ------------------------------------------------------------ basico

    @Test
    void capabilitiesAnunciadasIncluemTudo() {
        var capabilities = client.getServerCapabilities();
        assertThat(capabilities.tools().listChanged()).isTrue();
        assertThat(capabilities.resources().subscribe()).isTrue();
        assertThat(capabilities.prompts().listChanged()).isTrue();
        assertThat(capabilities.completions()).isNotNull();
        assertThat(capabilities.logging()).isNotNull();
    }

    @Test
    void toolsListEChamadaSimples() {
        var tools = client.listTools();
        assertThat(tools.tools()).extracting("name").contains(
                "showcase_greet", "showcase_weather_forecast", "showcase_create_order",
                "showcase_run_batch", "showcase_summarize", "showcase_discontinue_product",
                "showcase_list_roots");

        CallToolResult result = client.callTool(CallToolRequest.builder("showcase_greet")
                .arguments(Map.of("name", "Integracao", "language", "pt")).build());
        assertThat(text(result)).contains("Ola, Integracao");
    }

    @Test
    void structuredOutputSegueOSchema() {
        CallToolResult result = client.callTool(CallToolRequest.builder("showcase_weather_forecast")
                .arguments(Map.of("city", "Natal", "days", 2)).build());

        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent()).isInstanceOf(Map.class);
        Map<?, ?> structured = (Map<?, ?>) result.structuredContent();
        assertThat(structured.get("city")).isEqualTo("Natal");
        assertThat((List<?>) structured.get("days")).hasSize(2);
    }

    @Test
    void erroDeNegocioChegaComoIsError() {
        CallToolResult result = client.callTool(CallToolRequest.builder("showcase_reserve_stock")
                .arguments(Map.of("sku", "SKU-004", "quantity", 999)).build());
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("Estoque insuficiente");
    }

    @Test
    void resourcesEstaticosETemplates() {
        assertThat(client.listResources().resources()).extracting("uri")
                .contains("showcase://catalog/overview", "showcase://catalog/products.json");
        assertThat(client.listResourceTemplates().resourceTemplates()).extracting("uriTemplate")
                .contains("showcase://products/{sku}", "showcase://categories/{category}/products");

        ReadResourceResult product = client.readResource(
                ReadResourceRequest.builder("showcase://products/SKU-001").build());
        assertThat(((TextResourceContents) product.contents().get(0)).text()).contains("SKU-001");
    }

    @Test
    void promptsComEArgumentosEMultiMensagem() {
        GetPromptResult standup = client.getPrompt(
                GetPromptRequest.builder("daily-standup").arguments(Map.of()).build());
        assertThat(standup.messages()).hasSize(1);

        GetPromptResult analysis = client.getPrompt(
                GetPromptRequest.builder("sales-analysis").arguments(Map.of("threshold", 2)).build());
        assertThat(analysis.messages()).hasSizeGreaterThan(1);
    }

    // ------------------------------------------- capacidades avancadas

    @Test
    void completionsParaPromptEResourceTemplate() {
        CompleteResult prompt = client.completeCompletion(CompleteRequest.builder(
                new PromptReference("code-review"),
                new CompleteRequest.CompleteArgument("language", "ja")).build());
        assertThat(prompt.completion().values()).contains("java", "javascript");

        CompleteResult template = client.completeCompletion(CompleteRequest.builder(
                new ResourceReference("showcase://products/{sku}"),
                new CompleteRequest.CompleteArgument("sku", "SKU-00")).build());
        assertThat(template.completion().values()).isNotEmpty()
                .allMatch(value -> value.startsWith("SKU-00"));
    }

    @Test
    void samplingUsaOModeloDoCliente() {
        CallToolResult result = client.callTool(CallToolRequest.builder("showcase_summarize")
                .arguments(Map.of("text", "Um texto qualquer para resumir.", "maxSentences", 1)).build());

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("it-mock-model").contains("resumo-do-teste");
    }

    @Test
    void elicitationAceitaEExecutaAAcao() {
        CallToolResult result = client.callTool(CallToolRequest.builder("showcase_discontinue_product")
                .arguments(Map.of("sku", "SKU-002")).build());

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("descontinuado").contains("teste de integracao");
    }

    @Test
    void rootsDoClienteSaoVisiveisAoServidor() {
        CallToolResult result = client.callTool(
                CallToolRequest.builder("showcase_list_roots").arguments(Map.of()).build());
        assertThat(text(result)).contains("file:///tmp/it-workspace");
    }

    @Test
    void progressoChegaQuandoHaProgressToken() {
        progress.clear();

        client.callTool(CallToolRequest.builder("showcase_run_batch")
                .arguments(Map.of("steps", 3, "delayMillis", 10))
                .progressToken("it-token")
                .build());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(progress).hasSize(3));
        assertThat(progress).allMatch(n -> "it-token".equals(n.progressToken()));
        assertThat(progress.get(progress.size() - 1).progress()).isEqualTo(3.0);
    }

    @Test
    void logRespeitaONivelConfigurado() {
        client.setLoggingLevel(LoggingLevel.ERROR);
        logs.clear();

        client.callTool(CallToolRequest.builder("showcase_log_all_levels").arguments(Map.of()).build());

        // ERROR, CRITICAL, ALERT e EMERGENCY passam; DEBUG a WARNING sao filtrados.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(logs).hasSize(4));
        assertThat(logs).allMatch(n -> n.level().level() >= LoggingLevel.ERROR.level());

        client.setLoggingLevel(LoggingLevel.DEBUG);
        logs.clear();
        client.callTool(CallToolRequest.builder("showcase_log_all_levels").arguments(Map.of()).build());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(logs).hasSize(8));
    }

    @Test
    void paginacaoPorCursorNaTool() {
        String cursor = null;
        int pages = 0;
        int items = 0;
        do {
            var builder = CallToolRequest.builder("showcase_list_products");
            builder.arguments(cursor == null ? Map.of("pageSize", 4) : Map.of("pageSize", 4, "cursor", cursor));
            CallToolResult result = client.callTool(builder.build());
            assertThat(result.isError()).isFalse();

            Map<?, ?> structured = (Map<?, ?>) result.structuredContent();
            items += ((List<?>) structured.get("items")).size();
            cursor = (String) structured.get("nextCursor");
            pages++;
        }
        while (cursor != null && pages < 10);

        assertThat(pages).isGreaterThanOrEqualTo(3);
        assertThat(items).isGreaterThanOrEqualTo(10);
    }

    @Test
    void pingRespondido() {
        assertThat(client.ping()).isNotNull();
    }

    private static String text(CallToolResult result) {
        return result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(content -> ((TextContent) content).text())
                .findFirst()
                .orElseThrow();
    }
}
