package br.dev.showcase.mcp.core.tools;

import java.util.List;

import br.dev.showcase.mcp.core.model.OrderRequest;
import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da tool de entrada complexa: validacao devolve isError=true com todos os
 * problemas listados; caminho feliz devolve structuredContent.
 */
class OrderToolsTests {

    private OrderTools tools;

    @BeforeEach
    void setUp() {
        tools = new OrderTools(new CatalogService());
    }

    @Test
    void pedidoInvalidoListaTodosOsProblemasComIsError() {
        OrderRequest order = new OrderRequest(
                new OrderRequest.Customer("", "sem-arroba"),
                List.of(new OrderRequest.Item("SKU-999", 0)),
                null);

        CallToolResult result = tools.createOrder(order);

        assertThat(result.isError()).isTrue();
        String text = text(result);
        assertThat(text).contains("customer.name");
        assertThat(text).contains("customer.email");
        assertThat(text).contains("quantity");
        assertThat(text).contains("SKU-999");
    }

    @Test
    void pedidoSemItensERejeitado() {
        OrderRequest order = new OrderRequest(
                new OrderRequest.Customer("Ana", "ana@example.com"), List.of(), null);

        CallToolResult result = tools.createOrder(order);

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("pelo menos um item");
    }

    @Test
    void pedidoValidoDevolveStructuredContentComTotais() {
        OrderRequest order = new OrderRequest(
                new OrderRequest.Customer("Ana", "ana@example.com"),
                List.of(new OrderRequest.Item("SKU-001", 2)),
                "sem observacoes");

        CallToolResult result = tools.createOrder(order);

        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent()).isNotNull();
        assertThat(text(result)).contains("criado");
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }
}
