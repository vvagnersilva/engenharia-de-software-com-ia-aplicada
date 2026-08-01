package br.dev.showcase.mcp.core.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import br.dev.showcase.mcp.core.model.OrderReceipt;
import br.dev.showcase.mcp.core.model.OrderRequest;
import br.dev.showcase.mcp.core.model.Product;
import br.dev.showcase.mcp.core.service.CatalogService;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>tool com entrada complexa e validacao</b>.
 *
 * <p>O parametro e um record aninhado ({@link OrderRequest}) com objeto e array,
 * o que exercita a geracao completa de JSON Schema. A validacao e explicita e
 * devolve {@code isError = true} com a lista de problemas, no lugar de excecao -
 * assim o modelo consegue corrigir a chamada sozinho.
 *
 * <p>No caminho feliz a tool devolve saida estruturada montada a mao, para mostrar
 * como combinar {@code structuredContent} com {@code CallToolResult} customizado.
 */
@Component
public class OrderTools {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CatalogService catalog;

    public OrderTools(CatalogService catalog) {
        this.catalog = catalog;
    }

    @McpTool(name = "showcase_create_order",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false),
            title = "Criar pedido",
            description = "Cria um pedido a partir de um objeto complexo (cliente + itens) e valida os dados.")
    public CallToolResult createOrder(
            @McpToolParam(description = "Pedido completo, com dados do cliente e a lista de itens",
                    required = true) OrderRequest order) {

        List<String> problems = validate(order);
        if (!problems.isEmpty()) {
            return CallToolResult.builder()
                    .addTextContent("Pedido rejeitado:\n- " + String.join("\n- ", problems))
                    .isError(true)
                    .build();
        }

        List<OrderReceipt.Line> lines = new ArrayList<>();
        long total = 0;
        for (OrderRequest.Item item : order.items()) {
            Product product = catalog.findBySku(item.sku()).orElseThrow();
            long subtotal = product.priceCents() * item.quantity();
            total += subtotal;
            lines.add(new OrderReceipt.Line(product.sku(), product.name(), item.quantity(),
                    product.priceCents(), subtotal));
        }

        OrderReceipt receipt = new OrderReceipt(catalog.nextOrderId(), order.customer().name(), lines, total);

        return CallToolResult.builder()
                .addTextContent("Pedido " + receipt.orderId() + " criado. Total: R$ "
                        + CatalogService.formatPrice(receipt.totalCents()))
                .structuredContent(receipt)
                .isError(false)
                .build();
    }

    private List<String> validate(OrderRequest order) {
        List<String> problems = new ArrayList<>();
        if (order == null) {
            return List.of("O corpo do pedido e obrigatorio.");
        }
        if (order.customer() == null) {
            problems.add("customer e obrigatorio.");
        }
        else {
            if (isBlank(order.customer().name())) {
                problems.add("customer.name e obrigatorio.");
            }
            if (isBlank(order.customer().email()) || !EMAIL.matcher(order.customer().email()).matches()) {
                problems.add("customer.email precisa ser um e-mail valido.");
            }
        }
        if (order.items() == null || order.items().isEmpty()) {
            problems.add("items precisa ter pelo menos um item.");
            return problems;
        }
        for (int i = 0; i < order.items().size(); i++) {
            OrderRequest.Item item = order.items().get(i);
            String prefix = "items[" + i + "]";
            if (item == null || isBlank(item.sku())) {
                problems.add(prefix + ".sku e obrigatorio.");
                continue;
            }
            if (item.quantity() <= 0) {
                problems.add(prefix + ".quantity precisa ser maior que zero.");
            }
            Optional<Product> product = catalog.findBySku(item.sku());
            if (product.isEmpty()) {
                problems.add(prefix + ".sku desconhecido: " + item.sku() + ".");
            }
            else if (product.get().stock() < item.quantity()) {
                problems.add(prefix + " sem estoque: disponivel " + product.get().stock()
                        + ", solicitado " + item.quantity() + ".");
            }
        }
        return problems;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
