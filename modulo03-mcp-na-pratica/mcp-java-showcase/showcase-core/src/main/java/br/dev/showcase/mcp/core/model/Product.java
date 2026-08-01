package br.dev.showcase.mcp.core.model;

/**
 * Produto do catalogo de demonstracao.
 *
 * @param sku        identificador unico
 * @param name       nome comercial
 * @param category   categoria
 * @param priceCents preco em centavos (evita ponto flutuante em dinheiro)
 * @param stock      quantidade disponivel
 */
public record Product(String sku, String name, String category, long priceCents, int stock) {
}
