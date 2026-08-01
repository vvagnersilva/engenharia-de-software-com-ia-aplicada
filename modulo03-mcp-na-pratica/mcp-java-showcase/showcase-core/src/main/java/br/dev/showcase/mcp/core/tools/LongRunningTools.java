package br.dev.showcase.mcp.core.tools;

import java.util.concurrent.TimeUnit;

import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import org.springframework.ai.mcp.annotation.McpProgressToken;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

/**
 * Capacidades MCP: <b>notificacoes de progresso</b> e <b>logging estruturado</b>.
 *
 * <p>O cliente so recebe {@code notifications/progress} se enviar um
 * {@code progressToken} no {@code _meta} da chamada. O parametro anotado com
 * {@link McpProgressToken} recebe esse token (ou {@code null} quando o cliente
 * nao pediu progresso), o que permite avisar quem chamou.
 *
 * <p>As mensagens de log passam por {@code notifications/message} e sao filtradas
 * pelo nivel definido no {@code logging/setLevel} do cliente - quem define o corte
 * e o SDK, nao esta tool.
 */
@Component
public class LongRunningTools {

    @McpTool(name = "showcase_run_batch",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false),
            title = "Processar lote (progresso + log)",
            description = "Simula um processamento longo emitindo notificacoes de progresso e mensagens de log.")
    public String runBatch(
            McpSyncRequestContext context,
            @McpProgressToken String progressToken,
            @McpToolParam(description = "Quantidade de etapas entre 1 e 20. Padrao: 5", required = false)
            Integer steps,
            @McpToolParam(description = "Pausa em milissegundos entre etapas, de 0 a 1000. Padrao: 120",
                    required = false) Integer delayMillis) {

        int total = Math.clamp(steps == null ? 5 : steps, 1, 20);
        long pause = Math.clamp(delayMillis == null ? 120 : delayMillis, 0, 1000);

        context.log(spec -> spec.level(LoggingLevel.INFO)
                .logger("showcase.batch")
                .message("Iniciando lote com " + total + " etapa(s)."));

        for (int step = 1; step <= total; step++) {
            sleep(pause);

            final int current = step;
            context.progress(spec -> spec
                    .progress(current)
                    .total(total)
                    .message("Etapa " + current + " de " + total));

            context.log(spec -> spec.level(LoggingLevel.DEBUG)
                    .logger("showcase.batch")
                    .message("Etapa " + current + " concluida."));
        }

        context.log(spec -> spec.level(LoggingLevel.NOTICE)
                .logger("showcase.batch")
                .message("Lote finalizado."));

        String tokenInfo = (progressToken == null || progressToken.isBlank())
                ? "O cliente nao enviou progressToken, entao as notificacoes de progresso foram descartadas."
                : "Progresso enviado com o token '" + progressToken + "'.";

        return "Lote concluido: " + total + " etapa(s). " + tokenInfo;
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
