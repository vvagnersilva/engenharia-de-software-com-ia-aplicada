package br.dev.showcase.mcp.client.handlers;

import java.util.List;

import br.dev.showcase.mcp.client.support.NotificationLog;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.ai.mcp.annotation.McpPromptListChanged;
import org.springframework.ai.mcp.annotation.McpResourceListChanged;
import org.springframework.ai.mcp.annotation.McpToolListChanged;
import org.springframework.stereotype.Component;

/**
 * Lado do <b>cliente</b> das notificacoes que o servidor empurra.
 *
 * <p>Sao mensagens sem resposta (JSON-RPC notifications) que chegam fora do fluxo
 * de requisicao. Cada uma e registrada no {@link NotificationLog} para a API REST
 * conseguir mostrar depois o que aconteceu durante uma demo.
 */
@Component
public class ServerNotificationHandlers {

    private static final Logger log = LoggerFactory.getLogger(ServerNotificationHandlers.class);

    private final NotificationLog notifications;

    public ServerNotificationHandlers(NotificationLog notifications) {
        this.notifications = notifications;
    }

    /** {@code notifications/message} - log estruturado vindo do servidor. */
    @McpLogging(clients = { "stdio", "webmvc", "webflux" })
    public void onLogMessage(LoggingMessageNotification notification) {
        log.info("[MCP log] {} {}: {}", notification.level(), notification.logger(), notification.data());
        notifications.record("*", "log/" + notification.level(),
                notification.logger() + ": " + notification.data());
    }

    /** {@code notifications/progress} - andamento de uma chamada longa. */
    @McpProgress(clients = { "stdio", "webmvc", "webflux" })
    public void onProgress(ProgressNotification notification) {
        String percent = notification.total() == null ? "?"
                : "%.0f%%".formatted(100.0 * notification.progress() / notification.total());
        log.info("[MCP progress] {} - {}", percent, notification.message());
        notifications.record("*", "progress", percent + " " + notification.message());
    }

    /** {@code notifications/tools/list_changed}. */
    @McpToolListChanged(clients = { "stdio", "webmvc", "webflux" })
    public void onToolsChanged(List<Tool> tools) {
        notifications.record("*", "tools/list_changed", tools.size() + " tool(s) agora disponiveis");
    }

    /** {@code notifications/resources/list_changed}. */
    @McpResourceListChanged(clients = { "stdio", "webmvc", "webflux" })
    public void onResourcesChanged(List<Resource> resources) {
        notifications.record("*", "resources/list_changed", resources.size() + " resource(s) agora disponiveis");
    }

    /** {@code notifications/prompts/list_changed}. */
    @McpPromptListChanged(clients = { "stdio", "webmvc", "webflux" })
    public void onPromptsChanged(List<Prompt> prompts) {
        notifications.record("*", "prompts/list_changed", prompts.size() + " prompt(s) agora disponiveis");
    }
}
