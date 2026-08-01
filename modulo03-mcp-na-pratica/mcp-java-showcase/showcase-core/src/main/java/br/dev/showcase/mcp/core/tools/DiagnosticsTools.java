package br.dev.showcase.mcp.core.tools;

import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

/**
 * Capacidades MCP: <b>ping</b>, <b>logging por nivel</b> e leitura das
 * <b>capabilities do cliente</b>.
 */
@Component
public class DiagnosticsTools {

    @McpTool(name = "showcase_ping_client",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false),
            title = "Ping no cliente",
            description = "Envia um ping do servidor para o cliente e mede o tempo de resposta.")
    public String pingClient(McpSyncRequestContext context) {
        long start = System.nanoTime();
        context.ping();
        long millis = (System.nanoTime() - start) / 1_000_000;
        return "Ping respondido pelo cliente em " + millis + " ms (sessao " + context.sessionId() + ").";
    }

    @McpTool(name = "showcase_log_all_levels",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false),
            title = "Emitir log em todos os niveis",
            description = "Emite uma mensagem em cada nivel do MCP. O cliente so recebe as que estao "
                    + "acima do nivel configurado via logging/setLevel.")
    public String logAllLevels(McpSyncRequestContext context) {
        for (LoggingLevel level : LoggingLevel.values()) {
            context.log(spec -> spec.level(level)
                    .logger("showcase.diagnostics")
                    .message("Mensagem de teste no nivel " + level.name()));
        }
        return "Foram emitidas " + LoggingLevel.values().length + " mensagens, uma por nivel. "
                + "Use logging/setLevel no cliente para mudar o corte e repita a chamada.";
    }

    @McpTool(name = "showcase_client_capabilities",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false),
            title = "Capacidades do cliente conectado",
            description = "Mostra o que o cliente conectado declarou suportar: sampling, elicitation e roots.")
    public String clientCapabilities(McpSyncRequestContext context) {
        ClientCapabilities capabilities = context.clientCapabilities();
        StringBuilder sb = new StringBuilder();
        sb.append("Cliente: ").append(context.clientInfo() == null ? "desconhecido" : context.clientInfo().name())
                .append('\n');
        sb.append("sampling: ").append(supported(capabilities != null && capabilities.sampling() != null)).append('\n');
        sb.append("elicitation: ").append(supported(capabilities != null && capabilities.elicitation() != null))
                .append('\n');
        sb.append("roots: ").append(supported(capabilities != null && capabilities.roots() != null)).append('\n');
        sb.append("\nVisao do request context (funciona tambem no modo stateless):\n");
        sb.append("  sampleEnabled=").append(context.sampleEnabled())
                .append(", elicitEnabled=").append(context.elicitEnabled())
                .append(", rootsEnabled=").append(context.rootsEnabled());
        return sb.toString();
    }

    private static String supported(boolean value) {
        return value ? "suportado" : "nao suportado";
    }
}
