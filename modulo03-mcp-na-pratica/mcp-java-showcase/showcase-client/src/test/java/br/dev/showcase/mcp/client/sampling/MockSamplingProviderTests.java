package br.dev.showcase.mcp.client.sampling;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.ModelPreferences;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.SamplingMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockSamplingProviderTests {

    private final MockSamplingProvider provider = new MockSamplingProvider();

    @Test
    void respostaEDeterministicaEIdentificaOModeloMock() {
        CreateMessageRequest request = CreateMessageRequest.builder()
                .messages(List.of(new SamplingMessage(Role.USER,
                        TextContent.builder("Resuma este texto").build())))
                .modelPreferences(ModelPreferences.builder()
                        .addHint("claude-sonnet")
                        .intelligencePriority(0.8)
                        .build())
                .maxTokens(100)
                .build();

        CreateMessageResult result = provider.generate(request);

        assertThat(result.model()).isEqualTo("mock-sampling-provider");
        assertThat(result.role()).isEqualTo(Role.ASSISTANT);
        String text = ((TextContent) result.content()).text();
        assertThat(text).contains("resposta simulada");
        assertThat(text).contains("claude-sonnet");
        assertThat(text).contains("Resuma este texto");
    }
}
