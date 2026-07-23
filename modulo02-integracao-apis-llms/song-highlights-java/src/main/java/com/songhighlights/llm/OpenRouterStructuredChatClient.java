package com.songhighlights.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

/**
 * Implementação de StructuredChatClient usando o ChatClient do Spring AI,
 * configurado (via application.yml: spring.ai.openai.*) para falar com a API
 * compatível com OpenAI do OpenRouter.
 *
 * Simplificação assumida nesta conversão: o projeto TypeScript original também
 * enviava `models` (lista de fallback) e `provider.sort` como extensões
 * proprietárias do OpenRouter. O Spring AI não expõe esses campos nativamente,
 * então esta conversão usa apenas o modelo único configurado em
 * spring.ai.openai.chat.options.model. Ver README para detalhes.
 *
 * response_format=json_object é forçado aqui porque modelos gratuitos via
 * OpenRouter ocasionalmente devolvem JSON malformado (aspas não escapadas,
 * repetição de tokens) quando a estrutura é pedida apenas por instrução em
 * texto no prompt. Com json_object nativo, o provedor restringe a decodificação
 * a JSON sintaticamente válido, eliminando essa falha (validado empiricamente
 * contra a API do OpenRouter antes desta mudança).
 */
@Component
public class OpenRouterStructuredChatClient implements StructuredChatClient {

    private final ChatClient chatClient;

    public OpenRouterStructuredChatClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public <T> T generateStructured(String systemPrompt, String userPrompt, Class<T> responseType) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                        .build())
                .call()
                .entity(responseType);
    }
}
