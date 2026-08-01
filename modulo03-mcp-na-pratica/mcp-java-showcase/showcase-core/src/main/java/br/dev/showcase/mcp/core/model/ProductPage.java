package br.dev.showcase.mcp.core.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Pagina de resultados no estilo cursor do MCP.
 *
 * <p>O protocolo MCP pagina por cursor opaco: o servidor devolve {@code nextCursor}
 * e o cliente reenvia esse valor para pedir a proxima pagina. Na ultima pagina o
 * campo simplesmente nao aparece - a dupla de anotacoes garante isso:
 * <ul>
 *   <li>{@code @Nullable} tira {@code nextCursor} do {@code required} no
 *       outputSchema gerado;</li>
 *   <li>{@code @JsonInclude(NON_NULL)} omite o campo na serializacao - sem ela o
 *       JSON traria {@code "nextCursor": null} e a validacao de saida do servidor
 *       rejeitaria a ultima pagina ({@code null} nao e {@code string}).</li>
 * </ul>
 *
 * @param items      itens da pagina
 * @param nextCursor cursor da proxima pagina, ausente na ultima
 * @param total      total de itens em todas as paginas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductPage(List<Product> items, @Nullable String nextCursor, int total) {
}
