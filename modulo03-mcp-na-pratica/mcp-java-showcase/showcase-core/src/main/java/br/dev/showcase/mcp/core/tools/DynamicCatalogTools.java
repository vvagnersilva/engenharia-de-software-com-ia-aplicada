package br.dev.showcase.mcp.core.tools;

import br.dev.showcase.mcp.core.model.Product;
import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.ResourcesUpdatedNotification;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Capacidades MCP: <b>list changed</b> e <b>resource updated</b> (subscription).
 *
 * <p>Quando o catalogo muda, o servidor avisa os clientes conectados:
 * <ul>
 *   <li>{@code notifications/resources/list_changed} - a lista de resources mudou;</li>
 *   <li>{@code notifications/resources/updated} - o conteudo de uma URI especifica mudou,
 *       entregue apenas a quem assinou aquela URI via {@code resources/subscribe}.</li>
 * </ul>
 *
 * <p>Estas notificacoes vivem no {@link McpSyncServer} do SDK, que o starter do Spring AI
 * publica como bean. Usamos {@link ObjectProvider} porque no perfil <em>stateless</em>
 * esse bean nao existe - la nao ha sessao para notificar.
 */
@Component
public class DynamicCatalogTools {

    private final CatalogService catalog;
    private final ObjectProvider<McpSyncServer> mcpServer;

    public DynamicCatalogTools(CatalogService catalog, ObjectProvider<McpSyncServer> mcpServer) {
        this.catalog = catalog;
        this.mcpServer = mcpServer;
    }

    @McpTool(name = "showcase_publish_product",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false),
            title = "Publicar produto (dispara list changed)",
            description = "Cadastra um produto novo e notifica os clientes de que os resources mudaram.")
    public String publishProduct(
            @McpToolParam(description = "SKU do novo produto, por exemplo SKU-011", required = true) String sku,
            @McpToolParam(description = "Nome comercial", required = true) String name,
            @McpToolParam(description = "Categoria", required = true) String category,
            @McpToolParam(description = "Preco em centavos", required = true) long priceCents,
            @McpToolParam(description = "Estoque inicial", required = true) int stock) {

        Product product = catalog.publish(sku.toUpperCase(), name, category.toLowerCase(), priceCents, stock);

        McpSyncServer server = mcpServer.getIfAvailable();
        if (server == null) {
            return "Produto " + product.sku() + " publicado. "
                    + "Servidor em modo stateless: nao ha sessao para notificar.";
        }

        // A lista de resources mudou (o template ganhou um SKU novo).
        server.notifyResourcesListChanged();
        // E o conteudo destas URIs especificas mudou, para quem assinou.
        server.notifyResourcesUpdated(new ResourcesUpdatedNotification("showcase://catalog/overview"));
        server.notifyResourcesUpdated(new ResourcesUpdatedNotification("showcase://catalog/products.json"));

        return "Produto " + product.sku() + " publicado. Notificacoes enviadas: "
                + "resources/list_changed e resources/updated para "
                + "showcase://catalog/overview e showcase://catalog/products.json.";
    }
}
