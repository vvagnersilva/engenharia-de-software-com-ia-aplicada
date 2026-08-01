package br.dev.showcase.mcp.core.tools;

import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

/**
 * Capacidade MCP: <b>sampling</b> - o servidor pede uma geracao ao LLM do cliente.
 *
 * <p>A inversao e o ponto importante: quem tem o modelo (e paga por ele) e o cliente.
 * O servidor descreve a tarefa e envia {@code ModelPreferences} como dica de qual
 * classe de modelo atende melhor; a escolha final e sempre do cliente.
 *
 * <p>Se o cliente nao declarar a capability de sampling, a tool degrada com um
 * resumo local em vez de falhar.
 */
@Component
public class SamplingTools {

    @McpTool(name = "showcase_summarize",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = false, openWorldHint = true),
            title = "Resumir texto usando o LLM do cliente",
            description = "Pede ao cliente MCP que gere um resumo do texto informado. "
                    + "Se o cliente nao suportar sampling, devolve um resumo local simples.")
    public String summarize(
            McpSyncRequestContext context,
            @McpToolParam(description = "Texto a resumir", required = true) String text,
            @McpToolParam(description = "Numero maximo de frases do resumo. Padrao: 3", required = false)
            Integer maxSentences) {

        int sentences = Math.clamp(maxSentences == null ? 3 : maxSentences, 1, 10);

        if (!context.sampleEnabled()) {
            context.warn("Cliente sem suporte a sampling; usando o resumo local de fallback.");
            return "[fallback local - cliente sem sampling]\n" + localSummary(text, sentences);
        }

        context.info("Solicitando geracao ao cliente via sampling/createMessage.");

        CreateMessageResult result = context.sample(spec -> spec
                .systemPrompt("Voce e um assistente que resume textos em portugues do Brasil. "
                        + "Responda apenas com o resumo, sem preambulo.")
                .message("Resuma o texto a seguir em no maximo " + sentences + " frase(s):\n\n" + text)
                .modelPreferences(prefs -> prefs
                        .modelHints("claude-sonnet", "claude")
                        .intelligencePriority(0.8)
                        .speedPriority(0.4)
                        .costPriority(0.3))
                .temperature(0.2)
                .maxTokens(400));

        String generated = (result.content() instanceof TextContent textContent)
                ? textContent.text()
                : String.valueOf(result.content());

        return "[gerado pelo cliente - modelo: " + result.model() + "]\n" + generated;
    }

    /** Resumo bobo por corte de frases, usado quando o cliente nao faz sampling. */
    private static String localSummary(String text, int sentences) {
        String[] parts = text.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(sentences, parts.length); i++) {
            sb.append(parts[i].trim()).append(' ');
        }
        return sb.toString().trim();
    }
}
