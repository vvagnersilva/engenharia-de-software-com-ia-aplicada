package br.dev.showcase.mcp.core.completion;

import java.util.List;

import br.dev.showcase.mcp.core.model.Product;
import br.dev.showcase.mcp.core.service.CatalogService;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>completions</b> - autocompletar de argumentos.
 *
 * <p>Funciona para dois alvos: argumentos de prompt ({@code ref/prompt}) e variaveis
 * de resource template ({@code ref/resource}). O cliente chama {@code completion/complete}
 * enquanto o usuario digita e mostra as sugestoes.
 */
@Component
public class ShowcaseCompletions {

    private static final List<String> LANGUAGES =
            List.of("java", "javascript", "typescript", "python", "go", "kotlin", "rust", "sql");
    private static final List<String> FOCUS_AREAS =
            List.of("seguranca", "performance", "legibilidade", "testes", "concorrencia");

    private final CatalogService catalog;

    public ShowcaseCompletions(CatalogService catalog) {
        this.catalog = catalog;
    }

    // ------------------------------------------------- argumentos de prompt

    @McpComplete(prompt = "code-review")
    public List<String> completeCodeReviewArgument(String prefix) {
        // O SDK entrega o valor parcial digitado; devolvemos ate 10 sugestoes.
        String needle = prefix == null ? "" : prefix.toLowerCase();
        List<String> matches = LANGUAGES.stream().filter(v -> v.startsWith(needle)).toList();
        return matches.isEmpty()
                ? FOCUS_AREAS.stream().filter(v -> v.startsWith(needle)).limit(10).toList()
                : matches.stream().limit(10).toList();
    }

    // --------------------------------------- variaveis de resource template

    @McpComplete(uri = "showcase://products/{sku}")
    public List<String> completeSku(String prefix) {
        String needle = prefix == null ? "" : prefix.toUpperCase();
        return catalog.findAll().stream()
                .map(Product::sku)
                .filter(sku -> sku.startsWith(needle))
                .limit(10)
                .toList();
    }

    @McpComplete(uri = "showcase://categories/{category}/products")
    public List<String> completeCategory(String prefix) {
        String needle = prefix == null ? "" : prefix.toLowerCase();
        return catalog.categories().stream()
                .filter(category -> category.startsWith(needle))
                .limit(10)
                .toList();
    }
}
