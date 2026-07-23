package com.songhighlights.application.usecase;

import com.songhighlights.application.prompt.ChatPrompts;
import com.songhighlights.application.service.PreferencesService;
import com.songhighlights.domain.model.ChatTurnResult;
import com.songhighlights.domain.model.ConversationMessage;
import com.songhighlights.domain.model.LlmChatResponse;
import com.songhighlights.domain.port.ChatModelPort;
import com.songhighlights.domain.port.ConversationRepository;
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
public class HandleChatTurnUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleChatTurnUseCase.class);

    private final ChatModelPort chatModelPort;
    private final PreferencesService preferencesService;
    private final SummarizeConversationUseCase summarizeConversationUseCase;
    private final ConversationRepository conversationRepository;
    private final int maxMessagesToSummary;

    public HandleChatTurnUseCase(
            ChatModelPort chatModelPort,
            PreferencesService preferencesService,
            SummarizeConversationUseCase summarizeConversationUseCase,
            ConversationRepository conversationRepository,
            @Value("${app.chat.max-messages-to-summary:2}") int maxMessagesToSummary
    ) {
        this.chatModelPort = chatModelPort;
        this.preferencesService = preferencesService;
        this.summarizeConversationUseCase = summarizeConversationUseCase;
        this.conversationRepository = conversationRepository;
        this.maxMessagesToSummary = maxMessagesToSummary;
    }

    public ChatTurnResult handleTurn(String userId, String message) {
        String userContext = preferencesService.getBasicInfo(userId).orElse(null);
        List<ConversationMessage> history = conversationRepository.findByUserIdOrderByIdAsc(userId);

        String conversationHistory = history.isEmpty() ? null : history.stream()
                .map(msg -> (ConversationMessage.ROLE_USER.equals(msg.role()) ? "User" : "AI") + ": " + msg.content())
                .collect(Collectors.joining("\n"));

        String systemPrompt = ChatPrompts.getSystemPrompt(userContext);
        String userPrompt = ChatPrompts.getUserPromptTemplate(message, conversationHistory);

        LlmChatResponse response = generateWithRetry(userId, systemPrompt, userPrompt);

        conversationRepository.save(ConversationMessage.newMessage(userId, ConversationMessage.ROLE_USER, message));
        conversationRepository.save(ConversationMessage.newMessage(userId, ConversationMessage.ROLE_AI, response.message()));

        boolean preferencesUpdated = false;
        if (response.shouldSavePreferences() && response.preferences() != null) {
            preferencesService.mergePreferences(userId, response.preferences());
            preferencesUpdated = true;
        }

        long totalMessages = conversationRepository.countByUserId(userId);
        boolean needsSummarization = totalMessages >= maxMessagesToSummary;

        boolean conversationSummarized = false;
        if (needsSummarization) {
            conversationSummarized = summarizeConversationUseCase.summarize(userId);
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
                return chatModelPort.generateStructured(systemPrompt, userPrompt, LlmChatResponse.class);
            } catch (Exception e) {
                lastError = e;
                log.warn("Tentativa {} falhou ao gerar resposta para o usuário {}: {}", attempt, userId, e.getMessage());
            }
        }
        log.error("Falha ao gerar resposta para o usuário {} após retentativas", userId, lastError);
        return new LlmChatResponse("Desculpe, encontrei um erro. Pode tentar novamente?", null, false);
    }
}
