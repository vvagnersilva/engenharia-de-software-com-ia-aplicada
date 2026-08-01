package br.dev.showcase.mcp.core.completion;

import br.dev.showcase.mcp.core.service.CatalogService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShowcaseCompletionsTests {

    private final ShowcaseCompletions completions = new ShowcaseCompletions(new CatalogService());

    @Test
    void autocompletarDeLinguagemFiltraPeloPrefixo() {
        assertThat(completions.completeCodeReviewArgument("ja")).containsExactly("java", "javascript");
    }

    @Test
    void prefixoSemLinguagemCaiNosFocos() {
        assertThat(completions.completeCodeReviewArgument("seg")).containsExactly("seguranca");
    }

    @Test
    void autocompletarDeSkuIgnoraCaixa() {
        assertThat(completions.completeSku("sku-00")).hasSize(9).allMatch(sku -> sku.startsWith("SKU-00"));
    }

    @Test
    void autocompletarDeCategoriaFiltraPeloPrefixo() {
        assertThat(completions.completeCategory("mo")).containsExactly("mobiliario", "monitores");
    }

    @Test
    void prefixoVazioDevolveAteDezSugestoes() {
        assertThat(completions.completeSku("")).hasSizeLessThanOrEqualTo(10);
    }
}
