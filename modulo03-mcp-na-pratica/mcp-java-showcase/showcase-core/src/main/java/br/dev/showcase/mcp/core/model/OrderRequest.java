package br.dev.showcase.mcp.core.model;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Entrada complexa e aninhada de uma tool.
 *
 * <p>O gerador de schema do Spring AI transforma este record (e os records aninhados)
 * no {@code inputSchema} JSON Schema exposto pelo MCP, incluindo objetos e arrays.
 *
 * @param customer dados do cliente
 * @param items    itens do pedido (pelo menos um)
 * @param notes    observacoes livres. Marcado com {@code @Nullable} para sair de
 *                 {@code required} no JSON Schema gerado
 */
public record OrderRequest(Customer customer, List<Item> items, @Nullable String notes) {

    /**
     * @param name  nome do cliente
     * @param email e-mail para envio da confirmacao
     */
    public record Customer(String name, String email) {
    }

    /**
     * @param sku      SKU do produto
     * @param quantity quantidade desejada (maior que zero)
     */
    public record Item(String sku, int quantity) {
    }
}
