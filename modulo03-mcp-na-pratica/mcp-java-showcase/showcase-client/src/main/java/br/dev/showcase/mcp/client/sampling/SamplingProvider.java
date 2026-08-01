package br.dev.showcase.mcp.client.sampling;

import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;

/**
 * Quem realmente gera o texto quando um servidor pede sampling.
 *
 * <p>Existem duas implementacoes: uma mockada (padrao, sem chave de API) e uma
 * apoiada em um {@code ChatModel} do Spring AI, ativada apenas com o perfil
 * {@code real-llm} e a variavel {@code ANTHROPIC_API_KEY} preenchida.
 */
public interface SamplingProvider {

    /** Nome do "modelo" reportado de volta ao servidor no campo {@code model}. */
    String modelName();

    CreateMessageResult generate(CreateMessageRequest request);
}
