package br.dev.showcase.mcp.client.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import br.dev.showcase.mcp.client.handlers.ServerRequestHandlers;
import br.dev.showcase.mcp.client.mcp.McpServerRegistry;
import br.dev.showcase.mcp.client.support.NotificationLog;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import io.modelcontextprotocol.spec.McpSchema.EmbeddedResource;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.ListResourcesResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptReference;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceReference;
import io.modelcontextprotocol.spec.McpSchema.Root;
import io.modelcontextprotocol.spec.McpSchema.SubscribeRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.UnsubscribeRequest;
import org.springframework.stereotype.Service;

/**
 * Executa, uma a uma, as demonstracoes de cada capacidade do MCP.
 *
 * <p>Cada metodo publico corresponde a um item de {@link #catalog()} e pode ser
 * disparado pela API REST ou pela CLI. O relatorio devolvido inclui as notificacoes
 * que chegaram do servidor durante a execucao.
 */
@Service
public class ShowcaseDemoService {

    private static final String DEFAULT_SERVER = "showcase-webmvc";

    private final McpServerRegistry registry;
    private final NotificationLog notifications;
    private final ServerRequestHandlers requestHandlers;

    public ShowcaseDemoService(McpServerRegistry registry, NotificationLog notifications,
            ServerRequestHandlers requestHandlers) {
        this.registry = registry;
        this.notifications = notifications;
        this.requestHandlers = requestHandlers;
    }

    /** Nome da demo -> descricao curta. Alimenta a listagem do REST e da CLI. */
    public Map<String, String> catalog() {
        Map<String, String> demos = new LinkedHashMap<>();
        demos.put("servers", "Servidores conectados, capabilities e instructions");
        demos.put("tools", "Listagem de tools e chamada simples");
        demos.put("structured-output", "Tool com outputSchema e structuredContent");
        demos.put("error-handling", "Erro de negocio devolvido com isError=true");
        demos.put("complex-input", "Tool com objeto aninhado e validacao");
        demos.put("media", "Tool devolvendo imagem e recurso embutido");
        demos.put("resources", "Resources estaticos e resource templates");
        demos.put("subscription", "resources/subscribe e notificacoes de mudanca");
        demos.put("prompts", "Prompts com e sem argumentos, incluindo multi-mensagem");
        demos.put("completions", "Autocompletar de argumento de prompt e de template");
        demos.put("sampling", "Servidor pedindo geracao ao LLM do cliente");
        demos.put("elicitation", "Servidor pedindo confirmacao: ACCEPT, DECLINE e CANCEL");
        demos.put("roots", "roots/list e notifications/roots/list_changed");
        demos.put("progress-logging", "Progresso e log estruturado com filtro de nivel");
        demos.put("pagination", "Drenagem de listagens por cursor");
        demos.put("ping", "Ping em todos os servidores conectados");
        return demos;
    }

    // ------------------------------------------------------------------ demos

    public DemoResult servers() {
        List<String> out = new ArrayList<>();
        registry.all().forEach((name, client) -> {
            out.add("servidor: " + name + " v" + client.getServerInfo().version());
            out.add("  protocolo negociado: " + client.getCurrentInitializationResult().protocolVersion());
            out.add("  capabilities: " + client.getServerCapabilities());
            out.add("  instructions: " + shorten(client.getServerInstructions(), 100));
            out.add("");
        });
        return result("servers", "Servidores conectados", out);
    }

    public DemoResult tools() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        List<Tool> tools = drainTools(client);
        out.add("tools/list devolveu " + tools.size() + " tool(s):");
        tools.forEach(tool -> out.add("  - " + tool.name() + " :: " + tool.title()));

        CallToolResult greeting = client.callTool(call("showcase_greet", Map.of("name", "Wagner", "language", "pt")));
        out.add("");
        out.add("chamada de showcase_greet -> " + text(greeting));
        return result("tools", "Tools", out);
    }

    public DemoResult structuredOutput() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        CallToolResult forecast = client.callTool(call("showcase_weather_forecast", Map.of("city", "Curitiba", "days", 3)));

        List<String> out = new ArrayList<>();
        out.add("content textual: " + shorten(text(forecast), 120));
        out.add("structuredContent: " + forecast.structuredContent());
        out.add("");
        out.add("O outputSchema declarado na tool permite ao cliente validar a resposta");
        out.add("antes de entregar ao modelo.");
        return result("structured-output", "Saida estruturada", out);
    }

    public DemoResult errorHandling() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        CallToolResult ok = client.callTool(call("showcase_reserve_stock", Map.of("sku", "SKU-002", "quantity", 1)));
        out.add("reserva valida   -> isError=" + ok.isError() + " | " + text(ok));

        CallToolResult failure = client.callTool(call("showcase_reserve_stock", Map.of("sku", "SKU-004", "quantity", 999)));
        out.add("reserva invalida -> isError=" + failure.isError() + " | " + text(failure));
        out.add("");
        out.add("isError=true e resultado de negocio, nao erro de protocolo: o modelo le a");
        out.add("mensagem e pode tentar outra chamada.");
        return result("error-handling", "Erro tratado", out);
    }

    public DemoResult complexInput() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        Map<String, Object> invalid = Map.of("order", Map.of(
                "customer", Map.of("name", "", "email", "invalido"),
                "items", List.of(Map.of("sku", "SKU-999", "quantity", 0))));
        CallToolResult rejected = client.callTool(call("showcase_create_order", invalid));
        out.add("pedido invalido -> isError=" + rejected.isError());
        out.add(text(rejected));
        out.add("");

        Map<String, Object> valid = Map.of("order", Map.of(
                "customer", Map.of("name", "Wagner", "email", "wagner@example.com"),
                "items", List.of(Map.of("sku", "SKU-001", "quantity", 2)),
                "notes", "entrega expressa"));
        CallToolResult accepted = client.callTool(call("showcase_create_order", valid));
        out.add("pedido valido -> " + text(accepted));
        out.add("structuredContent: " + accepted.structuredContent());
        return result("complex-input", "Entrada complexa e validacao", out);
    }

    public DemoResult media() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        CallToolResult chart = client.callTool(call("showcase_stock_chart", Map.of("category", "perifericos")));

        List<String> out = new ArrayList<>();
        out.add("A resposta trouxe " + chart.content().size() + " bloco(s) de conteudo:");
        for (Content content : chart.content()) {
            switch (content) {
                case TextContent text -> out.add("  - text: " + shorten(text.text(), 80));
                case ImageContent image -> out.add("  - image: " + image.mimeType() + ", "
                        + image.data().length() + " caracteres em base64");
                case EmbeddedResource embedded -> out.add("  - resource embutido: "
                        + embedded.resource().uri() + " (" + embedded.resource().mimeType() + ")");
                default -> out.add("  - " + content.getClass().getSimpleName());
            }
        }
        return result("media", "Conteudo nao textual", out);
    }

    public DemoResult resources() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        List<Resource> staticResources = drainResources(client);
        out.add("resources/list (" + staticResources.size() + "):");
        staticResources.forEach(resource -> out.add("  - " + resource.uri() + " [" + resource.mimeType() + "]"));

        out.add("");
        out.add("resources/templates/list:");
        client.listResourceTemplates().resourceTemplates()
                .forEach(template -> out.add("  - " + template.uriTemplate() + " :: " + template.title()));

        out.add("");
        ReadResourceResult overview = client.readResource(
                ReadResourceRequest.builder("showcase://catalog/overview").build());
        out.add("leitura do resource estatico showcase://catalog/overview:");
        out.add(shorten(firstText(overview), 200));

        out.add("");
        ReadResourceResult product = client.readResource(
                ReadResourceRequest.builder("showcase://products/SKU-003").build());
        out.add("leitura via template showcase://products/{sku} com sku=SKU-003:");
        out.add(firstText(product));
        return result("resources", "Resources e templates", out);
    }

    public DemoResult subscription() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        notifications.drain();
        List<String> out = new ArrayList<>();

        String uri = "showcase://catalog/overview";
        client.subscribeResource(SubscribeRequest.builder(uri).build());
        out.add("assinado: " + uri);

        String sku = "SKU-" + (900 + (int) (System.currentTimeMillis() % 90));
        CallToolResult published = client.callTool(call("showcase_publish_product", Map.of("sku", sku, "name", "Produto de teste " + sku, "category", "acessorios",
                        "priceCents", 4900, "stock", 7)));
        out.add("publicado: " + text(published));

        sleep(700);
        client.unsubscribeResource(UnsubscribeRequest.builder(uri).build());
        out.add("assinatura encerrada");
        out.add("");
        out.add("Repare que resources/updated so chega para a URI assinada, enquanto");
        out.add("resources/list_changed vai para todos os clientes.");
        return result("subscription", "Subscription de resources", out);
    }

    public DemoResult prompts() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        List<Prompt> available = drainPrompts(client);
        out.add("prompts/list (" + available.size() + "):");
        available.forEach(prompt -> out.add("  - " + prompt.name() + " argumentos="
                + (prompt.arguments() == null ? "[]" : prompt.arguments().stream().map(a -> a.name()).toList())));

        out.add("");
        GetPromptResult standup = client.getPrompt(GetPromptRequest.builder("daily-standup").arguments(Map.of()).build());
        out.add("prompt sem argumentos (daily-standup): " + standup.messages().size() + " mensagem(ns)");
        out.add("  " + shorten(promptText(standup), 120));

        out.add("");
        GetPromptResult review = client.getPrompt(
                GetPromptRequest.builder("code-review").arguments(Map.of("language", "java", "focus", "seguranca")).build());
        out.add("prompt com argumentos (code-review): " + review.description());

        out.add("");
        GetPromptResult analysis = client.getPrompt(
                GetPromptRequest.builder("sales-analysis").arguments(Map.of("threshold", 4)).build());
        out.add("prompt multi-mensagem (sales-analysis): " + analysis.messages().size() + " mensagens");
        analysis.messages().forEach(message -> out.add("  [" + message.role() + "] "
                + shorten(message.content() instanceof TextContent t ? t.text() : "", 70)));
        return result("prompts", "Prompts", out);
    }

    public DemoResult completions() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        CompleteResult language = client.completeCompletion(CompleteRequest.builder(
                new PromptReference("code-review"),
                new CompleteRequest.CompleteArgument("language", "ja")).build());
        out.add("argumento de prompt 'language' com prefixo 'ja' -> " + language.completion().values());

        CompleteResult sku = client.completeCompletion(CompleteRequest.builder(
                new ResourceReference("showcase://products/{sku}"),
                new CompleteRequest.CompleteArgument("sku", "SKU-01")).build());
        out.add("variavel de template 'sku' com prefixo 'SKU-01' -> " + sku.completion().values());

        CompleteResult category = client.completeCompletion(CompleteRequest.builder(
                new ResourceReference("showcase://categories/{category}/products"),
                new CompleteRequest.CompleteArgument("category", "mo")).build());
        out.add("variavel de template 'category' com prefixo 'mo' -> " + category.completion().values());
        return result("completions", "Completions", out);
    }

    public DemoResult sampling() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        notifications.drain();

        CallToolResult summary = client.callTool(call("showcase_summarize", Map.of(
                "text", "O Model Context Protocol padroniza como aplicacoes de IA conversam com "
                        + "ferramentas e dados. Servidores expoem tools, resources e prompts. "
                        + "Clientes conectam modelos a esses servidores. O transporte pode ser "
                        + "STDIO ou HTTP.",
                "maxSentences", 2)));

        List<String> out = new ArrayList<>();
        out.add("A tool do servidor pediu uma geracao ao cliente (sampling/createMessage).");
        out.add("Resposta devolvida ao servidor e repassada como resultado da tool:");
        out.add("");
        out.add(text(summary));
        return result("sampling", "Sampling", out);
    }

    public DemoResult elicitation() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        for (ElicitResult.Action action : ElicitResult.Action.values()) {
            requestHandlers.setNextElicitationAction(action);
            CallToolResult response = client.callTool(call("showcase_discontinue_product", Map.of("sku", "SKU-006")));
            out.add("cliente respondendo " + action + " -> " + text(response));
        }
        requestHandlers.setNextElicitationAction(ElicitResult.Action.ACCEPT);
        return result("elicitation", "Elicitation", out);
    }

    public DemoResult roots() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        notifications.drain();
        List<String> out = new ArrayList<>();

        CallToolResult before = client.callTool(call("showcase_list_roots", Map.of()));
        out.add("estado inicial:");
        out.add(text(before));

        String newRoot = "file:///tmp/showcase-root-extra";
        client.addRoot(new Root(newRoot, "extra"));
        client.rootsListChangedNotification();
        out.add("");
        out.add("root adicionado no cliente e notificacao roots/list_changed enviada: " + newRoot);
        sleep(500);

        CallToolResult after = client.callTool(call("showcase_list_roots", Map.of()));
        out.add("");
        out.add("estado apos a notificacao:");
        out.add(text(after));

        client.removeRoot(newRoot);
        client.rootsListChangedNotification();
        return result("roots", "Roots", out);
    }

    public DemoResult progressAndLogging() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        client.setLoggingLevel(LoggingLevel.DEBUG);
        out.add("logging/setLevel = DEBUG");
        notifications.drain();

        CallToolResult batch = client.callTool(CallToolRequest.builder("showcase_run_batch")
                .arguments(Map.of("steps", 4, "delayMillis", 120))
                .progressToken("demo-" + System.currentTimeMillis())
                .build());
        out.add("resultado: " + text(batch));
        List<String> withDebug = notifications.drain();

        client.setLoggingLevel(LoggingLevel.WARNING);
        out.add("");
        out.add("logging/setLevel = WARNING e nova chamada de showcase_log_all_levels");
        client.callTool(call("showcase_log_all_levels", Map.of()));
        sleep(300);
        List<String> withWarning = notifications.drain();

        out.add("");
        out.add("notificacoes com nivel DEBUG: " + withDebug.size());
        withDebug.forEach(line -> out.add("  " + line));
        out.add("");
        out.add("notificacoes com nivel WARNING (as de debug/info sumiram): " + withWarning.size());
        withWarning.forEach(line -> out.add("  " + line));

        client.setLoggingLevel(LoggingLevel.INFO);
        return new DemoResult("progress-logging", "Progresso e logging", out, List.of());
    }

    public DemoResult pagination() {
        McpSyncClient client = registry.any(DEFAULT_SERVER);
        List<String> out = new ArrayList<>();

        out.add("Listagens do protocolo, drenadas por cursor:");
        out.add("  tools     : " + drainTools(client).size());
        out.add("  resources : " + drainResources(client).size());
        out.add("  prompts   : " + drainPrompts(client).size());
        out.add("");
        out.add("Observacao honesta: o servidor do MCP Java SDK 2.0.0 responde as listagens");
        out.add("em uma unica pagina (nextCursor nulo). O laco acima e o comportamento correto");
        out.add("de um cliente e continua valendo se o servidor passar a paginar.");
        out.add("");
        out.add("Paginacao real, no nivel do dominio, via tool showcase_list_products:");

        String cursor = null;
        int page = 0;
        do {
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("pageSize", 4);
            if (cursor != null) {
                arguments.put("cursor", cursor);
            }
            CallToolResult response = client.callTool(call("showcase_list_products", arguments));
            if (Boolean.TRUE.equals(response.isError())) {
                out.add("  ERRO na pagina " + (page + 1) + ": " + text(response));
                break;
            }
            Map<?, ?> structured = (Map<?, ?>) response.structuredContent();
            List<?> items = (List<?>) structured.get("items");
            cursor = (String) structured.get("nextCursor");
            page++;
            out.add("  pagina " + page + ": " + items.size() + " item(ns), nextCursor=" + cursor);
        }
        while (cursor != null && page < 20);

        return result("pagination", "Paginacao", out);
    }

    public DemoResult ping() {
        List<String> out = new ArrayList<>();
        registry.all().forEach((name, client) -> {
            long start = System.nanoTime();
            client.ping();
            out.add(name + " respondeu em " + ((System.nanoTime() - start) / 1_000_000) + " ms");
        });
        return result("ping", "Ping", out);
    }

    // -------------------------------------------------------------- despacho

    public DemoResult run(String demo) {
        return switch (demo) {
            case "servers" -> servers();
            case "tools" -> tools();
            case "structured-output" -> structuredOutput();
            case "error-handling" -> errorHandling();
            case "complex-input" -> complexInput();
            case "media" -> media();
            case "resources" -> resources();
            case "subscription" -> subscription();
            case "prompts" -> prompts();
            case "completions" -> completions();
            case "sampling" -> sampling();
            case "elicitation" -> elicitation();
            case "roots" -> roots();
            case "progress-logging" -> progressAndLogging();
            case "pagination" -> pagination();
            case "ping" -> ping();
            default -> throw new IllegalArgumentException(
                    "Demo desconhecida: " + demo + ". Validas: " + catalog().keySet());
        };
    }

    // ------------------------------------------------------------ auxiliares

    /**
     * Percorre todas as paginas de {@code tools/list} seguindo o {@code nextCursor}.
     * O mesmo padrao vale para resources e prompts.
     */
    private List<Tool> drainTools(McpSyncClient client) {
        List<Tool> all = new ArrayList<>();
        String cursor = null;
        do {
            ListToolsResult page = cursor == null ? client.listTools() : client.listTools(cursor);
            all.addAll(page.tools());
            cursor = page.nextCursor();
        }
        while (cursor != null);
        return all;
    }

    private List<Resource> drainResources(McpSyncClient client) {
        List<Resource> all = new ArrayList<>();
        String cursor = null;
        do {
            ListResourcesResult page = cursor == null ? client.listResources() : client.listResources(cursor);
            all.addAll(page.resources());
            cursor = page.nextCursor();
        }
        while (cursor != null);
        return all;
    }

    private List<Prompt> drainPrompts(McpSyncClient client) {
        List<Prompt> all = new ArrayList<>();
        String cursor = null;
        do {
            ListPromptsResult page = cursor == null ? client.listPrompts() : client.listPrompts(cursor);
            all.addAll(page.prompts());
            cursor = page.nextCursor();
        }
        while (cursor != null);
        return all;
    }

    /** Atalho para montar a requisicao de tool call sem repetir o builder. */
    private static CallToolRequest call(String name, Map<String, Object> arguments) {
        return CallToolRequest.builder(name).arguments(arguments).build();
    }

    private DemoResult result(String demo, String title, List<String> output) {
        return new DemoResult(demo, title, output, notifications.drain());
    }

    private static String text(CallToolResult result) {
        return result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(content -> ((TextContent) content).text())
                .findFirst()
                .orElse("(sem conteudo textual)");
    }

    private static String firstText(ReadResourceResult result) {
        return result.contents().stream()
                .filter(TextResourceContents.class::isInstance)
                .map(contents -> ((TextResourceContents) contents).text())
                .findFirst()
                .orElse("(conteudo binario)");
    }

    private static String promptText(GetPromptResult result) {
        return result.messages().stream()
                .map(message -> message.content() instanceof TextContent text ? text.text() : "")
                .findFirst()
                .orElse("");
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "(nulo)";
        }
        String single = value.replace('\n', ' ').trim();
        return single.length() <= max ? single : single.substring(0, max) + "...";
    }

    private static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
