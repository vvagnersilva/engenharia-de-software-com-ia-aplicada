package br.dev.showcase.mcp.client.web;

import java.util.List;
import java.util.Map;

import br.dev.showcase.mcp.client.demo.AsyncDemoService;
import br.dev.showcase.mcp.client.demo.DemoResult;
import br.dev.showcase.mcp.client.demo.ShowcaseDemoService;
import br.dev.showcase.mcp.client.handlers.ServerRequestHandlers;
import br.dev.showcase.mcp.client.mcp.McpServerRegistry;
import br.dev.showcase.mcp.client.support.NotificationLog;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para disparar cada demonstracao manualmente.
 *
 * <pre>
 * GET  /demo                          lista as demos disponiveis
 * POST /demo/{nome}                   executa uma demo (JSON)
 * GET  /demo/{nome}/text              executa e devolve texto puro, bom para curl
 * POST /demo/async                    variante assincrona da API de cliente
 * GET  /demo/notifications            ultimas notificacoes recebidas
 * POST /demo/elicitation/next?action= define a resposta da proxima elicitation
 * </pre>
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private final ShowcaseDemoService demos;
    private final AsyncDemoService asyncDemo;
    private final McpServerRegistry registry;
    private final NotificationLog notifications;
    private final ServerRequestHandlers requestHandlers;

    public DemoController(ShowcaseDemoService demos, AsyncDemoService asyncDemo, McpServerRegistry registry,
            NotificationLog notifications, ServerRequestHandlers requestHandlers) {
        this.demos = demos;
        this.asyncDemo = asyncDemo;
        this.registry = registry;
        this.notifications = notifications;
        this.requestHandlers = requestHandlers;
    }

    @GetMapping
    public Map<String, Object> index() {
        return Map.of(
                "servidoresConectados", registry.names(),
                "demos", demos.catalog(),
                "extras", Map.of("async", "Variante assincrona da API de cliente"),
                "comoUsar", "POST /demo/{nome} ou GET /demo/{nome}/text");
    }

    @PostMapping("/{demo}")
    public DemoResult run(@PathVariable String demo) {
        return "async".equals(demo) ? asyncDemo.run() : demos.run(demo);
    }

    @GetMapping(value = "/{demo}/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public String runAsText(@PathVariable String demo) {
        return ("async".equals(demo) ? asyncDemo.run() : demos.run(demo)).asText();
    }

    @GetMapping("/notifications")
    public List<String> recentNotifications(@RequestParam(defaultValue = "50") int max) {
        return notifications.recent(max);
    }

    @PostMapping("/elicitation/next")
    public Map<String, String> nextElicitation(@RequestParam String action) {
        requestHandlers.setNextElicitationAction(ElicitResult.Action.valueOf(action.toUpperCase()));
        return Map.of("proximaResposta", requestHandlers.getNextElicitationAction().name());
    }
}
