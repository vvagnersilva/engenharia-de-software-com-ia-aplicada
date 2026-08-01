package br.dev.showcase.mcp.core.tools;

import br.dev.showcase.mcp.core.service.CatalogService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.ai.mcp.annotation.context.StructuredElicitResult;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>elicitation</b> - o servidor pede dados ou confirmacao ao usuario
 * no meio da execucao de uma tool.
 *
 * <p>O cliente responde com uma das tres acoes do protocolo, e todas precisam ser
 * tratadas:
 * <ul>
 *   <li>{@code ACCEPT} - o usuario preencheu o formulario;</li>
 *   <li>{@code DECLINE} - o usuario recusou explicitamente;</li>
 *   <li>{@code CANCEL} - o usuario fechou o dialogo sem decidir.</li>
 * </ul>
 */
@Component
public class ElicitationTools {

    private final CatalogService catalog;

    public ElicitationTools(CatalogService catalog) {
        this.catalog = catalog;
    }

    /**
     * Formulario pedido ao usuario. O schema JSON enviado ao cliente e derivado
     * deste record.
     *
     * @param confirm     confirmacao explicita da exclusao
     * @param reason      motivo registrado na auditoria
     */
    public record DeletionConfirmation(boolean confirm, String reason) {
    }

    @McpTool(name = "showcase_discontinue_product",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
                    idempotentHint = true, openWorldHint = false),
            title = "Descontinuar produto (pede confirmacao)",
            description = "Marca um produto como descontinuado, pedindo confirmacao ao usuario via elicitation.")
    public String discontinueProduct(
            McpSyncRequestContext context,
            @McpToolParam(description = "SKU do produto a descontinuar", required = true) String sku) {

        var product = catalog.findBySku(sku);
        if (product.isEmpty()) {
            return "SKU desconhecido: " + sku + ".";
        }

        if (!context.elicitEnabled()) {
            context.warn("Cliente sem suporte a elicitation; a operacao exige confirmacao e foi abortada.");
            return "Operacao cancelada: este cliente nao suporta elicitation, "
                    + "entao nao ha como confirmar a exclusao com o usuario.";
        }

        StructuredElicitResult<DeletionConfirmation> answer = context.elicit(
                spec -> spec.message("Confirmar a descontinuacao de " + product.get().sku()
                        + " (" + product.get().name() + ")? Informe tambem o motivo."),
                DeletionConfirmation.class);

        return switch (answer.action()) {
            case ACCEPT -> handleAccept(product.get().sku(), answer.structuredContent(), context);
            case DECLINE -> {
                context.info("Usuario recusou a descontinuacao de " + product.get().sku() + ".");
                yield "Descontinuacao recusada pelo usuario. Nada foi alterado.";
            }
            case CANCEL -> {
                context.info("Usuario cancelou o dialogo de descontinuacao.");
                yield "Dialogo cancelado pelo usuario. Nada foi alterado.";
            }
        };
    }

    private String handleAccept(String sku, DeletionConfirmation data, McpSyncRequestContext context) {
        if (data == null || !data.confirm()) {
            return "O usuario respondeu ao formulario, mas nao marcou a confirmacao. Nada foi alterado.";
        }
        String reason = (data.reason() == null || data.reason().isBlank())
                ? "sem motivo informado" : data.reason();
        context.info("Produto " + sku + " descontinuado. Motivo: " + reason);
        return "Produto " + sku + " marcado como descontinuado. Motivo registrado: " + reason;
    }
}
