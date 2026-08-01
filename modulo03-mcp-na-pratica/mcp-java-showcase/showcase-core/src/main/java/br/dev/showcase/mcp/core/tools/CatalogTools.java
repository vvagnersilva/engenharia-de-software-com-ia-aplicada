package br.dev.showcase.mcp.core.tools;

import java.util.Optional;

import br.dev.showcase.mcp.core.model.ProductPage;
import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Capacidades MCP: <b>paginacao por cursor</b> e <b>erro de negocio tratado</b>.
 *
 * <p>Paginacao: o servidor devolve {@code nextCursor} e o cliente reenvia o valor
 * para pedir a proxima pagina, exatamente como as listagens do protocolo fazem.
 *
 * <p>Erro tratado: quando a regra de negocio falha, a tool devolve
 * {@code CallToolResult} com {@code isError = true} em vez de lancar excecao. A
 * diferenca importa - {@code isError} e um resultado que o modelo consegue ler e
 * corrigir, enquanto uma excecao vira erro de protocolo.
 */
@Component
public class CatalogTools {

    private final CatalogService catalog;

    public CatalogTools(CatalogService catalog) {
        this.catalog = catalog;
    }

    @McpTool(name = "showcase_list_products",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false),
            title = "Listar produtos (paginado)",
            description = "Lista o catalogo em paginas. Reenvie o campo nextCursor para obter a proxima pagina.",
            generateOutputSchema = true)
    public ProductPage listProducts(
            @McpToolParam(description = "Cursor devolvido pela chamada anterior. Vazio na primeira pagina.",
                    required = false) String cursor,
            @McpToolParam(description = "Itens por pagina, de 1 a 50. Padrao: 4", required = false)
            Integer pageSize) {

        return catalog.page(cursor, pageSize == null ? 4 : pageSize);
    }

    @McpTool(name = "showcase_reserve_stock",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false),
            title = "Reservar estoque",
            description = "Reserva unidades de um SKU. Devolve isError=true quando a regra de negocio nao permite.")
    public CallToolResult reserveStock(
            @McpToolParam(description = "SKU do produto, por exemplo SKU-001", required = true) String sku,
            @McpToolParam(description = "Quantidade a reservar", required = true) int quantity) {

        Optional<String> failure = catalog.reserve(sku, quantity);
        if (failure.isPresent()) {
            // Erro de dominio: o cliente recebe isError=true e um texto acionavel.
            return CallToolResult.builder()
                    .addTextContent("Nao foi possivel reservar: " + failure.get())
                    .isError(true)
                    .build();
        }
        return CallToolResult.builder()
                .addTextContent("Reserva confirmada: " + quantity + " unidade(s) de " + sku.toUpperCase() + ".")
                .isError(false)
                .build();
    }
}
