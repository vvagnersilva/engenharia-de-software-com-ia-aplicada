package com.songhighlights.service;

import com.songhighlights.entity.ConversationMessageEntity;
import com.songhighlights.llm.LlmChatResponse;
import com.songhighlights.llm.StructuredChatClient;
import com.songhighlights.prompt.ChatPrompts;
import com.songhighlights.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Substitui o StateGraph do LangGraph (buildChatGraph em graph.ts): orquestra,
 * em ordem, o que os nós chat -> savePreferences -> summarize faziam, com o
 * roteamento condicional (routeAfterChat / routeAfterSavePreferences)
 * reproduzido aqui como lógica explícita, já que cada turno agora é uma
 * chamada REST isolada em vez de uma invocação de grafo com estado em memória.
 */
@Service
public class ChatOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrationService.class);

    private final StructuredChatClient structuredChatClient;
    private final PreferencesService preferencesService;
    private final SummarizationService summarizationService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final int maxMessagesToSummary;

    public ChatOrchestrationService(
            StructuredChatClient structuredChatClient,
            PreferencesService preferencesService,
            SummarizationService summarizationService,
            ConversationMessageRepository conversationMessageRepository,
            @Value("${app.chat.max-messages-to-summary:2}") int maxMessagesToSummary
    ) {
        this.structuredChatClient = structuredChatClient;
        this.preferencesService = preferencesService;
        this.summarizationService = summarizationService;
        this.conversationMessageRepository = conversationMessageRepository;
        this.maxMessagesToSummary = maxMessagesToSummary;
    }

    public ChatTurnResult handleTurn(String userId, String message) {
        String userContext = preferencesService.getBasicInfo(userId).orElse(null);
        List<ConversationMessageEntity> history = conversationMessageRepository.findByUserIdOrderByIdAsc(userId);

        String conversationHistory = history.isEmpty() ? null : history.stream()
                .map(msg -> (ConversationMessageEntity.ROLE_USER.equals(msg.getRole()) ? "User" : "AI") + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        String systemPrompt = ChatPrompts.getSystemPrompt(userContext);
        String userPrompt = ChatPrompts.getUserPromptTemplate(message, conversationHistory);

        LlmChatResponse response = generateWithRetry(userId, systemPrompt, userPrompt);

        conversationMessageRepository.save(new ConversationMessageEntity(userId, ConversationMessageEntity.ROLE_USER, message));
        conversationMessageRepository.save(new ConversationMessageEntity(userId, ConversationMessageEntity.ROLE_AI, response.message()));

        boolean preferencesUpdated = false;
        if (response.shouldSavePreferences() && response.preferences() != null) {
            preferencesService.mergePreferences(userId, response.preferences());
            preferencesUpdated = true;
        }

        long totalMessages = conversationMessageRepository.countByUserId(userId);
        boolean needsSummarization = totalMessages >= maxMessagesToSummary;

        boolean conversationSummarized = false;
        if (needsSummarization) {
            conversationSummarized = summarizationService.summarize(userId);
        }

        return new ChatTurnResult(userId, response.message(), preferencesUpdated, conversationSummarized);
    }

    /**
     * Modelos gratuitos ocasionalmente "gaguejam" e devolvem JSON malformado
     * (ex.: aspas duplicadas não escapadas), o que quebra o parser estrito do
     * BeanOutputConverter. Uma segunda tentativa costuma resolver, já que é uma
     * falha de amostragem do modelo e não um erro determinístico.
     */
    private LlmChatResponse generateWithRetry(String userId, String systemPrompt, String userPrompt) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return structuredChatClient.generateStructured(systemPrompt, userPrompt, LlmChatResponse.class);
            } catch (Exception e) {
                lastError = e;
                log.warn("Tentativa {} falhou ao gerar resposta para o usuário {}: {}", attempt, userId, e.getMessage());
            }
        }
        log.error("Falha ao gerar resposta para o usuário {} após retentativas", userId, lastError);
        return new LlmChatResponse("Desculpe, encontrei um erro. Pode tentar novamente?", null, false);
    }
}
