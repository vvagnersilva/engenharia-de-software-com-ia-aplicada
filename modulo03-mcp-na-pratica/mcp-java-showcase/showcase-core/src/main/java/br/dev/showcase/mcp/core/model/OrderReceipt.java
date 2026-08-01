package br.dev.showcase.mcp.core.model;

import java.util.List;

/**
 * Saida estruturada da tool de criacao de pedido.
 *
 * @param orderId    identificador gerado
 * @param customer   nome do cliente
 * @param lines      linhas do pedido ja precificadas
 * @param totalCents total em centavos
 */
public record OrderReceipt(String orderId, String customer, List<Line> lines, long totalCents) {

    /**
     * @param sku            SKU do produto
     * @param name           nome do produto
     * @param quantity       quantidade
     * @param unitPriceCents preco unitario em centavos
     * @param subtotalCents  subtotal da linha
     */
    public record Line(String sku, String name, int quantity, long unitPriceCents, long subtotalCents) {
    }
}
