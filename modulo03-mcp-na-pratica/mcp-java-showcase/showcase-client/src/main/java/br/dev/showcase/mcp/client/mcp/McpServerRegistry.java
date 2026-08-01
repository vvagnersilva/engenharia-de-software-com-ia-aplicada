package br.dev.showcase.mcp.client.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.stereotype.Component;

/**
 * Da nome aos clientes MCP criados pelo starter.
 *
 * <p>O autoconfigure publica um {@code List<McpSyncClient>} sem preservar a chave
 * usada na configuracao, entao identificamos cada conexao pelo nome que o proprio
 * servidor anuncia no {@code initialize} ({@code serverInfo.name}).
 */
@Component
public class McpServerRegistry {

    private final Map<String, McpSyncClient> byServerName = new LinkedHashMap<>();

    public McpServerRegistry(List<McpSyncClient> clients) {
        clients.forEach(client -> byServerName.put(client.getServerInfo().name(), client));
    }

    /** Todos os clientes conectados, na ordem em que foram criados. */
    public Map<String, McpSyncClient> all() {
        return Map.copyOf(byServerName);
    }

    public List<String> names() {
        return List.copyOf(byServerName.keySet());
    }

    public Optional<McpSyncClient> find(String serverName) {
        return Optional.ofNullable(byServerName.get(serverName));
    }

    /**
     * Devolve o primeiro cliente disponivel, preferindo o transporte informado.
     * As demos que precisam de um unico servidor usam este atalho.
     */
    public McpSyncClient any(String preferredServerName) {
        return find(preferredServerName)
                .or(() -> byServerName.values().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhum servidor MCP conectado. Confira spring.ai.mcp.client.* no application.yml."));
    }
}
