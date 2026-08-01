package br.dev.showcase.mcp.core.service;

import java.util.ArrayList;
import java.util.List;

import br.dev.showcase.mcp.core.model.Product;
import br.dev.showcase.mcp.core.model.ProductPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitarios do catalogo, com foco na paginacao por cursor - a mesma
 * semantica das listagens do protocolo MCP.
 */
class CatalogServiceTests {

    private CatalogService catalog;

    @BeforeEach
    void setUp() {
        catalog = new CatalogService();
    }

    @Test
    void paginacaoPercorreTodoOCatalogoSemRepetirItens() {
        List<Product> collected = new ArrayList<>();
        String cursor = null;
        int pages = 0;

        do {
            ProductPage page = catalog.page(cursor, 3);
            collected.addAll(page.items());
            cursor = page.nextCursor();
            pages++;
        }
        while (cursor != null && pages < 20);

        assertThat(collected).hasSize(catalog.findAll().size());
        assertThat(collected.stream().map(Product::sku).distinct()).hasSize(collected.size());
        assertThat(pages).isEqualTo(4); // 10 itens em paginas de 3
    }

    @Test
    void ultimaPaginaNaoTemCursor() {
        ProductPage page = catalog.page(null, 50);
        assertThat(page.nextCursor()).isNull();
        assertThat(page.total()).isEqualTo(page.items().size());
    }

    @Test
    void cursorInvalidoFalhaComMensagemClara() {
        assertThatThrownBy(() -> catalog.page("cursor-que-nao-existe", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor invalido");
    }

    @Test
    void reservaRespeitaEstoqueEQuantidade() {
        assertThat(catalog.reserve("SKU-002", 1)).isEmpty();
        assertThat(catalog.reserve("SKU-004", 1)).hasValueSatisfying(
                message -> assertThat(message).contains("Estoque insuficiente"));
        assertThat(catalog.reserve("SKU-002", 0)).hasValueSatisfying(
                message -> assertThat(message).contains("maior que zero"));
        assertThat(catalog.reserve("SKU-INEXISTENTE", 1)).hasValueSatisfying(
                message -> assertThat(message).contains("SKU desconhecido"));
    }

    @Test
    void reservaDebitaEstoque() {
        int before = catalog.findBySku("SKU-001").orElseThrow().stock();
        catalog.reserve("SKU-001", 2);
        assertThat(catalog.findBySku("SKU-001").orElseThrow().stock()).isEqualTo(before - 2);
    }
}
