package com.songhighlights.application.usecase;

import com.songhighlights.application.prompt.SummarizationPrompts;
import com.songhighlights.application.service.PreferencesService;
import com.songhighlights.domain.model.ConversationMessage;
import com.songhighlights.domain.model.ConversationSummaryData;
import com.songhighlights.domain.port.ChatModelPort;
import com.songhighlights.domain.port.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Porte de src/graph/nodes/summarizationNode.ts: condensa o histórico da
 * conversa em um sumário estruturado, persiste-o (via PreferencesService) e
 * reduz o histórico de mensagens às últimas 2 - equivalente ao RemoveMessage
 * aplicado no projeto original.
 */
@Service
public class SummarizeConversationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SummarizeConversationUseCase.class);
    private static final int MESSAGES_TO_KEEP_AFTER_SUMMARY = 2;

    private final ChatModelPort chatModelPort;
    private final PreferencesService preferencesService;
    private final ConversationRepository conversationRepository;

    public SummarizeConversationUseCase(
            ChatModelPort chatModelPort,
            PreferencesService preferencesService,
            ConversationRepository conversationRepository
    ) {
        this.chatModelPort = chatModelPort;
        this.preferencesService = preferencesService;
        this.conversationRepository = conversationRepository;
    }

    public boolean summarize(String userId) {
        List<ConversationMessage> messages = conversationRepository.findByUserIdOrderByIdAsc(userId);

        List<Map<String, String>> conversationHistory = messages.stream()
                .map(msg -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("role", ConversationMessage.ROLE_USER.equals(msg.role()) ? "User" : "AI");
                    entry.put("content", msg.content());
                    return entry;
                })
                .toList();

        ConversationSummaryData previousSummary = preferencesService.getSummary(userId).orElse(null);

        String systemPrompt = SummarizationPrompts.getSummarizationSystemPrompt();
        String userPrompt = SummarizationPrompts.getSummarizationUserPrompt(conversationHistory, previousSummary);

        ConversationSummaryData summary = generateWithRetry(userId, systemPrompt, userPrompt);
        if (summary == null) {
            return false;
        }

        preferencesService.storeSummary(userId, summary);
        trimHistory(messages);

        return true;
    }

    private void trimHistory(List<ConversationMessage> messages) {
        if (messages.size() <= MESSAGES_TO_KEEP_AFTER_SUMMARY) {
            return;
        }
        List<Long> idsToDelete = messages.subList(0, messages.size() - MESSAGES_TO_KEEP_AFTER_SUMMARY).stream()
                .map(ConversationMessage::id)
                .toList();
        conversationRepository.deleteAllById(idsToDelete);
    }

    /**
     * Modelos gratuitos ocasionalmente "gaguejam" e devolvem JSON malformado,
     * o que quebra o parser estrito do BeanOutputConverter. Uma segunda
     * tentativa costuma resolver, já que é uma falha de amostragem do modelo.
     */
    private ConversationSummaryData generateWithRetry(String userId, String systemPrompt, String userPrompt) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return chatModelPort.generateStructured(systemPrompt, userPrompt, ConversationSummaryData.class);
            } catch (Exception e) {
                lastError = e;
                log.warn("Tentativa {} falhou ao sumarizar conversa do usuário {}: {}", attempt, userId, e.getMessage());
            }
        }
        log.error("Falha ao sumarizar conversa do usuário {} após retentativas", userId, lastError);
        return null;
    }
}
