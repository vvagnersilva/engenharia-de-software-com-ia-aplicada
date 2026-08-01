package br.dev.showcase.mcp.client.demo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.dev.showcase.mcp.client.support.NotificationLog;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.client.webflux.transport.WebClientStreamableHttpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * A variante <b>assincrona</b> da API de cliente, montada direto no SDK.
 *
 * <p>O starter do Spring AI cria clientes sync <em>ou</em> async conforme
 * {@code spring.ai.mcp.client.type}; como o resto do projeto usa sync, esta demo
 * constroi um {@link McpAsyncClient} na mao para mostrar a outra metade da API:
 * tudo devolve {@code Mono}/{@code Flux} e nada bloqueia ate o {@code block()}
 * final, que existe so para a demo caber em uma resposta HTTP.
 */
@Service
public class AsyncDemoService {

    private final String webfluxUrl;
    private final NotificationLog notifications;

    public AsyncDemoService(
            @Value("${showcase.client.webflux-url:http://localhost:8092}") String webfluxUrl,
            NotificationLog notifications) {
        this.webfluxUrl = webfluxUrl;
        this.notifications = notifications;
    }

    public DemoResult run() {
        List<String> out = new ArrayList<>();

        var transport = WebClientStreamableHttpTransport
                .builder(WebClient.builder().baseUrl(webfluxUrl))
                .endpoint("/mcp")
                .build();

        McpAsyncClient client = McpClient.async(transport)
                .clientInfo(Implementation.builder("showcase-client-async", "1.0.0").build())
                .requestTimeout(Duration.ofSeconds(30))
                .capabilities(ClientCapabilities.builder().roots(true).build())
                .loggingConsumer(notification -> {
                    notifications.record("async", "log/" + notification.level(), notification.data());
                    return Mono.empty();
                })
                .progressConsumer(notification -> {
                    notifications.record("async", "progress", String.valueOf(notification.message()));
                    return Mono.empty();
                })
                .build();

        try {
            // Encadeamento reativo: initialize -> listTools -> callTool -> ping.
            String report = client.initialize()
                    .doOnNext(init -> out.add("initialize: servidor " + init.serverInfo().name()
                            + ", protocolo " + init.protocolVersion()))
                    .then(client.listTools())
                    .doOnNext(tools -> out.add("listTools: " + tools.tools().size() + " tool(s)"))
                    .then(client.callTool(CallToolRequest.builder("showcase_greet")
                            .arguments(Map.of("name", "Mundo assincrono", "language", "pt"))
                            .build()))
                    .map(result -> result.content().stream()
                            .filter(TextContent.class::isInstance)
                            .map(content -> ((TextContent) content).text())
                            .findFirst()
                            .orElse("(sem texto)"))
                    .doOnNext(text -> out.add("callTool: " + text))
                    .flatMap(text -> client.ping().thenReturn(text))
                    .doOnNext(text -> out.add("ping: respondido"))
                    .block(Duration.ofSeconds(40));

            out.add("");
            out.add("Cadeia reativa concluida. Ultimo valor propagado: " + shorten(report));
        }
        finally {
            client.closeGracefully().block(Duration.ofSeconds(10));
        }

        return new DemoResult("async", "Cliente assincrono (SDK puro)", out, notifications.drain());
    }

    private static String shorten(String value) {
        if (value == null) {
            return "(nulo)";
        }
        String single = value.replace('\n', ' ').trim();
        return single.length() <= 80 ? single : single.substring(0, 80) + "...";
    }
}
