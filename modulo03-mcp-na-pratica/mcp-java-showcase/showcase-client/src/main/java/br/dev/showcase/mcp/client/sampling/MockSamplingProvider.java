package br.dev.showcase.mcp.client.sampling;

import java.util.stream.Collectors;

import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Provedor de sampling deterministico, usado quando nao ha LLM configurado.
 *
 * <p>Nao chama modelo nenhum: monta uma resposta previsivel a partir do proprio
 * pedido. Serve para o projeto rodar sem chave de API e para os testes terem
 * resultado estavel.
 */
public class MockSamplingProvider implements SamplingProvider {

    @Override
    public String modelName() {
        return "mock-sampling-provider";
    }

    @Override
    public CreateMessageResult generate(CreateMessageRequest request) {
        String prompt = request.messages().stream()
                .map(message -> message.content() instanceof TextContent text ? text.text() : "")
                .collect(Collectors.joining("\n"));

        String hints = (request.modelPreferences() == null || request.modelPreferences().hints() == null)
                ? "nenhuma"
                : request.modelPreferences().hints().stream()
                        .map(io.modelcontextprotocol.spec.McpSchema.ModelHint::name)
                        .collect(Collectors.joining(", "));

        String answer = """
                [resposta simulada - nenhum LLM foi chamado]
                Dicas de modelo pedidas pelo servidor: %s
                Tamanho do prompt recebido: %d caracteres
                Primeira linha do pedido: %s
                """.formatted(hints, prompt.length(), firstLine(prompt));

        return CreateMessageResult
                .builder(Role.ASSISTANT, TextContent.builder(answer).build(), modelName())
                .stopReason(CreateMessageResult.StopReason.END_TURN)
                .build();
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        String line = newline < 0 ? text : text.substring(0, newline);
        return line.length() <= 120 ? line : line.substring(0, 120) + "...";
    }
}
