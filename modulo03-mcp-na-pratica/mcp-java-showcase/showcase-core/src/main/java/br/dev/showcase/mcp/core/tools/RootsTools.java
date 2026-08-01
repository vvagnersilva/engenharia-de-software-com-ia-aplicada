package br.dev.showcase.mcp.core.tools;

import java.util.List;

import br.dev.showcase.mcp.core.service.RootsRegistry;
import io.modelcontextprotocol.spec.McpSchema.ListRootsResult;
import io.modelcontextprotocol.spec.McpSchema.Root;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>roots</b> - as raizes de trabalho que o cliente expoe ao servidor.
 *
 * <p>Sao dois caminhos complementares:
 * <ul>
 *   <li>consulta ativa: {@code roots/list}, aqui via {@code context.roots()};</li>
 *   <li>notificacao passiva: {@code notifications/roots/list_changed}, tratada pelo
 *       handler registrado em {@code RootsChangeConfiguration} e acumulada no
 *       {@link RootsRegistry}.</li>
 * </ul>
 */
@Component
public class RootsTools {

    private final RootsRegistry registry;

    public RootsTools(RootsRegistry registry) {
        this.registry = registry;
    }

    @McpTool(name = "showcase_list_roots",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false),
            title = "Listar roots do cliente",
            description = "Consulta as raizes de trabalho expostas pelo cliente (roots/list) e "
                    + "mostra o ultimo evento de roots/list_changed recebido.")
    public String listRoots(McpSyncRequestContext context) {
        StringBuilder sb = new StringBuilder();

        if (!context.rootsEnabled()) {
            sb.append("O cliente nao declarou a capability de roots.\n");
        }
        else {
            ListRootsResult result = context.roots();
            List<Root> roots = result.roots();
            registry.onRootsListed(roots);

            sb.append("roots/list devolveu ").append(roots.size()).append(" raiz(es):\n");
            roots.forEach(root -> sb.append("  - ").append(root.uri())
                    .append(root.name() == null ? "" : " (" + root.name() + ")").append('\n'));
        }

        RootsRegistry.Snapshot snapshot = registry.current();
        sb.append("\nUltimo estado conhecido (origem: ").append(snapshot.source()).append(", atualizado em ")
                .append(snapshot.updatedAt() == null ? "-" : snapshot.updatedAt()).append(")\n");
        sb.append("Eventos roots/list_changed recebidos ate agora: ").append(registry.changeCount());

        return sb.toString();
    }
}
