package br.dev.showcase.mcp.client.sampling;

import java.util.ArrayList;
import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Provedor de sampling que delega para um LLM real via {@code ChatModel} do Spring AI.
 *
 * <p>So entra em cena com o perfil {@code real-llm} ativo e a chave da API presente.
 * Traduz o pedido MCP ({@code CreateMessageRequest}) para o modelo de mensagens do
 * Spring AI e converte a resposta de volta.
 */
public class ChatModelSamplingProvider implements SamplingProvider {

    private final ChatModel chatModel;

    public ChatModelSamplingProvider(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String modelName() {
        return "spring-ai-chat-model";
    }

    @Override
    public CreateMessageResult generate(CreateMessageRequest request) {
        List<Message> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        request.messages().forEach(sampling -> {
            String text = sampling.content() instanceof TextContent content ? content.text() : "";
            messages.add(sampling.role() == Role.ASSISTANT ? new AssistantMessage(text) : new UserMessage(text));
        });

        var response = chatModel.call(new Prompt(messages));
        String answer = response.getResult().getOutput().getText();

        return CreateMessageResult
                .builder(Role.ASSISTANT,
                        TextContent.builder(answer == null ? "" : answer).build(),
                        response.getMetadata().getModel())
                .stopReason(CreateMessageResult.StopReason.END_TURN)
                .build();
    }
}
